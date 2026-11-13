package com.vitalab.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Categoria
import com.vitalab.app.data.Examen
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.validarHorasAyuno
import com.vitalab.app.data.validarHorasEntrega
import com.vitalab.app.data.validarNombreExamen
import com.vitalab.app.data.validarPrecio
import com.vitalab.app.data.validarTextoObligatorio
import com.vitalab.app.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamenFormUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrado: Boolean = false,
    val nombre: String = "",
    val categoria: Categoria = Categoria.HEMATOLOGIA,
    /** Los números se editan como texto; el ViewModel los valida y convierte. */
    val precio: String = "",
    val descripcion: String = "",
    val muestra: String = "Sangre",
    val ayunoHoras: String = "0",
    val horasEntrega: String = "24",
    val indicacion: String = "",
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorDescripcion: String? = null,
    val errorMuestra: String? = null,
    val errorAyuno: String? = null,
    val errorEntrega: String? = null,
    val intentado: Boolean = false,
    val guardando: Boolean = false,
    /** Pasa a true cuando se guardó: la pantalla debe volver atrás. */
    val guardado: Boolean = false
) {
    val hayErrores: Boolean
        get() = listOf(errorNombre, errorPrecio, errorDescripcion, errorMuestra, errorAyuno, errorEntrega).any { it != null }
}

/** Crear (ruta sin id) o editar (ruta con `id`) un examen del catálogo. */
class ExamenFormViewModel(
    private val repo: LabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Int? = savedStateHandle.get<Long>(Rutas.ARG_ID)?.toInt()
    private var original: Examen? = null

    private val _estado = MutableStateFlow(ExamenFormUiState(esEdicion = id != null, cargando = id != null))
    val uiState: StateFlow<ExamenFormUiState> = _estado.asStateFlow()

    init {
        if (id != null) {
            viewModelScope.launch {
                val examen = repo.obtenerExamen(id)
                original = examen
                _estado.update {
                    if (examen == null) it.copy(cargando = false, noEncontrado = true)
                    else conErrores(
                        it.copy(
                            cargando = false,
                            nombre = examen.nombre,
                            categoria = examen.categoria,
                            precio = examen.precio.toString(),
                            descripcion = examen.descripcion,
                            muestra = examen.muestra,
                            ayunoHoras = examen.ayunoHoras.toString(),
                            horasEntrega = examen.horasEntrega.toString(),
                            indicacion = examen.indicacion
                        )
                    )
                }
            }
        }
    }

    fun onNombreChange(v: String) = cambiar { it.copy(nombre = v) }
    fun onCategoriaChange(v: Categoria) = cambiar { it.copy(categoria = v) }
    fun onPrecioChange(v: String) = cambiar { it.copy(precio = v) }
    fun onDescripcionChange(v: String) = cambiar { it.copy(descripcion = v) }
    fun onMuestraChange(v: String) = cambiar { it.copy(muestra = v) }
    fun onAyunoChange(v: String) = cambiar { it.copy(ayunoHoras = v) }
    fun onEntregaChange(v: String) = cambiar { it.copy(horasEntrega = v) }
    fun onIndicacionChange(v: String) = cambiar { it.copy(indicacion = v) }

    fun onGuardar() {
        cambiar { it.copy(intentado = true) }
        val s = _estado.value
        if (s.hayErrores || s.guardando || s.guardado) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repo.guardarExamen(
                Examen(
                    id = original?.id ?: 0,
                    nombre = s.nombre.trim(),
                    categoria = s.categoria,
                    precio = s.precio.trim().toInt(),
                    descripcion = s.descripcion.trim(),
                    muestra = s.muestra.trim(),
                    ayunoHoras = s.ayunoHoras.trim().toInt(),
                    horasEntrega = s.horasEntrega.trim().toInt(),
                    indicacion = s.indicacion.trim(),
                    // Activar/desactivar se hace desde el catálogo, no desde este formulario.
                    activo = original?.activo ?: true
                )
            )
            _estado.update { it.copy(guardando = false, guardado = true) }
        }
    }

    private fun cambiar(transformar: (ExamenFormUiState) -> ExamenFormUiState) =
        _estado.update { conErrores(transformar(it)) }

    private fun conErrores(s: ExamenFormUiState) = s.copy(
        errorNombre = errorVisible(s.nombre, s.intentado, ::validarNombreExamen),
        errorPrecio = errorVisible(s.precio, s.intentado, ::validarPrecio),
        errorDescripcion = errorVisible(s.descripcion, s.intentado) { validarTextoObligatorio(it, "la descripción") },
        errorMuestra = errorVisible(s.muestra, s.intentado) { validarTextoObligatorio(it, "el tipo de muestra") },
        errorAyuno = errorVisible(s.ayunoHoras, s.intentado, ::validarHorasAyuno),
        errorEntrega = errorVisible(s.horasEntrega, s.intentado, ::validarHorasEntrega)
    )
}
