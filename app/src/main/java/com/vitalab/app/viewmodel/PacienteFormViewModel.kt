package com.vitalab.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.validarCorreo
import com.vitalab.app.data.validarDocumento
import com.vitalab.app.data.validarNombre
import com.vitalab.app.data.validarTelefono
import com.vitalab.app.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacienteFormUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrado: Boolean = false,
    val nombre: String = "",
    val documento: String = "",
    val telefono: String = "",
    val correo: String = "",
    val errorNombre: String? = null,
    /** Incluye el error de "documento ya registrado" que devuelve el repositorio. */
    val errorDocumento: String? = null,
    val errorTelefono: String? = null,
    val errorCorreo: String? = null,
    val intentado: Boolean = false,
    val guardando: Boolean = false,
    /** Pasa a true cuando se guardó: la pantalla debe volver atrás. */
    val guardado: Boolean = false
) {
    val hayErrores: Boolean
        get() = listOf(errorNombre, errorDocumento, errorTelefono, errorCorreo).any { it != null }
}

/** Crear (ruta sin id) o editar (ruta con `id`) un paciente. */
class PacienteFormViewModel(
    private val repo: LabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Long? = savedStateHandle.get<Long>(Rutas.ARG_ID)
    private var original: Paciente? = null

    // Documento que el repositorio rechazó por repetido y su mensaje; el error se quita solo al cambiar el documento.
    private var documentoRechazado: Pair<String, String>? = null

    private val _estado = MutableStateFlow(PacienteFormUiState(esEdicion = id != null, cargando = id != null))
    val uiState: StateFlow<PacienteFormUiState> = _estado.asStateFlow()

    init {
        if (id != null) {
            viewModelScope.launch {
                val paciente = repo.obtenerPaciente(id)
                original = paciente
                _estado.update {
                    if (paciente == null) it.copy(cargando = false, noEncontrado = true)
                    else conErrores(
                        it.copy(
                            cargando = false,
                            nombre = paciente.nombre,
                            documento = paciente.documento,
                            telefono = paciente.telefono,
                            correo = paciente.correo
                        )
                    )
                }
            }
        }
    }

    fun onNombreChange(v: String) = cambiar { it.copy(nombre = v) }
    fun onDocumentoChange(v: String) = cambiar { it.copy(documento = v) }
    fun onTelefonoChange(v: String) = cambiar { it.copy(telefono = v) }
    fun onCorreoChange(v: String) = cambiar { it.copy(correo = v) }

    fun onGuardar() {
        cambiar { it.copy(intentado = true) }
        val s = _estado.value
        if (s.hayErrores || s.guardando || s.guardado) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val paciente = (original ?: Paciente(nombre = "", documento = "", telefono = "", correo = "")).copy(
                nombre = s.nombre.trim(),
                documento = s.documento.trim(),
                telefono = s.telefono.trim(),
                correo = s.correo.trim()
            )
            when (val r = repo.guardarPaciente(paciente)) {
                is Resultado.Exito -> _estado.update { it.copy(guardando = false, guardado = true) }
                is Resultado.Error -> {
                    documentoRechazado = paciente.documento to r.mensaje
                    _estado.update { conErrores(it.copy(guardando = false)) }
                }
            }
        }
    }

    private fun cambiar(transformar: (PacienteFormUiState) -> PacienteFormUiState) =
        _estado.update { conErrores(transformar(it)) }

    private fun conErrores(s: PacienteFormUiState): PacienteFormUiState {
        val rechazado = documentoRechazado?.takeIf { it.first == s.documento.trim() }?.second
        return s.copy(
            errorNombre = errorVisible(s.nombre, s.intentado, ::validarNombre),
            errorDocumento = rechazado ?: errorVisible(s.documento, s.intentado, ::validarDocumento),
            errorTelefono = errorVisible(s.telefono, s.intentado, ::validarTelefono),
            errorCorreo = errorVisible(s.correo, s.intentado, ::validarCorreo)
        )
    }
}
