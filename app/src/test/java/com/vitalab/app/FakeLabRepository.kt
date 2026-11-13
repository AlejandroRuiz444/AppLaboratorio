package com.vitalab.app

import com.vitalab.app.data.Examen
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.coincideBusqueda
import com.vitalab.app.data.local.Cita
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.CitaExamen
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.local.PacienteConConteo
import com.vitalab.app.data.repository.Estadisticas
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.PacienteDestino
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.repository.SolicitudCita
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repositorio en memoria para probar ViewModels sin Room. Las reglas son las mismas, simplificadas. */
class FakeLabRepository : LabRepository {
    val examenes = MutableStateFlow(catalogoExamenes)
    val ordenIds = MutableStateFlow<List<Int>>(emptyList())
    val adultoMayorFlow = MutableStateFlow(false)
    val listaPacientes = MutableStateFlow<List<Paciente>>(emptyList())
    val listaCitas = MutableStateFlow<List<CitaCompleta>>(emptyList())
    val estadisticasFlow = MutableStateFlow(Estadisticas())

    /** Si no es null, `crearCitaDesdeOrden` devuelve esto en vez de crear la cita. */
    var resultadoForzado: Resultado<CitaCompleta>? = null
    var ultimaSolicitud: SolicitudCita? = null
    private var siguienteId = 1L

    override fun examenesActivos(): Flow<List<Examen>> = examenes.map { l -> l.filter { it.activo } }
    override fun todosLosExamenes(): Flow<List<Examen>> = examenes
    override suspend fun obtenerExamen(id: Int): Examen? = examenes.value.firstOrNull { it.id == id }
    override suspend fun guardarExamen(examen: Examen): Int {
        val id = if (examen.id == 0) (examenes.value.maxOf { it.id } + 1) else examen.id
        examenes.update { l -> l.filter { it.id != id } + examen.copy(id = id) }
        return id
    }
    override suspend fun cambiarActivoExamen(id: Int, activo: Boolean) {
        examenes.update { l -> l.map { if (it.id == id) it.copy(activo = activo) else it } }
        if (!activo) ordenIds.update { it - id }
    }

    override fun orden(): Flow<List<Examen>> = combine(examenes, ordenIds) { ex, ids -> ids.mapNotNull { id -> ex.firstOrNull { it.id == id && it.activo } } }
    override fun adultoMayor(): Flow<Boolean> = adultoMayorFlow
    override suspend fun alternarEnOrden(examenId: Int) = ordenIds.update { if (examenId in it) it - examenId else it + examenId }
    override suspend fun quitarDeOrden(examenId: Int) = ordenIds.update { it - examenId }
    override suspend fun setAdultoMayor(valor: Boolean) { adultoMayorFlow.value = valor }

    override fun pacientes(consulta: String): Flow<List<PacienteConConteo>> =
        combine(listaPacientes, listaCitas) { ps, cs ->
            ps.filter { consulta.isBlank() || coincideBusqueda(it.nombre, consulta) || it.documento.contains(consulta.trim()) }
                .map { p -> PacienteConConteo(p, cs.count { it.cita.pacienteId == p.id }) }
        }
    override suspend fun obtenerPaciente(id: Long): Paciente? = listaPacientes.value.firstOrNull { it.id == id }
    override suspend fun guardarPaciente(paciente: Paciente): Resultado<Long> {
        if (listaPacientes.value.any { it.documento == paciente.documento && it.id != paciente.id }) {
            return Resultado.Error("Ya existe un paciente con el documento ${paciente.documento}.")
        }
        val id = if (paciente.id == 0L) siguienteId++ else paciente.id
        listaPacientes.update { l -> l.filter { it.id != id } + paciente.copy(id = id) }
        return Resultado.Exito(id)
    }
    override suspend fun eliminarPaciente(id: Long) {
        listaPacientes.update { l -> l.filter { it.id != id } }
        listaCitas.update { l -> l.filter { it.cita.pacienteId != id } }
    }

    override fun citas(estado: EstadoCita?, consulta: String): Flow<List<CitaCompleta>> = listaCitas.map { l ->
        l.filter { estado == null || it.cita.estado == estado }
            .filter { consulta.isBlank() || coincideBusqueda(it.paciente.nombre, consulta) || it.cita.codigo.contains(consulta.trim(), ignoreCase = true) }
    }
    override fun cita(id: Long): Flow<CitaCompleta?> = listaCitas.map { l -> l.firstOrNull { it.cita.id == id } }
    override fun citasProgramadasDesde(epochDay: Long): Flow<List<CitaCompleta>> =
        listaCitas.map { l -> l.filter { it.cita.estado == EstadoCita.PROGRAMADA && it.cita.fecha >= epochDay } }

    override suspend fun crearCitaDesdeOrden(solicitud: SolicitudCita): Resultado<CitaCompleta> {
        ultimaSolicitud = solicitud
        resultadoForzado?.let { return it }
        val ex = ordenIds.value.mapNotNull { id -> examenes.value.firstOrNull { it.id == id && it.activo } }
        if (ex.isEmpty()) return Resultado.Error("Tu orden está vacía.")
        val paciente = when (val d = solicitud.paciente) {
            is PacienteDestino.Existente -> listaPacientes.value.first { it.id == d.id }
            is PacienteDestino.Nuevo -> d.paciente.copy(id = siguienteId++).also { nuevo -> listaPacientes.update { it + nuevo } }
        }
        val r = calcularResumen(ex, adultoMayorFlow.value, solicitud.domicilio)
        val cita = Cita(
            id = siguienteId++, codigo = "VL-${1000 + siguienteId}", pacienteId = paciente.id, fecha = solicitud.fecha,
            hora = solicitud.hora, domicilio = solicitud.domicilio, direccion = solicitud.direccion,
            adultoMayor = adultoMayorFlow.value, subtotal = r.subtotal, descuentoPaquete = r.descuentoPaquete,
            descuentoAdultoMayor = r.descuentoAdultoMayor, recargoDomicilio = r.recargoDomicilio
        )
        val completa = CitaCompleta(cita, paciente, ex, ex.map { CitaExamen(cita.id, it.id, it.precio) })
        listaCitas.update { it + completa }
        ordenIds.value = emptyList()
        adultoMayorFlow.value = false
        return Resultado.Exito(completa)
    }

    override suspend fun reprogramarCita(id: Long, fecha: Long, hora: String, domicilio: Boolean, direccion: String): Resultado<Unit> {
        listaCitas.update { l -> l.map { if (it.cita.id == id) it.copy(cita = it.cita.copy(fecha = fecha, hora = hora, domicilio = domicilio, direccion = direccion)) else it } }
        return Resultado.Exito(Unit)
    }
    override suspend fun cambiarEstadoCita(id: Long, estado: EstadoCita): Resultado<Unit> {
        listaCitas.update { l -> l.map { if (it.cita.id == id) it.copy(cita = it.cita.copy(estado = estado)) else it } }
        return Resultado.Exito(Unit)
    }
    override suspend fun eliminarCita(id: Long) = listaCitas.update { l -> l.filter { it.cita.id != id } }

    override fun estadisticas(): Flow<Estadisticas> = estadisticasFlow
}
