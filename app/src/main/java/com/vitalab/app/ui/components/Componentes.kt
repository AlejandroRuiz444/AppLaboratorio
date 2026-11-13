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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.Examen
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.textoEntrega

/** "1 examen" / "3 exámenes": concordancia de número sin el "(es)" de los textos provisionales. */
fun conteo(cantidad: Int, singular: String, plural: String) = "$cantidad ${if (cantidad == 1) singular else plural}"

/** Gráfica de barras dibujada a mano: `material-icons-core` no trae ningún ícono de estadísticas. */
val IconoEstadisticas: ImageVector by lazy {
    ImageVector.Builder("Estadisticas", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 9.2f); horizontalLineToRelative(3f); verticalLineTo(19f); horizontalLineTo(5f); close()
            moveTo(10.6f, 5f); horizontalLineToRelative(2.8f); verticalLineToRelative(14f); horizontalLineToRelative(-2.8f); close()
            moveTo(16.2f, 13f); horizontalLineTo(19f); verticalLineToRelative(6f); horizontalLineToRelative(-2.8f); close()
        }
    }.build()
}

@Composable
fun EncabezadoPantalla(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Igual que [EncabezadoPantalla] pero con flecha para volver: pantallas de formulario o detalle. */
@Composable
fun EncabezadoConVolver(titulo: String, subtitulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

/**
 * Tarjeta de un examen del catálogo. Con `modoAdmin` añade las acciones de administración (editar y
 * desactivar/reactivar); un examen desactivado se ve atenuado y ya no se puede agregar a la orden.
 */
@Composable
fun ExamenCard(
    examen: Examen,
    enOrden: Boolean,
    onAgregarQuitar: () -> Unit,
    modifier: Modifier = Modifier,
    modoAdmin: Boolean = false,
    onEditar: () -> Unit = {},
    onCambiarActivo: () -> Unit = {}
) {
    val c = MaterialTheme.colorScheme
    val inactivo = !examen.activo
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                inactivo -> c.surfaceVariant
                enOrden -> c.primaryContainer
                else -> c.surfaceContainerHighest
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (inactivo) 0.dp else 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(examen.categoria.etiqueta.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (inactivo) c.onSurfaceVariant else c.secondary)
                    Text(examen.nombre, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    formatoPesos(examen.precio),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (inactivo) c.onSurfaceVariant else c.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(examen.descripcion, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta(if (examen.ayunoHoras > 0) "Ayuno ${examen.ayunoHoras} h" else "Sin ayuno", alerta = examen.ayunoHoras > 0)
                Etiqueta(textoEntrega(examen.horasEntrega))
                if (inactivo) Etiqueta("Desactivado")
                Spacer(Modifier.weight(1f))
                val relleno = PaddingValues(horizontal = 12.dp)
                // Un examen desactivado no se puede pedir: solo se administra (fila de abajo).
                if (!inactivo && enOrden) {
                    OutlinedButton(
                        onClick = onAgregarQuitar,
                        contentPadding = relleno,
                        modifier = Modifier.semantics { contentDescription = "Quitar ${examen.nombre} de la orden" }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quitar")
                    }
                } else if (!inactivo) {
                    Button(
                        onClick = onAgregarQuitar,
                        contentPadding = relleno,
                        modifier = Modifier.semantics { contentDescription = "Agregar ${examen.nombre} a la orden" }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
            }
            if (modoAdmin) {
                HorizontalDivider(Modifier.padding(top = 8.dp), color = c.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onEditar,
                        modifier = Modifier.semantics { contentDescription = "Editar ${examen.nombre}" }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                    TextButton(
                        onClick = onCambiarActivo,
                        colors = if (inactivo) ButtonDefaults.textButtonColors() else ButtonDefaults.textButtonColors(contentColor = c.error),
                        modifier = Modifier.semantics {
                            contentDescription = (if (inactivo) "Reactivar " else "Desactivar ") + examen.nombre
                        }
                    ) { Text(if (inactivo) "Reactivar" else "Desactivar") }
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

/**
 * Campo de texto que muestra su mensaje de error debajo cuando corresponde.
 * Los parámetros desde `modifier` son opcionales: sirven a los formularios que necesitan prefijo ("$"),
 * sufijo ("h"), texto de ayuda o varias líneas. El ícono ahora puede omitirse.
 */
@Composable
fun CampoValidado(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    mostrarError: Boolean,
    icono: ImageVector? = null,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    prefijo: String? = null,
    sufijo: String? = null,
    minLineas: Int = 1,
    maxLineas: Int = 1,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Default
) {
    val conError = mostrarError && error != null
    val textoApoyo = if (conError) error else ayuda
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        leadingIcon = if (icono != null) ({ Icon(icono, contentDescription = null) }) else null,
        prefix = if (prefijo != null) ({ Text(prefijo) }) else null,
        suffix = if (sufijo != null) ({ Text(sufijo) }) else null,
        isError = conError,
        supportingText = if (textoApoyo != null) ({ Text(textoApoyo) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, capitalization = capitalizacion, imeAction = imeAction),
        singleLine = maxLineas == 1,
        minLines = minLineas,
        maxLines = maxLineas,
        modifier = modifier.fillMaxWidth()
    )
}
