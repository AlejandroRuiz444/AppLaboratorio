package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CitasUiState(
    /** Ya filtradas. Primero las programadas (la más próxima arriba), después el historial. */
    val citas: List<CitaCompleta> = emptyList(),
    val estadoFiltro: EstadoCita? = null,
    val busqueda: String = "",
    val cargando: Boolean = true
) {
    val hayFiltros: Boolean get() = estadoFiltro != null || busqueda.isNotBlank()
}

/** "Mis citas": lista con filtro por estado y búsqueda por paciente/documento/código. */
class CitasViewModel(private val repo: LabRepository) : ViewModel() {

    private data class Filtros(val estado: EstadoCita? = null, val busqueda: String = "")

    private val filtros = MutableStateFlow(Filtros())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CitasUiState> = combine(
        filtros, filtros.flatMapLatest { repo.citas(it.estado, it.busqueda) }
    ) { f, citas ->
        CitasUiState(citas, f.estado, f.busqueda, cargando = false)
    }.stateIn(viewModelScope, Suscripcion, CitasUiState())

    fun onEstadoFiltroChange(estado: EstadoCita?) = filtros.update { it.copy(estado = estado) }
    fun onBusquedaChange(texto: String) = filtros.update { it.copy(busqueda = texto) }
    fun onLimpiarFiltros() = filtros.update { Filtros() }
}
