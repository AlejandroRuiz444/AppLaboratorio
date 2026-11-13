package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.local.CitasPorPaciente
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.ExamenPopular
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class EstadisticasUiState(
    val cargando: Boolean = true,
    /** Siempre trae las tres claves (con 0 si no hay citas en ese estado). */
    val citasPorEstado: Map<EstadoCita, Int> = EstadoCita.entries.associateWith { 0 },
    val totalCitas: Int = 0,
    /** Pesos cobrados en citas COMPLETADAS. */
    val ingresos: Int = 0,
    /** Pesos descontados (paquete + adulto mayor) en citas no canceladas. */
    val ahorro: Int = 0,
    /** Top 5 por veces pedido, sin contar citas canceladas. */
    val topExamenes: List<ExamenPopular> = emptyList(),
    /** Todas las citas de cada paciente, de mayor a menor. */
    val citasPorPaciente: List<CitasPorPaciente> = emptyList()
) {
    val hayDatos: Boolean get() = totalCitas > 0
}

/** Estadísticas: todas son consultas agregadas (SQL) de Room, se actualizan solas. */
class EstadisticasViewModel(repo: LabRepository) : ViewModel() {

    val uiState: StateFlow<EstadisticasUiState> = repo.estadisticas().map { e ->
        EstadisticasUiState(
            cargando = false,
            citasPorEstado = e.citasPorEstado,
            totalCitas = e.totalCitas,
            ingresos = e.ingresos,
            ahorro = e.ahorro,
            topExamenes = e.topExamenes,
            citasPorPaciente = e.citasPorPaciente
        )
    }.stateIn(viewModelScope, Suscripcion, EstadisticasUiState())
}
