package com.vitalab.app.ui.screens.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.local.CitasPorPaciente
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.ExamenPopular
import com.vitalab.app.ui.components.EncabezadoConAtras
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.ItemBarra
import com.vitalab.app.ui.components.TarjetaBarras
import com.vitalab.app.ui.components.TarjetaCitasPorEstado
import com.vitalab.app.ui.components.TarjetaDinero
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.EstadisticasUiState
import com.vitalab.app.viewmodel.EstadisticasViewModel

/** Estadísticas: resumen de citas, ingresos y ahorro, exámenes más pedidos y citas por paciente (todo desde Room). */
@Composable
fun EstadisticasScreen(
    viewModel: EstadisticasViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    EstadisticasContenido(estado, onVolver)
}

@Composable
internal fun EstadisticasContenido(estado: EstadisticasUiState, onVolver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoConAtras("Estadísticas", "Resumen de la actividad del laboratorio", onVolver)
        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !estado.hayDatos -> EstadoVacio(
                Icons.Filled.DateRange,
                "Todavía no hay citas. Cuando agendes la primera verás aquí las citas por estado, los exámenes más pedidos y los ingresos.",
                "Volver",
                onVolver
            )
            // ponytail: Column con scroll; si "citas por paciente" llegara a cientos de filas, pasarla a LazyColumn.
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TarjetaCitasPorEstado(estado.citasPorEstado, estado.totalCitas)
                TarjetaDinero(estado.ingresos, estado.ahorro)
                TarjetaBarras(
                    titulo = "Exámenes más solicitados",
                    items = estado.topExamenes.map { ItemBarra(it.nombre, it.veces) },
                    unidadSingular = "vez", unidadPlural = "veces",
                    textoVacio = "Aún no hay exámenes en citas activas; las canceladas no cuentan.",
                    colorBarra = MaterialTheme.colorScheme.primary
                )
                TarjetaBarras(
                    titulo = "Citas por paciente",
                    items = estado.citasPorPaciente.map { ItemBarra(it.nombre, it.total) },
                    unidadSingular = "cita", unidadPlural = "citas",
                    textoVacio = "Aún no hay pacientes con citas.",
                    colorBarra = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EstadisticasConDatosPreview() {
    VitaLabTheme {
        EstadisticasContenido(
            EstadisticasUiState(
                cargando = false,
                citasPorEstado = mapOf(EstadoCita.PROGRAMADA to 4, EstadoCita.COMPLETADA to 7, EstadoCita.CANCELADA to 1),
                totalCitas = 12, ingresos = 1_245_000, ahorro = 186_750,
                topExamenes = listOf(
                    ExamenPopular(1, "Hemograma completo", 9), ExamenPopular(3, "Perfil lipídico", 6),
                    ExamenPopular(6, "Glucosa en ayunas", 5), ExamenPopular(8, "TSH", 2), ExamenPopular(2, "Uroanálisis", 1)
                ),
                citasPorPaciente = listOf(
                    CitasPorPaciente(1, "Marta Lucía Ruiz", 5), CitasPorPaciente(2, "Andrés Felipe Gómez", 4), CitasPorPaciente(3, "Camila Torres", 3)
                )
            ),
            onVolver = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EstadisticasVaciaPreview() {
    VitaLabTheme { EstadisticasContenido(EstadisticasUiState(cargando = false), onVolver = {}) }
}
