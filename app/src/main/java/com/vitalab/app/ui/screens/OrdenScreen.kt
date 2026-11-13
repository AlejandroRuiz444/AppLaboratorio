package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.Examen
import com.vitalab.app.data.MINIMO_PAQUETE
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.TarjetaIndicaciones
import com.vitalab.app.ui.components.TarjetaResumenCosto
import com.vitalab.app.ui.components.conteo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.OrdenUiState
import com.vitalab.app.viewmodel.OrdenViewModel

/**
 * Mi orden: los exámenes elegidos (guardados en Room, sobreviven al cierre de la app), el descuento de
 * paquete, el switch de adulto mayor, el total y la preparación. El botón "Agendar cita" queda fijo abajo.
 */
@Composable
fun OrdenScreen(viewModel: OrdenViewModel, onVerExamenes: () -> Unit, onAgendar: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    OrdenContenido(estado, viewModel::onAdultoMayorChange, viewModel::onQuitar, onVerExamenes, onAgendar)
}

@Composable
internal fun OrdenContenido(
    estado: OrdenUiState,
    onAdultoMayorChange: (Boolean) -> Unit,
    onQuitar: (Int) -> Unit,
    onVerExamenes: () -> Unit,
    onAgendar: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            "Mi orden",
            when {
                estado.cargando -> ""
                estado.examenes.isEmpty() -> "Aún no has agregado exámenes"
                else -> conteo(estado.cantidad, "examen seleccionado", "exámenes seleccionados")
            }
        )
        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            estado.examenes.isEmpty() ->
                EstadoVacio(Icons.Filled.ShoppingCart, "Agrega los exámenes que te ordenó tu médico.", "Ver exámenes", onVerExamenes)

            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.examenes, key = { it.id }) { examen -> FilaExamenOrden(examen, onQuitar = { onQuitar(examen.id) }) }
                    item { AvisoPaquete(estado.faltanParaPaquete, onVerExamenes) }
                    item { TarjetaAdultoMayor(estado.adultoMayor, onAdultoMayorChange) }
                    item { TarjetaResumenCosto(estado.resumen) }
                    item { TarjetaIndicaciones(estado.indicaciones) }
                }
                BarraAgendar(estado.resumen.total, onAgendar)
            }
        }
    }
}

@Composable
private fun FilaExamenOrden(examen: Examen, onQuitar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(examen.nombre, style = MaterialTheme.typography.titleSmall)
                Text("Muestra: ${examen.muestra}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatoPesos(examen.precio), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onQuitar) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar ${examen.nombre} de la orden", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Cuántos exámenes faltan para el 10 % por paquete (o confirmación de que ya aplica). */
@Composable
private fun AvisoPaquete(faltan: Int, onVerExamenes: () -> Unit) {
    val aplicado = faltan == 0
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (aplicado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (aplicado) Icons.Filled.CheckCircle else Icons.Filled.Info, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                if (aplicado) "Descuento por paquete aplicado: 10%."
                else "Agrega ${conteo(faltan, "examen más", "exámenes más")} y obtén 10% de descuento por paquete.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp)
            )
            if (!aplicado) TextButton(onClick = onVerExamenes) { Text("Agregar más") }
        }
    }
}

@Composable
private fun TarjetaAdultoMayor(adultoMayor: Boolean, onCambio: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        // Toda la fila es el interruptor: objetivo táctil grande y lector de pantalla con un solo elemento.
        Row(
            Modifier.fillMaxWidth().toggleable(value = adultoMayor, role = Role.Switch, onValueChange = onCambio).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Paciente adulto mayor", style = MaterialTheme.typography.titleSmall)
                Text("Mayores de 60 años: 15% de descuento", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = adultoMayor, onCheckedChange = null)
        }
    }
}

/** Total y acción principal, siempre a la vista aunque la lista sea larga. */
@Composable
private fun BarraAgendar(total: Int, onAgendar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Total a pagar", style = MaterialTheme.typography.labelMedium)
                Text(formatoPesos(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onAgendar) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Agendar cita")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrdenPreview() {
    val examenes = catalogoExamenes.take(3)
    VitaLabTheme {
        OrdenContenido(
            OrdenUiState(
                examenes = examenes, adultoMayor = true,
                resumen = calcularResumen(examenes, adultoMayor = true, domicilio = false),
                indicaciones = indicacionesPreparacion(examenes), faltanParaPaquete = 0, cargando = false
            ),
            onAdultoMayorChange = {}, onQuitar = {}, onVerExamenes = {}, onAgendar = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdenFaltanPreview() {
    val examenes = catalogoExamenes.take(1)
    VitaLabTheme {
        OrdenContenido(
            OrdenUiState(
                examenes = examenes, resumen = calcularResumen(examenes, adultoMayor = false, domicilio = false),
                indicaciones = indicacionesPreparacion(examenes), faltanParaPaquete = MINIMO_PAQUETE - 1, cargando = false
            ),
            onAdultoMayorChange = {}, onQuitar = {}, onVerExamenes = {}, onAgendar = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdenVaciaPreview() {
    VitaLabTheme {
        OrdenContenido(OrdenUiState(cargando = false), onAdultoMayorChange = {}, onQuitar = {}, onVerExamenes = {}, onAgendar = {})
    }
}
