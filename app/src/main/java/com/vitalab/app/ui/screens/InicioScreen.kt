@file:OptIn(ExperimentalMaterial3Api::class)

package com.vitalab.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.R
import com.vitalab.app.data.Examen
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.local.Cita
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.CitaExamen
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.ui.components.Etiqueta
import com.vitalab.app.ui.components.IconoEstadisticas
import com.vitalab.app.ui.components.conteo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.InicioUiState
import com.vitalab.app.viewmodel.InicioViewModel
import java.time.LocalDate

/** Pantalla de inicio: marca, próxima cita (de Room), accesos rápidos, más solicitados y datos de la sede. */
@Composable
fun InicioScreen(
    viewModel: InicioViewModel,
    onVerExamenes: () -> Unit,
    onVerOrden: () -> Unit,
    onVerCita: (citaId: Long) -> Unit,
    onVerEstadisticas: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    InicioContenido(estado, onVerExamenes, onVerOrden, onVerCita, onVerEstadisticas)
}

@Composable
internal fun InicioContenido(
    estado: InicioUiState,
    onVerExamenes: () -> Unit,
    onVerOrden: () -> Unit,
    onVerCita: (Long) -> Unit,
    onVerEstadisticas: () -> Unit
) {
    val cita = estado.proximaCita
    val examenesEnOrden = estado.examenesEnOrden
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EncabezadoMarca() }
        // Mientras Room responde no se dibuja ninguna de las dos tarjetas, para que la invitación no parpadee antes de la cita.
        if (!estado.cargando) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    if (cita != null) TarjetaProximaCita(cita, onVerCita) else TarjetaInvitacion(onVerExamenes)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccesoRapido(Icons.Filled.Search, "Exámenes", "${estado.totalExamenes} disponibles", onVerExamenes, Modifier.weight(1f))
                    AccesoRapido(
                        Icons.Filled.ShoppingCart, "Mi orden",
                        if (examenesEnOrden == 0) "Sin exámenes" else conteo(examenesEnOrden, "examen", "exámenes"),
                        onVerOrden, Modifier.weight(1f)
                    )
                }
                AccesoEstadisticas(onVerEstadisticas)
            }
        }
        if (estado.destacados.isNotEmpty()) {
            item {
                Text("Destacados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.size(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(estado.destacados, key = { it.id }) { TarjetaDestacado(it, onVerExamenes) }
                }
            }
        }
        item { TarjetaSede(Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun EncabezadoMarca() {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(c.primary, c.secondary)))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = "Logo de VitaLab", modifier = Modifier.size(76.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("VitaLab", style = MaterialTheme.typography.headlineMedium, color = c.onPrimary)
                Text("Laboratorio Clínico", style = MaterialTheme.typography.titleMedium, color = c.onPrimary)
                Text("Resultados confiables, atención humana", style = MaterialTheme.typography.bodySmall, color = c.onPrimary)
            }
        }
    }
}

@Composable
private fun TarjetaProximaCita(completa: CitaCompleta, onVerCita: (Long) -> Unit) {
    val cita = completa.cita
    val c = MaterialTheme.colorScheme
    Card(
        onClick = { onVerCita(cita.id) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = c.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tu próxima cita", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Etiqueta(cita.codigo)
            }
            Text("${formatoFecha(cita.fechaLocal)} · ${cita.hora}", style = MaterialTheme.typography.titleLarge)
            Text(if (cita.domicilio) "Toma a domicilio: ${cita.direccion}" else "En sede principal", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${conteo(completa.examenes.size, "examen", "exámenes")} · ${formatoPesos(cita.total)}",
                style = MaterialTheme.typography.bodyMedium
            )
            val aviso = indicacionesPreparacion(completa.examenes).firstOrNull()
            if (aviso != null) {
                Surface(color = c.tertiaryContainer, contentColor = c.onTertiaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(aviso, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(8.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("Ver detalle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TarjetaInvitacion(onVerExamenes: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Agenda tu toma de muestra", style = MaterialTheme.typography.titleMedium)
            Text(
                "Elige tus exámenes, mira el valor con descuentos y la preparación que necesitas, y agenda en sede o a domicilio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onVerExamenes) { Text("Empezar") }
        }
    }
}

@Composable
private fun AccesoRapido(icono: ImageVector, titulo: String, subtitulo: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(8.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccesoEstadisticas(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(IconoEstadisticas, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text("Estadísticas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Citas, ingresos y exámenes más pedidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TarjetaDestacado(examen: Examen, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(160.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiqueta(examen.categoria.etiqueta)
            Text(examen.nombre, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2)
            Text(formatoPesos(examen.precio), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TarjetaSede(modifier: Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sede principal", style = MaterialTheme.typography.titleMedium)
            FilaIcono(Icons.Filled.LocationOn, "Cra. 15 # 93-60, Bogotá")
            FilaIcono(Icons.Filled.DateRange, "Lunes a sábado · 6:00 a.m. a 4:00 p.m.")
            FilaIcono(Icons.Filled.Phone, "(601) 555 0100")
        }
    }
}

@Composable
private fun FilaIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Datos de muestra solo para las vistas previas. */
internal fun citaDeEjemplo(): CitaCompleta {
    val examenes = catalogoExamenes.take(3)
    val resumen = calcularResumen(examenes, adultoMayor = false, domicilio = true)
    val cita = Cita(
        id = 1, codigo = "VL-4821", pacienteId = 1, fecha = LocalDate.now().plusDays(2).toEpochDay(), hora = "7:00 a.m.",
        domicilio = true, direccion = "Calle 100 # 15-20", adultoMayor = false, subtotal = resumen.subtotal,
        descuentoPaquete = resumen.descuentoPaquete, descuentoAdultoMayor = resumen.descuentoAdultoMayor,
        recargoDomicilio = resumen.recargoDomicilio
    )
    return CitaCompleta(
        cita, Paciente(id = 1, nombre = "Marta Ruiz", documento = "52123456", telefono = "3001112233", correo = "marta@correo.com"),
        examenes, examenes.map { CitaExamen(1, it.id, it.precio) }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioConCitaPreview() {
    VitaLabTheme {
        InicioContenido(
            InicioUiState(
                proximaCita = citaDeEjemplo(), examenesEnOrden = 2, totalExamenes = 11,
                destacados = catalogoExamenes.take(4), cargando = false
            ),
            onVerExamenes = {}, onVerOrden = {}, onVerCita = {}, onVerEstadisticas = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioSinCitaPreview() {
    VitaLabTheme {
        InicioContenido(
            InicioUiState(totalExamenes = 11, destacados = catalogoExamenes.take(3), cargando = false),
            onVerExamenes = {}, onVerOrden = {}, onVerCita = {}, onVerEstadisticas = {}
        )
    }
}
