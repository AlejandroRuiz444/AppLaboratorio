package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.local.PacienteConConteo
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacientesUiState(
    val pacientes: List<PacienteConConteo> = emptyList(),
    val consulta: String = "",
    val cargando: Boolean = true,
    /** Si no es null, la pantalla muestra el diálogo "¿Eliminar a X y sus N citas?". */
    val pacientePorEliminar: PacienteConConteo? = null,
    /** Aviso de una sola vez (p. ej. para un Snackbar). Llamar `onMensajeMostrado()` al mostrarlo. */
    val mensaje: String? = null
)

/** Lista de pacientes con búsqueda y eliminación con confirmación. */
class PacientesViewModel(private val repo: LabRepository) : ViewModel() {

    private val consulta = MutableStateFlow("")
    private val porEliminar = MutableStateFlow<PacienteConConteo?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PacientesUiState> = combine(
        consulta.flatMapLatest { repo.pacientes(it) }, consulta, porEliminar, mensaje
    ) { lista, q, pendiente, aviso ->
        PacientesUiState(lista, q, cargando = false, pacientePorEliminar = pendiente, mensaje = aviso)
    }.stateIn(viewModelScope, Suscripcion, PacientesUiState())

    fun onConsultaChange(texto: String) = consulta.update { texto }
    fun onPedirEliminar(paciente: PacienteConConteo) = porEliminar.update { paciente }
    fun onDescartarEliminar() = porEliminar.update { null }
    fun onMensajeMostrado() = mensaje.update { null }

    fun onConfirmarEliminar() {
        val paciente = porEliminar.value ?: return
        porEliminar.value = null
        viewModelScope.launch {
            repo.eliminarPaciente(paciente.paciente.id)
            mensaje.value = "Paciente eliminado"
        }
    }
}
