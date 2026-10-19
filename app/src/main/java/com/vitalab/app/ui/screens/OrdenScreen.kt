package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.Examen
import com.vitalab.app.data.MINIMO_PAQUETE
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.TarjetaIndicaciones
import com.vitalab.app.ui.components.TarjetaResumenCosto

@Composable
fun OrdenScreen(
    orden: List<Examen>,
    adultoMayor: Boolean,
    onAdultoMayorChange: (Boolean) -> Unit,
    onQuitar: (Examen) -> Unit,
    onVerExamenes: () -> Unit,
    onAgendar: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Mi orden", if (orden.isEmpty()) "Aún no has agregado exámenes" else "${orden.size} examen(es) seleccionados")
        if (orden.isEmpty()) {
            EstadoVacio(Icons.Filled.ShoppingCart, "Agrega los exámenes que te ordenó tu médico.", "Ver exámenes", onVerExamenes)
        } else {
            ContenidoOrden(orden, adultoMayor, onAdultoMayorChange, onQuitar, onAgendar)
        }
    }
}

@Composable
private fun ContenidoOrden(
    orden: List<Examen>,
    adultoMayor: Boolean,
    onAdultoMayorChange: (Boolean) -> Unit,
    onQuitar: (Examen) -> Unit,
    onAgendar: () -> Unit
) {
    // Se recalcula en cada recomposición: cualquier cambio en la orden o el switch actualiza el total.
    val resumen = calcularResumen(orden, adultoMayor, domicilio = false)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orden, key = { it.id }) { examen -> FilaExamenOrden(examen, onQuitar = { onQuitar(examen) }) }
        item {
            val faltan = MINIMO_PAQUETE - orden.size
            Text(
                if (faltan > 0) "Agrega $faltan examen(es) más y obtén 10% de descuento por paquete."
                else "¡Descuento por paquete aplicado!",
                style = MaterialTheme.typography.bodyMedium,
                color = if (faltan > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        }
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Paciente adulto mayor", style = MaterialTheme.typography.titleSmall)
                        Text("Mayores de 60 años: 15% de descuento", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = adultoMayor, onCheckedChange = onAdultoMayorChange)
                }
            }
        }
        item { TarjetaResumenCosto(resumen) }
        item { TarjetaIndicaciones(indicacionesPreparacion(orden)) }
        item {
            Button(onClick = onAgendar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agendar toma de muestra")
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
                Icon(Icons.Filled.Delete, contentDescription = "Quitar ${examen.nombre}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
