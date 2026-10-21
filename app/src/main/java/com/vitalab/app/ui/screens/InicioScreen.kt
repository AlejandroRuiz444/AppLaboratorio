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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import com.vitalab.app.ui.theme.VitaLabTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalab.app.R
import com.vitalab.app.data.Cita
import com.vitalab.app.data.Examen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.ui.components.Etiqueta

private val destacados = catalogoExamenes.filter { it.id in listOf(1, 2, 3, 8, 6) }

@Composable
fun InicioScreen(cita: Cita?, examenesEnOrden: Int, onVerExamenes: () -> Unit, onVerOrden: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EncabezadoMarca() }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                if (cita != null) TarjetaProximaCita(cita) else TarjetaInvitacion(onVerExamenes)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccesoRapido(Icons.Filled.Search, "Exámenes", "${catalogoExamenes.size} disponibles", onVerExamenes, Modifier.weight(1f))
                AccesoRapido(
                    Icons.Filled.ShoppingCart, "Mi orden",
                    if (examenesEnOrden == 0) "Sin exámenes" else "$examenesEnOrden examen(es)",
                    onVerOrden, Modifier.weight(1f)
                )
            }
        }
        item {
            Text("Más solicitados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.size(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(destacados, key = { it.id }) { TarjetaDestacado(it, onVerExamenes) }
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
private fun TarjetaProximaCita(cita: Cita) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tu próxima cita", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Etiqueta(cita.codigo)
            }
            Text("${formatoFecha(cita.fecha)} · ${cita.hora}", style = MaterialTheme.typography.titleLarge)
            Text(if (cita.domicilio) "Toma a domicilio: ${cita.direccion}" else "En sede principal", style = MaterialTheme.typography.bodyMedium)
            Text("${cita.examenes.size} examen(es) · ${formatoPesos(cita.resumen.total)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                indicacionesPreparacion(cita.examenes).first(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InicioScreenPreview() {
    VitaLabTheme {
        InicioScreen(
            cita = null,
            examenesEnOrden = 0,
            onVerExamenes = {},
            onVerOrden = {}
        )
    }
}
