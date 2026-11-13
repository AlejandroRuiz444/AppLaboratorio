package com.vitalab.app.data.repository

import com.vitalab.app.data.Examen
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.CitasPorPaciente
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.ExamenPopular
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.local.PacienteConConteo
import kotlinx.coroutines.flow.Flow

/** Resultado de una operación que puede fallar por una regla de negocio. `mensaje` ya está listo para mostrar. */
sealed interface Resultado<out T> {
    data class Exito<out T>(val valor: T) : Resultado<T>
    data class Error(val mensaje: String) : Resultado<Nothing>
}

/** Para quién es la cita: un paciente que ya existe, o uno nuevo que se crea en la misma transacción. */
sealed interface PacienteDestino {
    data class Existente(val id: Long) : PacienteDestino
    data class Nuevo(val paciente: Paciente) : PacienteDestino
}

/** Datos que elige el usuario al agendar; los exámenes y descuentos salen de la orden guardada. */
data class SolicitudCita(
    val paciente: PacienteDestino,
    /** `LocalDate.toEpochDay()` */
    val fecha: Long,
    val hora: String,
    val domicilio: Boolean,
    val direccion: String
)

data class Estadisticas(
    /** Siempre trae las tres claves (con 0 si no hay citas en ese estado). */
    val citasPorEstado: Map<EstadoCita, Int> = EstadoCita.entries.associateWith { 0 },
    /** Suma de lo cobrado en citas COMPLETADAS. */
    val ingresos: Int = 0,
    /** Descuentos (paquete + adulto mayor) de las citas no canceladas. */
    val ahorro: Int = 0,
    /** Top 5, sin contar citas canceladas. */
    val topExamenes: List<ExamenPopular> = emptyList(),
    /** Todas las citas de cada paciente (cualquier estado), de mayor a menor. */
    val citasPorPaciente: List<CitasPorPaciente> = emptyList()
) {
    val totalCitas: Int get() = citasPorEstado.values.sum()
}

/** Único punto de acceso a los datos para los ViewModels. */
interface LabRepository {
    // --- Catálogo de exámenes ---
    fun examenesActivos(): Flow<List<Examen>>
    /** Incluye los desactivados (para la pantalla de administración). */
    fun todosLosExamenes(): Flow<List<Examen>>
    suspend fun obtenerExamen(id: Int): Examen?
    /** `examen.id == 0` crea uno nuevo; otro id lo actualiza. Devuelve el id. */
    suspend fun guardarExamen(examen: Examen): Int
    /** Borrado lógico. Al desactivar, el examen sale también de la orden en curso. */
    suspend fun cambiarActivoExamen(id: Int, activo: Boolean)

    // --- Orden en curso (persistente) ---
    fun orden(): Flow<List<Examen>>
    fun adultoMayor(): Flow<Boolean>
    suspend fun alternarEnOrden(examenId: Int)
    suspend fun quitarDeOrden(examenId: Int)
    suspend fun setAdultoMayor(valor: Boolean)

    // --- Pacientes (CRUD) ---
    /** Filtra por nombre o documento; sin distinguir mayúsculas ni tildes. `""` devuelve todos. */
    fun pacientes(consulta: String = ""): Flow<List<PacienteConConteo>>
    suspend fun obtenerPaciente(id: Long): Paciente?
    /** `paciente.id == 0` crea; otro id actualiza. Error si el documento ya pertenece a otro paciente. */
    suspend fun guardarPaciente(paciente: Paciente): Resultado<Long>
    /** Elimina también sus citas (CASCADE). */
    suspend fun eliminarPaciente(id: Long)

    // --- Citas ---
    /** `estado == null` = todas. `consulta` busca en nombre/documento del paciente y en el código. */
    fun citas(estado: EstadoCita?, consulta: String = ""): Flow<List<CitaCompleta>>
    fun cita(id: Long): Flow<CitaCompleta?>
    fun citasProgramadasDesde(epochDay: Long): Flow<List<CitaCompleta>>
    /** Guarda cita + líneas con precio congelado y vacía la orden, todo en una transacción. */
    suspend fun crearCitaDesdeOrden(solicitud: SolicitudCita): Resultado<CitaCompleta>
    /** Solo citas PROGRAMADA. Recalcula el recargo de domicilio según la modalidad nueva. */
    suspend fun reprogramarCita(id: Long, fecha: Long, hora: String, domicilio: Boolean, direccion: String): Resultado<Unit>
    /** Solo desde PROGRAMADA hacia COMPLETADA o CANCELADA. */
    suspend fun cambiarEstadoCita(id: Long, estado: EstadoCita): Resultado<Unit>
    suspend fun eliminarCita(id: Long)

    // --- Estadísticas ---
    fun estadisticas(): Flow<Estadisticas>
}
