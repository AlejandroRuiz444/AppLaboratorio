@file:OptIn(ExperimentalLayoutApi::class)

package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.Categoria
import com.vitalab.app.ui.components.CampoValidado
import com.vitalab.app.ui.components.EncabezadoConVolver
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.ExamenFormUiState
import com.vitalab.app.viewmodel.ExamenFormViewModel

/**
 * Crear (`examen/nuevo`) o editar (`examen/{id}/editar`) un examen del catálogo.
 * Las validaciones y sus mensajes vienen del ViewModel; esta pantalla solo los muestra.
 */
@Composable
fun ExamenFormScreen(
    viewModel: ExamenFormViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // `guardado` pasa a true una sola vez: al guardar, se vuelve al catálogo.
    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }
    ExamenFormContenido(
        estado,
        onVolver = onVolver,
        onNombreChange = viewModel::onNombreChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onPrecioChange = viewModel::onPrecioChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onMuestraChange = viewModel::onMuestraChange,
        onAyunoChange = viewModel::onAyunoChange,
        onEntregaChange = viewModel::onEntregaChange,
        onIndicacionChange = viewModel::onIndicacionChange,
        onGuardar = viewModel::onGuardar
    )
}

@Composable
internal fun ExamenFormContenido(
    estado: ExamenFormUiState,
    onVolver: () -> Unit,
    onNombreChange: (String) -> Unit,
    onCategoriaChange: (Categoria) -> Unit,
    onPrecioChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onMuestraChange: (String) -> Unit,
    onAyunoChange: (String) -> Unit,
    onEntregaChange: (String) -> Unit,
    onIndicacionChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        EncabezadoConVolver(
            titulo = if (estado.esEdicion) "Editar examen" else "Nuevo examen",
            subtitulo = if (estado.esEdicion) "Actualiza los datos del catálogo" else "Agrégalo al catálogo del laboratorio",
            onVolver = onVolver
        )
        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            estado.noEncontrado -> EstadoVacio(
                Icons.Filled.Warning, "No encontramos este examen. Puede que ya no exista.", "Volver al catálogo", onVolver
            )

            else -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampoValidado(
                        estado.nombre, onNombreChange, "Nombre del examen", estado.errorNombre, mostrarError = true,
                        capitalizacion = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next
                    )
                    SelectorCategoria(estado.categoria, onCategoriaChange)
                    CampoValidado(
                        estado.precio, onPrecioChange, "Precio", estado.errorPrecio, mostrarError = true,
                        teclado = KeyboardType.Number, prefijo = "$", ayuda = "En pesos, sin puntos ni comas.", imeAction = ImeAction.Next
                    )
                    CampoValidado(
                        estado.descripcion, onDescripcionChange, "Descripción", estado.errorDescripcion, mostrarError = true,
                        minLineas = 2, maxLineas = 4, capitalizacion = KeyboardCapitalization.Sentences
                    )
                    CampoValidado(
                        estado.muestra, onMuestraChange, "Tipo de muestra", estado.errorMuestra, mostrarError = true,
                        capitalizacion = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        CampoValidado(
                            estado.ayunoHoras, onAyunoChange, "Horas de ayuno", estado.errorAyuno, mostrarError = true,
                            teclado = KeyboardType.Number, sufijo = "h", imeAction = ImeAction.Next, modifier = Modifier.weight(1f)
                        )
                        CampoValidado(
                            estado.horasEntrega, onEntregaChange, "Horas de entrega", estado.errorEntrega, mostrarError = true,
                            teclado = KeyboardType.Number, sufijo = "h", imeAction = ImeAction.Next, modifier = Modifier.weight(1f)
                        )
                    }
                    CampoValidado(
                        estado.indicacion, onIndicacionChange, "Indicación para el paciente (opcional)", error = null, mostrarError = false,
                        ayuda = "Se suma a la preparación que ve el paciente en su orden y su cita.",
                        minLineas = 2, maxLineas = 4, capitalizacion = KeyboardCapitalization.Sentences
                    )
                }
                BarraGuardar(estado, onVolver, onGuardar)
            }
        }
    }
}

@Composable
private fun SelectorCategoria(seleccionada: Categoria, onCambio: (Categoria) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Categoría", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Categoria.entries.forEach { c ->
                val activa = c == seleccionada
                FilterChip(
                    selected = activa,
                    onClick = { onCambio(c) },
                    label = { Text(c.etiqueta) },
                    leadingIcon = if (activa) ({ Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }) else null
                )
            }
        }
    }
}

@Composable
private fun BarraGuardar(estado: ExamenFormUiState, onVolver: () -> Unit, onGuardar: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado.intentado && estado.hayErrores) {
                Text(
                    "Revisa los campos marcados en rojo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVolver, enabled = !estado.guardando, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = onGuardar, enabled = !estado.guardando, modifier = Modifier.weight(1.4f)) {
                    if (estado.guardando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (estado.esEdicion) "Guardar cambios" else "Guardar examen")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExamenFormNuevoPreview() {
    VitaLabTheme {
        ExamenFormContenido(
            ExamenFormUiState(
                nombre = "Ab", precio = "", intentado = true,
                errorNombre = "El nombre debe tener al menos 3 letras", errorPrecio = "Precio en pesos, mayor que 0",
                errorDescripcion = "Escribe la descripción"
            ),
            onVolver = {}, onNombreChange = {}, onCategoriaChange = {}, onPrecioChange = {}, onDescripcionChange = {},
            onMuestraChange = {}, onAyunoChange = {}, onEntregaChange = {}, onIndicacionChange = {}, onGuardar = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamenFormEdicionPreview() {
    VitaLabTheme {
        ExamenFormContenido(
            ExamenFormUiState(
                esEdicion = true, nombre = "Perfil lipídico", categoria = Categoria.QUIMICA, precio = "48000",
                descripcion = "Colesterol total, HDL, LDL y triglicéridos.", muestra = "Sangre", ayunoHoras = "12", horasEntrega = "6",
                indicacion = "Evita comidas grasas y alcohol 24 horas antes."
            ),
            onVolver = {}, onNombreChange = {}, onCategoriaChange = {}, onPrecioChange = {}, onDescripcionChange = {},
            onMuestraChange = {}, onAyunoChange = {}, onEntregaChange = {}, onIndicacionChange = {}, onGuardar = {}
        )
    }
}
