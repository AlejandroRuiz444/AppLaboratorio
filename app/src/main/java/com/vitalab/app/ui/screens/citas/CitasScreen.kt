package com.vitalab.app.ui.screens.citas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.ui.components.DatoConIcono
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.EtiquetaEstadoCita
import com.vitalab.app.ui.components.citaDeEjemplo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.CitasUiState
import com.vitalab.app.viewmodel.CitasViewModel

/** Mis citas: lista de Room con filtro por estado y búsqueda por paciente o código. */
@Composable
fun CitasScreen(
    viewModel: CitasViewModel,
    onVerCita: (citaId: Long) -> Unit,
    onAgendar: () -> Unit,
    onVerEstadisticas: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // El texto del buscador vive aquí: `uiState` se recalcula con retraso (consulta a Room) y un campo atado
    // a él perdería letras al escribir rápido. El ViewModel recibe cada cambio; la lista se filtra sola.
    var texto by rememberSaveable { mutableStateOf(estado.busqueda) }
    LaunchedEffect(Unit) { if (texto != estado.busqueda) viewModel.onBusquedaChange(texto) }

    CitasContenido(
        estado = estado,
        busqueda = texto,
        onBusquedaChange = { texto = it; viewModel.onBusquedaChange(it) },
        onEstadoFiltroChange = viewModel::onEstadoFiltroChange,
        onLimpiarFiltros = { texto = ""; viewModel.onLimpiarFiltros() },
        onVerCita = onVerCita,
        onAgendar = onAgendar,
        onVerEstadisticas = onVerEstadisticas
    )
}

private val FILTROS_ESTADO: List<Pair<String, EstadoCita?>> = listOf(
    "Todas" to null,
    "Programadas" to EstadoCita.PROGRAMADA,
    "Completadas" to EstadoCita.COMPLETADA,
    "Canceladas" to EstadoCita.CANCELADA
)

@Composable
internal fun CitasContenido(
    estado: CitasUiState,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    onEstadoFiltroChange: (EstadoCita?) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onVerCita: (Long) -> Unit,
    onAgendar: () -> Unit,
    onVerEstadisticas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hayLista = !estado.cargando && estado.citas.isNotEmpty()
    Scaffold(
        modifier = modifier,
        // El Scaffold de la app ya reserva barras del sistema; no se vuelven a sumar aquí.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (hayLista || (!estado.cargando && estado.hayFiltros)) {
                ExtendedFloatingActionButton(
                    onClick = onAgendar,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Agendar cita") }
                )
            }
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EncabezadoPantalla("Mis citas", subtituloLista(estado), Modifier.weight(1f))
                TextButton(onClick = onVerEstadisticas, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Estadísticas")
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            BuscadorCitas(busqueda, onBusquedaChange)
            FiltrosEstado(estado.estadoFiltro, onEstadoFiltroChange)
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                estado.citas.isEmpty() && estado.hayFiltros -> EstadoVacio(
                    Icons.Filled.Search, "No encontramos citas con esos filtros.", "Limpiar filtros", onLimpiarFiltros
                )
                estado.citas.isEmpty() -> EstadoVacio(
                    Icons.Filled.DateRange,
                    "Aún no tienes citas. Agrega exámenes a tu orden y agenda tu toma de muestra.",
                    "Agendar cita",
                    onAgendar
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.citas, key = { it.cita.id }) { item -> TarjetaCita(item, onClick = { onVerCita(item.cita.id) }) }
                }
            }
        }
    }
}

private fun plural(n: Int, singular: String, plural: String) = if (n == 1) "1 $singular" else "$n $plural"

private fun subtituloLista(estado: CitasUiState): String = when {
    estado.cargando -> "Cargando citas…"
    estado.citas.isEmpty() -> if (estado.hayFiltros) "Sin resultados" else "Aún sin citas"
    else -> {
        val programadas = estado.citas.count { it.cita.estado == EstadoCita.PROGRAMADA }
        val total = plural(estado.citas.size, "cita", "citas")
        if (estado.estadoFiltro == null && programadas > 0) "$total · ${plural(programadas, "programada", "programadas")}" else total
    }
}

@Composable
private fun BuscadorCitas(busqueda: String, onBusquedaChange: (String) -> Unit) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusquedaChange,
        label = { Text("Buscar por paciente o código") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (busqueda.isNotEmpty()) {
            { IconButton(onClick = { onBusquedaChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda") } }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltrosEstado(seleccionado: EstadoCita?, onCambio: (EstadoCita?) -> Unit) {
    // En dos líneas si hace falta: las cuatro opciones se ven siempre, sin desplazar.
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FILTROS_ESTADO.forEach { (texto, estado) ->
            val activo = seleccionado == estado
            FilterChip(
                selected = activo,
                onClick = { onCambio(estado) },
                label = { Text(texto) },
                leadingIcon = if (activo) {
                    { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
        }
    }
}

@Composable
internal fun TarjetaCita(item: CitaCompleta, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val cita = item.cita
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.paciente.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(cita.codigo, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                }
                EtiquetaEstadoCita(cita.estado)
            }
            DatoConIcono(Icons.Filled.DateRange, "${formatoFecha(cita.fechaLocal)} · ${cita.hora}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                DatoConIcono(
                    if (cita.domicilio) Icons.Filled.Home else Icons.Filled.LocationOn,
                    if (cita.domicilio) "A domicilio" else "En sede",
                    Modifier.weight(1f)
                )
                Text(formatoPesos(cita.total), style = MaterialTheme.typography.titleMedium, color = c.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val citasDeEjemplo = listOf(
    citaDeEjemplo(id = 1, codigo = "VL-4821"),
    citaDeEjemplo(id = 2, estado = EstadoCita.COMPLETADA, domicilio = true, paciente = "Andrés Felipe Castañeda Villamizar", codigo = "VL-2210"),
    citaDeEjemplo(id = 3, estado = EstadoCita.CANCELADA, paciente = "Luz Mery Pardo", codigo = "VL-7733")
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CitasConDatosPreview() {
    VitaLabTheme {
        CitasContenido(CitasUiState(citasDeEjemplo, cargando = false), "", {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CitasFiltroSinResultadosPreview() {
    VitaLabTheme {
        CitasContenido(
            CitasUiState(emptyList(), estadoFiltro = EstadoCita.CANCELADA, busqueda = "zzz", cargando = false),
            "zzz", {}, {}, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CitasVaciasPreview() {
    VitaLabTheme { CitasContenido(CitasUiState(cargando = false), "", {}, {}, {}, {}, {}, {}) }
}
