package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Examen
import com.vitalab.app.data.MINIMO_PAQUETE
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.repository.LabRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdenUiState(
    val examenes: List<Examen> = emptyList(),
    val adultoMayor: Boolean = false,
    /** Costos con descuento de paquete y de adulto mayor. Sin recargo de domicilio: eso se elige al agendar. */
    val resumen: Resumen = Resumen(0, 0, 0, 0),
    val indicaciones: List<String> = emptyList(),
    /** Exámenes que faltan para el descuento de paquete (0 = ya aplica). */
    val faltanParaPaquete: Int = MINIMO_PAQUETE,
    val cargando: Boolean = true
) {
    val cantidad: Int get() = examenes.size
}

/** La orden en curso. Vive en Room, así que sobrevive a cerrar la app. */
class OrdenViewModel(private val repo: LabRepository) : ViewModel() {

    val uiState: StateFlow<OrdenUiState> = combine(repo.orden(), repo.adultoMayor()) { examenes, adultoMayor ->
        OrdenUiState(
            examenes = examenes,
            adultoMayor = adultoMayor,
            resumen = calcularResumen(examenes, adultoMayor, domicilio = false),
            indicaciones = indicacionesPreparacion(examenes),
            faltanParaPaquete = (MINIMO_PAQUETE - examenes.size).coerceAtLeast(0),
            cargando = false
        )
    }.stateIn(viewModelScope, Suscripcion, OrdenUiState())

    fun onAdultoMayorChange(valor: Boolean) {
        viewModelScope.launch { repo.setAdultoMayor(valor) }
    }

    fun onQuitar(examenId: Int) {
        viewModelScope.launch { repo.quitarDeOrden(examenId) }
    }
}
