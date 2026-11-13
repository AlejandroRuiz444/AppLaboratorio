package com.vitalab.app.ui.screens.citas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.franjasDisponibles
import com.vitalab.app.data.proximasFechas
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.PasoFormulario
import com.vitalab.app.ui.components.SelectorFecha
import com.vitalab.app.ui.components.SelectorHora
import com.vitalab.app.ui.components.SelectorModalidad
import com.vitalab.app.ui.components.TarjetaResumenCosto
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.ReprogramarCitaViewModel
import com.vitalab.app.viewmodel.ReprogramarUiState
import java.time.LocalDate

/** Reprogramar una cita PROGRAMADA: nueva fecha, hora y modalidad (con dirección si es a domicilio). */
@Composable
fun ReprogramarCitaScreen(viewModel: ReprogramarCitaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ReprogramarContenido(
        estado = estado,
        onVolver = onVolver,
        onFechaChange = viewModel::onFechaChange,
        onHoraChange = viewModel::onHoraChange,
        onDomicilioChange = viewModel::onDomicilioChange,
        onDireccionChange = viewModel::onDireccionChange,
        onGuardar = viewModel::onGuardar
    )
}

@Composable
internal fun ReprogramarContenido(
    estado: ReprogramarUiState,
    onVolver: () -> Unit,
    onFechaChange: (Long) -> Unit,
    onHoraChange: (String) -> Unit,
    onDomicilioChange: (Boolean) -> Unit,
    onDireccionChange: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }

    Scaffold(modifier = modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver sin guardar") }
                EncabezadoPantalla(
                    "Reprogramar cita",
                    if (estado.codigo.isNotEmpty()) "${estado.codigo} · ${estado.paciente}" else ""
                )
            }
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                estado.noEncontrada -> EstadoVacio(Icons.Filled.Info, "No encontramos esta cita.", "Volver", onVolver)
                estado.noReprogramable -> EstadoVacio(
                    Icons.Filled.Info, "Esta cita ya no se puede reprogramar porque no está programada.", "Volver", onVolver
                )
                else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PasoFormulario(1, "Fecha", completo = estado.fecha != null) {
                        SelectorFecha(estado.fechasDisponibles, estado.fecha, onFechaChange, estado.errorFecha)
                    }
                    PasoFormulario(2, "Hora", completo = estado.hora != null && estado.hora in estado.franjas) {
                        SelectorHora(estado.franjas, estado.hora, onHoraChange, estado.ayunoHoras, estado.errorHora)
                    }
                    PasoFormulario(3, "Modalidad", completo = !estado.domicilio || (estado.direccion.isNotBlank() && estado.errorDireccion == null)) {
                        SelectorModalidad(estado.domicilio, estado.direccion, estado.errorDireccion, onDomicilioChange, onDireccionChange)
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaResumenCosto(estado.resumen)
                        estado.errorGeneral?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        Button(onClick = onGuardar, enabled = !estado.guardando, modifier = Modifier.fillMaxWidth()) {
                            Text(if (estado.guardando) "Guardando…" else "Guardar cambios")
                        }
                        if (estado.intentado && listOf(estado.errorFecha, estado.errorHora, estado.errorDireccion).any { it != null }) {
                            Text(
                                "Revisa los campos marcados en rojo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun estadoDeEjemplo(domicilio: Boolean = false, intentado: Boolean = false): ReprogramarUiState {
    val hoy = LocalDate.of(2026, 11, 12)
    val fechas = proximasFechas(hoy).map { it.toEpochDay() }
    return ReprogramarUiState(
        cargando = false,
        codigo = "VL-4821",
        paciente = "Marta Ruiz Gómez",
        fechasDisponibles = fechas,
        franjas = franjasDisponibles(8),
        ayunoHoras = 8,
        fecha = fechas[1],
        hora = if (intentado) null else "7:00 a.m.",
        domicilio = domicilio,
        direccion = if (domicilio) "Cra 15 # 82-30" else "",
        resumen = Resumen(95_000, 9_500, 0, if (domicilio) 20_000 else 0),
        errorHora = if (intentado) "Selecciona una hora" else null,
        intentado = intentado
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReprogramarPreview() {
    VitaLabTheme { ReprogramarContenido(estadoDeEjemplo(), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReprogramarDomicilioConErrorPreview() {
    VitaLabTheme { ReprogramarContenido(estadoDeEjemplo(domicilio = true, intentado = true), {}, {}, {}, {}, {}, {}) }
}
