package com.vitalab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.local.PacienteConConteo

/** Encabezado con flecha "Volver" a la izquierda (formulario de paciente y estadísticas). Reutiliza `EncabezadoPantalla`. */
@Composable
fun EncabezadoConAtras(titulo: String, subtitulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        EncabezadoPantalla(titulo, subtitulo, Modifier.weight(1f))
    }
}

/** "1 cita" / "3 citas". */
fun textoConteo(cantidad: Int, singular: String, plural: String): String = "$cantidad ${if (cantidad == 1) singular else plural}"

/**
 * Tarjeta de un paciente: nombre, documento, contacto y número de citas, con Editar y Eliminar.
 * El bloque de datos se lee de corrido en TalkBack; los botones llevan el nombre del paciente para no repetir "Editar".
 */
@Composable
fun TarjetaPaciente(item: PacienteConConteo, onEditar: () -> Unit, onEliminar: () -> Unit, modifier: Modifier = Modifier) {
    val paciente = item.paciente
    val citas = if (item.totalCitas == 0) "Sin citas" else textoConteo(item.totalCitas, "cita", "citas")
    val lectura = "${paciente.nombre}. Documento ${paciente.documento}. Celular ${paciente.telefono}. Correo ${paciente.correo}. $citas."
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Iniciales(paciente.nombre)
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f).padding(end = 8.dp).clearAndSetSemantics { contentDescription = lectura },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(paciente.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Etiqueta(citas)
                    }
                    DatoConIcono(Icons.Filled.AccountBox, paciente.documento)
                    DatoConIcono(Icons.Filled.Phone, paciente.telefono)
                    DatoConIcono(Icons.Filled.Email, paciente.correo)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEditar, modifier = Modifier.semantics { contentDescription = "Editar a ${paciente.nombre}" }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Editar")
                }
                TextButton(
                    onClick = onEliminar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.semantics { contentDescription = "Eliminar a ${paciente.nombre}" }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun Iniciales(nombre: String) {
    val texto = nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun DatoConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Confirmación de borrado: avisa que las citas del paciente también se eliminan (CASCADE en Room). */
@Composable
fun DialogoEliminarPaciente(paciente: PacienteConConteo, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val citas = when (paciente.totalCitas) {
        0 -> "Se borrarán sus datos."
        1 -> "Se borrarán sus datos y su cita."
        else -> "Se borrarán sus datos y sus ${paciente.totalCitas} citas."
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Eliminar a ${paciente.paciente.nombre}?") },
        text = { Text("$citas Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirmar, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
