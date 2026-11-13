package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.Categoria
import com.vitalab.app.data.Examen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.ExamenCard
import com.vitalab.app.ui.components.conteo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.CatalogoUiState
import com.vitalab.app.viewmodel.CatalogoViewModel
import kotlinx.coroutines.launch

/**
 * Catálogo de exámenes y su administración. Buscar y filtrar, agregar o quitar de la orden, y (con el
 * interruptor "Administrar") editar, desactivar o reactivar. Crear y editar abren `ExamenFormScreen`.
 */
@Composable
fun ExamenesScreen(
    viewModel: CatalogoViewModel,
    onVerOrden: () -> Unit,
    onNuevoExamen: () -> Unit,
    onEditarExamen: (examenId: Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ExamenesContenido(
        estado,
        onBusquedaChange = viewModel::onBusquedaChange,
        onCategoriaChange = viewModel::onCategoriaChange,
        onLimpiarFiltros = viewModel::onLimpiarFiltros,
        onModoAdminChange = viewModel::onModoAdminChange,
        onAlternarOrden = viewModel::onAlternarOrden,
        onCambiarActivo = viewModel::onCambiarActivo,
        onVerOrden = onVerOrden,
        onNuevoExamen = onNuevoExamen,
        onEditarExamen = onEditarExamen
    )
}

@Composable
internal fun ExamenesContenido(
    estado: CatalogoUiState,
    onBusquedaChange: (String) -> Unit,
    onCategoriaChange: (Categoria?) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onModoAdminChange: (Boolean) -> Unit,
    onAlternarOrden: (Int) -> Unit,
    onCambiarActivo: (Examen) -> Unit,
    onVerOrden: () -> Unit,
    onNuevoExamen: () -> Unit,
    onEditarExamen: (Int) -> Unit
) {
    val busqueda = estado.busqueda
    val categoria = estado.categoria
    val foco = LocalFocusManager.current

    // Examen pendiente de confirmar (solo su id, para sobrevivir a rotaciones). Si el examen deja de estar visible, el diálogo se cierra solo.
    var pendienteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val pendiente = estado.examenes.firstOrNull { it.id == pendienteId }
    val mensajes = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Catálogo de exámenes", conteo(estado.totalActivos, "examen disponible", "exámenes disponibles"))
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusquedaChange,
            label = { Text("Buscar examen") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { onBusquedaChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda") }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = categoria == null, onClick = { onCategoriaChange(null) }, label = { Text("Todos") }) }
            items(Categoria.entries) { c ->
                FilterChip(
                    selected = categoria == c,
                    onClick = { onCategoriaChange(if (categoria == c) null else c) },
                    label = { Text(c.etiqueta) }
                )
            }
        }

        Box(Modifier.weight(1f)) {
            if (estado.cargando) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "administracion") {
                        BarraAdministracion(estado.modoAdmin, onModoAdminChange, onNuevoExamen)
                    }
                    if (estado.examenes.isEmpty()) {
                        item(key = "vacio") {
                            if (estado.hayFiltros) {
                                EstadoVacio(Icons.Filled.Search, "No encontramos exámenes con esos filtros.", "Limpiar filtros", onLimpiarFiltros)
                            } else {
                                EstadoVacio(Icons.Filled.Search, "Todavía no hay exámenes activos en el catálogo.", "Crear examen", onNuevoExamen)
                            }
                        }
                    }
                    items(estado.examenes, key = { it.id }) { examen ->
                        ExamenCard(
                            examen,
                            enOrden = examen.id in estado.idsEnOrden,
                            onAgregarQuitar = { onAlternarOrden(examen.id) },
                            modoAdmin = estado.modoAdmin,
                            onEditar = { onEditarExamen(examen.id) },
                            onCambiarActivo = { pendienteId = examen.id }
                        )
                    }
                }
            }
        }

        SnackbarHost(mensajes)

        if (estado.cantidadEnOrden > 0) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(conteo(estado.cantidadEnOrden, "examen en tu orden", "exámenes en tu orden"), style = MaterialTheme.typography.titleSmall)
                        Text(formatoPesos(estado.totalEnOrden), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onVerOrden) { Text("Ver orden") }
                }
            }
        }
    }

    if (pendiente != null) {
        DialogoCambioActivo(
            examen = pendiente,
            onConfirmar = {
                onCambiarActivo(pendiente)
                pendienteId = null
                val aviso = if (pendiente.activo) "«${pendiente.nombre}» desactivado" else "«${pendiente.nombre}» reactivado"
                alcance.launch { mensajes.showSnackbar(aviso) }
            },
            onCancelar = { pendienteId = null }
        )
    }
}

/** Interruptor del modo administración y acceso a "Nuevo examen". Es la primera fila de la lista. */
@Composable
private fun BarraAdministracion(modoAdmin: Boolean, onModoAdminChange: (Boolean) -> Unit, onNuevoExamen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .toggleable(value = modoAdmin, role = Role.Switch, onValueChange = onModoAdminChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(checked = modoAdmin, onCheckedChange = null)
                Text("Administrar", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(onClick = onNuevoExamen) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo examen")
            }
        }
        if (modoAdmin) {
            Text(
                "Edita un examen o desactívalo: los desactivados salen del catálogo y de tu orden, y puedes reactivarlos cuando quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DialogoCambioActivo(examen: Examen, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val desactivar = examen.activo
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (desactivar) "¿Desactivar este examen?" else "¿Reactivar este examen?") },
        text = {
            Text(
                if (desactivar) {
                    "«${examen.nombre}» dejará de aparecer en el catálogo y se quitará de tu orden. Las citas ya agendadas no cambian."
                } else {
                    "«${examen.nombre}» volverá a aparecer en el catálogo y podrás agregarlo a tu orden."
                }
            )
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(if (desactivar) "Desactivar" else "Reactivar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExamenesAdminPreview() {
    VitaLabTheme {
        ExamenesContenido(
            CatalogoUiState(
                examenes = catalogoExamenes.take(4).mapIndexed { i, e -> if (i == 3) e.copy(activo = false) else e },
                totalActivos = 10, modoAdmin = true, idsEnOrden = setOf(1, 3), cantidadEnOrden = 2, totalEnOrden = 73_000, cargando = false
            ),
            onBusquedaChange = {}, onCategoriaChange = {}, onLimpiarFiltros = {}, onModoAdminChange = {},
            onAlternarOrden = {}, onCambiarActivo = {}, onVerOrden = {}, onNuevoExamen = {}, onEditarExamen = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamenesVacioPreview() {
    VitaLabTheme {
        ExamenesContenido(
            CatalogoUiState(busqueda = "xyz", totalActivos = 11, cargando = false),
            onBusquedaChange = {}, onCategoriaChange = {}, onLimpiarFiltros = {}, onModoAdminChange = {},
            onAlternarOrden = {}, onCambiarActivo = {}, onVerOrden = {}, onNuevoExamen = {}, onEditarExamen = {}
        )
    }
}
