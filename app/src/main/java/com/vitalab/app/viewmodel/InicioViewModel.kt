package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Examen
import com.vitalab.app.data.FRANJAS
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class InicioUiState(
    /** La cita PROGRAMADA más próxima (hoy o después), o null si no hay. */
    val proximaCita: CitaCompleta? = null,
    val examenesEnOrden: Int = 0,
    /** Exámenes activos del catálogo. */
    val totalExamenes: Int = 0,
    val destacados: List<Examen> = emptyList(),
    val cargando: Boolean = true
)

class InicioViewModel(
    repo: LabRepository,
    hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<InicioUiState> = combine(
        repo.citasProgramadasDesde(hoy().toEpochDay()), repo.orden(), repo.examenesActivos()
    ) { programadas, orden, activos ->
        InicioUiState(
            // Room ordena por día; dentro del mismo día se desempata por la posición de la hora en FRANJAS.
            proximaCita = programadas.minWithOrNull(compareBy({ it.cita.fecha }, { FRANJAS.indexOf(it.cita.hora) })),
            examenesEnOrden = orden.size,
            totalExamenes = activos.size,
            destacados = activos.filter { it.id in DESTACADOS },
            cargando = false
        )
    }.stateIn(viewModelScope, Suscripcion, InicioUiState())

    private companion object {
        /** Cuadro hemático, glucosa, perfil lipídico, parcial de orina, TSH (los de la tarjeta "Destacados"; es una selección fija, no un ranking). */
        val DESTACADOS = setOf(1, 2, 3, 8, 6)
    }
}
