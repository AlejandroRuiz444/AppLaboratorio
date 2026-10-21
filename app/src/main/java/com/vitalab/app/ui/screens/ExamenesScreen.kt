@file:OptIn(ExperimentalMaterial3Api::class)

package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.data.Categoria
import com.vitalab.app.data.Examen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.ExamenCard

@Composable
fun ExamenesScreen(orden: List<Examen>, onAgregarQuitar: (Examen) -> Unit, onVerOrden: () -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }

    val filtrados = catalogoExamenes.filter {
        (categoria == null || it.categoria == categoria) && it.nombre.contains(busqueda.trim(), ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Catálogo de exámenes", "${catalogoExamenes.size} exámenes disponibles")
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar examen") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { busqueda = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda") }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = categoria == null, onClick = { categoria = null }, label = { Text("Todos") }) }
            items(Categoria.entries) { c ->
                FilterChip(
                    selected = categoria == c,
                    onClick = { categoria = if (categoria == c) null else c },
                    label = { Text(c.etiqueta) }
                )
            }
        }

        Box(Modifier.weight(1f)) {
            if (filtrados.isEmpty()) {
                EstadoVacio(Icons.Filled.Search, "No encontramos exámenes con esos filtros.", "Limpiar filtros", {
                    busqueda = ""
                    categoria = null
                })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtrados, key = { it.id }) { examen ->
                        ExamenCard(examen, enOrden = examen in orden, onAgregarQuitar = { onAgregarQuitar(examen) })
                    }
                }
            }
        }

        if (orden.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${orden.size} en tu orden · ${formatoPesos(orden.sumOf { it.precio })}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onVerOrden) { Text("Ver orden") }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExamenesScreenPreview() {
    VitaLabTheme {
        ExamenesScreen(
            orden = emptyList(),
            onAgregarQuitar = {},
            onVerOrden = {}
        )
    }
}
