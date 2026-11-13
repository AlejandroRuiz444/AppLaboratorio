package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Categoria
import com.vitalab.app.data.Examen
import com.vitalab.app.data.coincideBusqueda
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogoUiState(
    /** Exámenes ya filtrados. En modo admin incluye los desactivados (`activo == false`). */
    val examenes: List<Examen> = emptyList(),
    val totalActivos: Int = 0,
    val busqueda: String = "",
    val categoria: Categoria? = null,
    val modoAdmin: Boolean = false,
    val idsEnOrden: Set<Int> = emptySet(),
    val cantidadEnOrden: Int = 0,
    val totalEnOrden: Int = 0,
    val cargando: Boolean = true
) {
    val hayFiltros: Boolean get() = busqueda.isNotBlank() || categoria != null
}

/** Catálogo: lista con filtros, agregar/quitar de la orden y administración (activar/desactivar). */
class CatalogoViewModel(private val repo: LabRepository) : ViewModel() {

    private data class Filtros(val busqueda: String = "", val categoria: Categoria? = null, val modoAdmin: Boolean = false)

    private val filtros = MutableStateFlow(Filtros())

    val uiState: StateFlow<CatalogoUiState> = combine(repo.todosLosExamenes(), repo.orden(), filtros) { todos, orden, f ->
        val visibles = todos.filter { f.modoAdmin || it.activo }
        CatalogoUiState(
            examenes = visibles.filter {
                (f.categoria == null || it.categoria == f.categoria) &&
                    (coincideBusqueda(it.nombre, f.busqueda) || coincideBusqueda(it.categoria.etiqueta, f.busqueda))
            },
            totalActivos = todos.count { it.activo },
            busqueda = f.busqueda,
            categoria = f.categoria,
            modoAdmin = f.modoAdmin,
            idsEnOrden = orden.map { it.id }.toSet(),
            cantidadEnOrden = orden.size,
            totalEnOrden = orden.sumOf { it.precio },
            cargando = false
        )
    }.stateIn(viewModelScope, Suscripcion, CatalogoUiState())

    fun onBusquedaChange(texto: String) = filtros.update { it.copy(busqueda = texto) }
    fun onCategoriaChange(categoria: Categoria?) = filtros.update { it.copy(categoria = categoria) }
    fun onLimpiarFiltros() = filtros.update { it.copy(busqueda = "", categoria = null) }
    fun onModoAdminChange(activo: Boolean) = filtros.update { it.copy(modoAdmin = activo) }

    /** Agrega el examen a la orden, o lo quita si ya estaba. */
    fun onAlternarOrden(examenId: Int) {
        viewModelScope.launch { repo.alternarEnOrden(examenId) }
    }

    /** Desactiva un examen activo o reactiva uno desactivado (borrado lógico). */
    fun onCambiarActivo(examen: Examen) {
        viewModelScope.launch { repo.cambiarActivoExamen(examen.id, !examen.activo) }
    }
}
