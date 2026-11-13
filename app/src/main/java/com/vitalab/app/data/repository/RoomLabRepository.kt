package com.vitalab.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.vitalab.app.data.Examen
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.calcularResumenDePrecios
import com.vitalab.app.data.coincideBusqueda
import com.vitalab.app.data.generarCodigoCita
import com.vitalab.app.data.local.AppDatabase
import com.vitalab.app.data.local.Cita
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.OrdenAjuste
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.local.PacienteConConteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomLabRepository(private val db: AppDatabase) : LabRepository {
    private val examenDao = db.examenDao()
    private val pacienteDao = db.pacienteDao()
    private val citaDao = db.citaDao()
    private val ordenDao = db.ordenDao()
    private val estadisticasDao = db.estadisticasDao()

    // --- Catálogo ---

    override fun examenesActivos(): Flow<List<Examen>> = examenDao.observarActivos()
    override fun todosLosExamenes(): Flow<List<Examen>> = examenDao.observarTodos()
    override suspend fun obtenerExamen(id: Int): Examen? = examenDao.obtener(id)

    override suspend fun guardarExamen(examen: Examen): Int =
        if (examen.id == 0) {
            examenDao.insertar(examen).toInt()
        } else {
            examenDao.actualizar(examen)
            examen.id
        }

    override suspend fun cambiarActivoExamen(id: Int, activo: Boolean) {
        db.withTransaction {
            examenDao.cambiarActivo(id, activo)
            if (!activo) ordenDao.quitar(id)
        }
    }

    // --- Orden ---

    override fun orden(): Flow<List<Examen>> = ordenDao.observarExamenes()
    override fun adultoMayor(): Flow<Boolean> = ordenDao.observarAdultoMayor().map { it ?: false }
    override suspend fun alternarEnOrden(examenId: Int) = ordenDao.alternar(examenId)
    override suspend fun quitarDeOrden(examenId: Int) {
        ordenDao.quitar(examenId)
    }
    override suspend fun setAdultoMayor(valor: Boolean) = ordenDao.guardarAjuste(OrdenAjuste(adultoMayor = valor))

    // --- Pacientes ---

    // El texto se filtra en Kotlin y no con LIKE: el lower() de SQLite solo entiende ASCII y
    // "ángel" no encontraría "Ángel". Son pocos registros, así que no hay costo real.
    override fun pacientes(consulta: String): Flow<List<PacienteConConteo>> =
        pacienteDao.observarConConteo().map { lista ->
            if (consulta.isBlank()) lista else lista.filter { coincidePaciente(it.paciente, consulta) }
        }

    override suspend fun obtenerPaciente(id: Long): Paciente? = pacienteDao.obtener(id)

    override suspend fun guardarPaciente(paciente: Paciente): Resultado<Long> = protegido {
        val otro = pacienteDao.porDocumento(paciente.documento)
        if (otro != null && otro.id != paciente.id) return@protegido Resultado.Error(mensajeDocumentoRepetido(paciente.documento))
        if (paciente.id == 0L) {
            Resultado.Exito(pacienteDao.insertar(paciente))
        } else {
            pacienteDao.actualizar(paciente)
            Resultado.Exito(paciente.id)
        }
    }

    override suspend fun eliminarPaciente(id: Long) = pacienteDao.eliminar(id)

    // --- Citas ---

    override fun citas(estado: EstadoCita?, consulta: String): Flow<List<CitaCompleta>> =
        citaDao.observarCompletas(estado).map { lista ->
            if (consulta.isBlank()) lista
            else lista.filter { coincidePaciente(it.paciente, consulta) || it.cita.codigo.contains(consulta.trim(), ignoreCase = true) }
        }

    override fun cita(id: Long): Flow<CitaCompleta?> = citaDao.observarCompleta(id)
    override fun citasProgramadasDesde(epochDay: Long): Flow<List<CitaCompleta>> = citaDao.observarProgramadasDesde(epochDay)

    override suspend fun crearCitaDesdeOrden(solicitud: SolicitudCita): Resultado<CitaCompleta> = protegido {
        // Todo se lee y se escribe dentro de la misma transacción: un doble toque en "Confirmar"
        // encuentra la orden ya vacía y no crea una segunda cita.
        val examenes = ordenDao.examenes()
        if (examenes.isEmpty()) return@protegido Resultado.Error("Tu orden está vacía.")

        val pacienteId = when (val destino = solicitud.paciente) {
            is PacienteDestino.Existente ->
                pacienteDao.obtener(destino.id)?.id
                    ?: return@protegido Resultado.Error("El paciente seleccionado ya no existe.")
            is PacienteDestino.Nuevo -> {
                if (pacienteDao.porDocumento(destino.paciente.documento) != null) {
                    return@protegido Resultado.Error(mensajeDocumentoRepetido(destino.paciente.documento))
                }
                pacienteDao.insertar(destino.paciente)
            }
        }

        val codigo = codigoUnico() ?: return@protegido Resultado.Error("No se pudo generar el código de la cita. Intenta de nuevo.")
        val adultoMayor = ordenDao.adultoMayor() ?: false
        val resumen = calcularResumen(examenes, adultoMayor, solicitud.domicilio)
        val cita = Cita(
            codigo = codigo,
            pacienteId = pacienteId,
            fecha = solicitud.fecha,
            hora = solicitud.hora,
            domicilio = solicitud.domicilio,
            direccion = if (solicitud.domicilio) solicitud.direccion.trim() else "",
            adultoMayor = adultoMayor,
            subtotal = resumen.subtotal,
            descuentoPaquete = resumen.descuentoPaquete,
            descuentoAdultoMayor = resumen.descuentoAdultoMayor,
            recargoDomicilio = resumen.recargoDomicilio
        )
        val id = citaDao.crearConLineas(cita, examenes.associate { it.id to it.precio })

        ordenDao.vaciar()
        ordenDao.guardarAjuste(OrdenAjuste(adultoMayor = false))
        Resultado.Exito(checkNotNull(citaDao.obtenerCompleta(id)))
    }

    override suspend fun reprogramarCita(
        id: Long, fecha: Long, hora: String, domicilio: Boolean, direccion: String
    ): Resultado<Unit> = protegido {
        val actual = citaDao.obtenerCompleta(id) ?: return@protegido Resultado.Error("La cita ya no existe.")
        if (actual.cita.estado != EstadoCita.PROGRAMADA) {
            return@protegido Resultado.Error("Solo se pueden reprogramar citas programadas.")
        }
        // Cambiar de sede a domicilio (o al revés) cambia el recargo; los precios son los congelados al agendar.
        val recargo = calcularResumenDePrecios(actual.lineas.map { it.precio }, actual.cita.adultoMayor, domicilio).recargoDomicilio
        citaDao.reprogramarSiProgramada(id, fecha, hora, domicilio, if (domicilio) direccion.trim() else "", recargo)
        Resultado.Exito(Unit)
    }

    override suspend fun cambiarEstadoCita(id: Long, estado: EstadoCita): Resultado<Unit> {
        if (estado == EstadoCita.PROGRAMADA) return Resultado.Error("Una cita no puede volver a estar programada.")
        val filas = citaDao.cambiarEstadoSiProgramada(id, estado)
        return if (filas == 1) Resultado.Exito(Unit) else Resultado.Error("Solo se puede cambiar el estado de citas programadas.")
    }

    override suspend fun eliminarCita(id: Long) {
        citaDao.eliminar(id)
    }

    // --- Estadísticas ---

    override fun estadisticas(): Flow<Estadisticas> = combine(
        estadisticasDao.citasPorEstado(),
        estadisticasDao.ingresosCompletadas(),
        estadisticasDao.ahorroPorDescuentos(),
        estadisticasDao.examenesMasSolicitados(TOP_EXAMENES),
        estadisticasDao.citasPorPaciente()
    ) { porEstado, ingresos, ahorro, top, porPaciente ->
        Estadisticas(
            citasPorEstado = EstadoCita.entries.associateWith { e -> porEstado.firstOrNull { it.estado == e }?.cantidad ?: 0 },
            ingresos = ingresos,
            ahorro = ahorro,
            topExamenes = top,
            citasPorPaciente = porPaciente
        )
    }

    // --- Utilidades ---

    /**
     * Corre `bloque` en una transacción. Las reglas se revisan antes de escribir (para dar un mensaje claro);
     * si aun así una restricción de la base (UNIQUE/FK) salta, se devuelve un Error en vez de reventar la UI.
     */
    private suspend fun <T> protegido(bloque: suspend () -> Resultado<T>): Resultado<T> =
        try {
            db.withTransaction(bloque)
        } catch (e: SQLiteConstraintException) {
            Resultado.Error("No se pudo guardar: el dato ya existe o depende de otro que fue eliminado.")
        }

    private suspend fun codigoUnico(): String? {
        repeat(INTENTOS_CODIGO) {
            val codigo = generarCodigoCita()
            if (citaDao.contarPorCodigo(codigo) == 0) return codigo
        }
        return null
    }

    private fun coincidePaciente(p: Paciente, consulta: String) =
        coincideBusqueda(p.nombre, consulta) || p.documento.contains(consulta.trim())

    private fun mensajeDocumentoRepetido(documento: String) = "Ya existe un paciente con el documento $documento."

    private companion object {
        const val TOP_EXAMENES = 5
        const val INTENTOS_CODIGO = 20
    }
}
