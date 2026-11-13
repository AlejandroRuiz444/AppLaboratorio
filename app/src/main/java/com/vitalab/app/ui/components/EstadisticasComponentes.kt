package com.vitalab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.local.EstadoCita

/** Una fila de un gráfico de barras: nombre y valor. */
data class ItemBarra(val etiqueta: String, val valor: Int)

/** Citas por estado (y el total), como filas de resumen. */
@Composable
fun TarjetaCitasPorEstado(citasPorEstado: Map<EstadoCita, Int>, total: Int, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Citas por estado", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                Etiqueta("$total en total")
            }
            Spacer(Modifier.height(8.dp))
            EstadoCita.entries.forEach { estado -> FilaResumen("${estado.etiqueta}s", "${citasPorEstado[estado] ?: 0}") }
        }
    }
}

/** Ingresos (solo citas completadas) y ahorro por descuentos (sin canceladas), con la regla a la vista. */
@Composable
fun TarjetaDinero(ingresos: Int, ahorro: Int, modifier: Modifier = Modifier) {
    val nota = MaterialTheme.typography.bodySmall
    val colorNota = MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Ingresos y ahorro", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            FilaResumen("Ingresos", formatoPesos(ingresos), destacado = true)
            Text("Solo de citas completadas", style = nota, color = colorNota)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            FilaResumen("Ahorro por descuentos", formatoPesos(ahorro))
            Text("Paquete y adulto mayor; no cuenta citas canceladas", style = nota, color = colorNota)
        }
    }
}

/**
 * Lista con barras horizontales. El valor va escrito junto a cada barra, así que se lee sin depender del color;
 * cada fila se anuncia completa ("Perfil lipídico: 4 veces") y la barra en sí queda oculta para el lector de pantalla.
 */
@Composable
fun TarjetaBarras(
    titulo: String,
    items: List<ItemBarra>,
    unidadSingular: String,
    unidadPlural: String,
    textoVacio: String,
    colorBarra: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (items.isEmpty()) {
                Text(textoVacio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maximo = items.maxOf { it.valor }.coerceAtLeast(1)
                items.forEach { FilaBarra(it, maximo, textoConteo(it.valor, unidadSingular, unidadPlural), colorBarra) }
            }
        }
    }
}

@Composable
private fun FilaBarra(item: ItemBarra, maximo: Int, valorTexto: String, color: Color) {
    // Una barra con valor > 0 nunca queda por debajo de un punto visible.
    val fraccion = if (item.valor <= 0) 0f else (item.valor.toFloat() / maximo).coerceIn(0.03f, 1f)
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "${item.etiqueta}: $valorTexto" }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(item.etiqueta, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(valorTexto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}
