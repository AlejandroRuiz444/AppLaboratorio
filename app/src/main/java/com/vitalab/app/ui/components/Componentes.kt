package com.vitalab.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.Examen
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.textoEntrega

@Composable
fun EncabezadoPantalla(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Etiqueta pequeña; en color coral cuando es un aviso (por ejemplo, ayuno). */
@Composable
fun Etiqueta(texto: String, alerta: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Surface(
        color = if (alerta) c.tertiaryContainer else c.secondaryContainer,
        contentColor = if (alerta) c.onTertiaryContainer else c.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun ExamenCard(examen: Examen, enOrden: Boolean, onAgregarQuitar: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (enOrden) c.primaryContainer else c.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(examen.categoria.etiqueta.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
                    Text(examen.nombre, style = MaterialTheme.typography.titleMedium)
                }
                Text(formatoPesos(examen.precio), style = MaterialTheme.typography.titleMedium, color = c.primary, fontWeight = FontWeight.Bold)
            }
            Text(examen.descripcion, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta(if (examen.ayunoHoras > 0) "Ayuno ${examen.ayunoHoras} h" else "Sin ayuno", alerta = examen.ayunoHoras > 0)
                Etiqueta(textoEntrega(examen.horasEntrega))
                Spacer(Modifier.weight(1f))
                val relleno = PaddingValues(horizontal = 12.dp)
                if (enOrden) {
                    OutlinedButton(onClick = onAgregarQuitar, contentPadding = relleno) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quitar")
                    }
                } else {
                    Button(onClick = onAgregarQuitar, contentPadding = relleno) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
            }
        }
    }
}

@Composable
fun FilaResumen(etiqueta: String, valor: String, destacado: Boolean = false) {
    val estilo = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = estilo, modifier = Modifier.weight(1f))
        Text(
            valor,
            style = estilo,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TarjetaResumenCosto(resumen: Resumen, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen de costos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            FilaResumen("Subtotal", formatoPesos(resumen.subtotal))
            if (resumen.descuentoPaquete > 0) FilaResumen("Descuento paquete (10%)", "-" + formatoPesos(resumen.descuentoPaquete))
            if (resumen.descuentoAdultoMayor > 0) FilaResumen("Descuento adulto mayor (15%)", "-" + formatoPesos(resumen.descuentoAdultoMayor))
            if (resumen.recargoDomicilio > 0) FilaResumen("Toma a domicilio", "+" + formatoPesos(resumen.recargoDomicilio))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            FilaResumen("Total a pagar", formatoPesos(resumen.total), destacado = true)
        }
    }
}

@Composable
fun TarjetaIndicaciones(indicaciones: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Preparación para tu toma de muestra", style = MaterialTheme.typography.titleMedium)
            }
            indicaciones.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, mensaje: String, textoBoton: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onClick) { Text(textoBoton) }
    }
}

/** Campo de texto que muestra su mensaje de error debajo cuando corresponde. */
@Composable
fun CampoValidado(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    mostrarError: Boolean,
    icono: ImageVector,
    teclado: KeyboardType = KeyboardType.Text
) {
    val conError = mostrarError && error != null
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        isError = conError,
        supportingText = if (conError) ({ Text(error.orEmpty()) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
