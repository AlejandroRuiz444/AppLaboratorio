package com.vitalab.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.LineaCita
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Acciones que piden confirmación en el detalle de una cita. */
enum class AccionCita { CANCELAR, COMPLETAR, ELIMINAR }

data class CitaDetalleUiState(
    val cargando: Boolean = true,
    /** null mientras carga o si la cita no existe. */
    val cita: CitaCompleta? = null,
    val noEncontrada: Boolean = false,
    /** Exámenes con el precio que se cobró al agendar. */
    val lineas: List<LineaCita> = emptyList(),
    val indicaciones: List<String> = emptyList(),
    /** true solo si está PROGRAMADA: entonces se puede reprogramar, cancelar o completar. */
    val puedeModificar: Boolean = false,
    /** Si no es null, la pantalla muestra el diálogo de confirmación de esa acción. */
    val accionPendiente: AccionCita? = null,
    val procesando: Boolean = false,
    /** Aviso de una sola vez (Snackbar). Llamar `onMensajeMostrado()` al mostrarlo. */
    val mensaje: String? = null,
    /** Pasa a true cuando se eliminó: la pantalla debe volver atrás. */
    val eliminada: Boolean = false
)

/** Detalle de una cita (id en la ruta) con cambio de estado y eliminación. */
class CitaDetalleViewModel(
    private val repo: LabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>(Rutas.ARG_ID) ?: -1L

    private data class Interno(
        val accion: AccionCita? = null,
        val procesando: Boolean = false,
        val mensaje: String? = null,
        val eliminada: Boolean = false
    )

    private val interno = MutableStateFlow(Interno())

    val uiState: StateFlow<CitaDetalleUiState> = combine(repo.cita(id), interno) { cita, i ->
        val detalle = cita?.detalle.orEmpty()
        CitaDetalleUiState(
            cargando = false,
            cita = cita,
            // Tras eliminarla el flujo emite null: no es "no encontrada", es que ya se fue.
            noEncontrada = cita == null && !i.eliminada,
            lineas = detalle,
            indicaciones = indicacionesPreparacion(detalle.map { it.examen }),
            puedeModificar = cita?.cita?.estado == EstadoCita.PROGRAMADA,
            accionPendiente = i.accion,
            procesando = i.procesando,
            mensaje = i.mensaje,
            eliminada = i.eliminada
        )
    }.stateIn(viewModelScope, Suscripcion, CitaDetalleUiState())

    fun onPedirAccion(accion: AccionCita) = interno.update { it.copy(accion = accion) }
    fun onDescartarAccion() = interno.update { it.copy(accion = null) }
    fun onMensajeMostrado() = interno.update { it.copy(mensaje = null) }

    fun onConfirmarAccion() {
        val accion = interno.value.accion ?: return
        interno.update { it.copy(accion = null, procesando = true) }
        viewModelScope.launch {
            when (accion) {
                AccionCita.ELIMINAR -> {
                    repo.eliminarCita(id)
                    interno.update { it.copy(procesando = false, eliminada = true) }
                }
                AccionCita.CANCELAR -> cambiarEstado(EstadoCita.CANCELADA, "Cita cancelada")
                AccionCita.COMPLETAR -> cambiarEstado(EstadoCita.COMPLETADA, "Cita marcada como completada")
            }
        }
    }

    private suspend fun cambiarEstado(estado: EstadoCita, aviso: String) {
        val texto = when (val r = repo.cambiarEstadoCita(id, estado)) {
            is Resultado.Exito -> aviso
            is Resultado.Error -> r.mensaje
        }
        interno.update { it.copy(procesando = false, mensaje = texto) }
    }
}
