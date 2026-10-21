@file:OptIn(ExperimentalMaterial3Api::class)

package com.vitalab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.Cita
import com.vitalab.app.data.Examen
import com.vitalab.app.data.RECARGO_DOMICILIO
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.diaCorto
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.franjasDisponibles
import com.vitalab.app.data.generarCodigoCita
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.mesCorto
import com.vitalab.app.data.proximasFechas
import com.vitalab.app.data.validarCorreo
import com.vitalab.app.data.validarDireccion
import com.vitalab.app.data.validarDocumento
import com.vitalab.app.data.validarNombre
import com.vitalab.app.data.validarTelefono
import com.vitalab.app.ui.components.CampoValidado
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.FilaResumen
import com.vitalab.app.ui.components.TarjetaIndicaciones
import com.vitalab.app.ui.components.TarjetaResumenCosto
import java.time.LocalDate

@Composable
fun CitaScreen(
    orden: List<Examen>,
    adultoMayor: Boolean,
    onConfirmar: (Cita) -> Unit,
    onVolver: () -> Unit,
    onIrInicio: () -> Unit
) {
    var confirmada by remember { mutableStateOf<Cita?>(null) }
    val cita = confirmada
    if (cita != null) {
        ConfirmacionCita(cita, onIrInicio)
        return
    }
    if (orden.isEmpty()) {
        EstadoVacio(Icons.Filled.Info, "Primero agrega exámenes a tu orden.", "Volver", onVolver)
        return
    }

    var nombre by rememberSaveable { mutableStateOf("") }
    var documento by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var diaElegido by rememberSaveable { mutableStateOf<Long?>(null) }
    var hora by rememberSaveable { mutableStateOf<String?>(null) }
    var domicilio by rememberSaveable { mutableStateOf(false) }
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }

    val fechas = remember { proximasFechas(LocalDate.now()) }
    val ayuno = orden.maxOf { it.ayunoHoras }
    val franjas = franjasDisponibles(ayuno)
    val resumen = calcularResumen(orden, adultoMayor, domicilio)

    val errorNombre = validarNombre(nombre)
    val errorDocumento = validarDocumento(documento)
    val errorTelefono = validarTelefono(telefono)
    val errorCorreo = validarCorreo(correo)
    val errorDireccion = if (domicilio) validarDireccion(direccion) else null
    val formularioValido = listOf(errorNombre, errorDocumento, errorTelefono, errorCorreo, errorDireccion).all { it == null } &&
        diaElegido != null && hora?.let { it in franjas } == true

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a mi orden") }
            EncabezadoPantalla("Agendar toma de muestra", "${orden.size} examen(es) · ${formatoPesos(resumen.total)}")
        }

        Seccion("1. Datos del paciente") {
            CampoValidado(nombre, { nombre = it }, "Nombre completo", errorNombre, intentoEnviar || nombre.isNotEmpty(), Icons.Filled.Person)
            CampoValidado(documento, { documento = it }, "Número de documento", errorDocumento, intentoEnviar || documento.isNotEmpty(), Icons.Filled.Info, KeyboardType.Number)
            CampoValidado(telefono, { telefono = it }, "Celular", errorTelefono, intentoEnviar || telefono.isNotEmpty(), Icons.Filled.Phone, KeyboardType.Phone)
            CampoValidado(correo, { correo = it }, "Correo electrónico", errorCorreo, intentoEnviar || correo.isNotEmpty(), Icons.Filled.Email, KeyboardType.Email)
        }

        Seccion("2. Fecha") {}
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fechas) { fecha ->
                val elegida = diaElegido == fecha.toEpochDay()
                val c = MaterialTheme.colorScheme
                Card(
                    onClick = { diaElegido = fecha.toEpochDay() },
                    colors = CardDefaults.cardColors(
                        containerColor = if (elegida) c.primary else c.surfaceContainerHighest,
                        contentColor = if (elegida) c.onPrimary else c.onSurface
                    )
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(diaCorto(fecha), style = MaterialTheme.typography.labelMedium)
                        Text("${fecha.dayOfMonth}", style = MaterialTheme.typography.titleLarge)
                        Text(mesCorto(fecha), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (intentoEnviar && diaElegido == null) TextoError("Selecciona una fecha")

        Seccion("3. Hora") {
            if (ayuno > 0) {
                Text(
                    "Tu orden requiere ayuno de $ayuno horas: solo mostramos franjas hasta las 9:00 a.m.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(franjas) { franja ->
                FilterChip(selected = hora == franja, onClick = { hora = franja }, label = { Text(franja) })
            }
        }
        if (intentoEnviar && hora == null) TextoError("Selecciona una hora")

        Seccion("4. Modalidad") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !domicilio, onClick = { domicilio = false }, label = { Text("En sede") })
                FilterChip(
                    selected = domicilio,
                    onClick = { domicilio = true },
                    label = { Text("A domicilio (+${formatoPesos(RECARGO_DOMICILIO)})") }
                )
            }
            if (domicilio) {
                CampoValidado(direccion, { direccion = it }, "Dirección de la toma", errorDireccion, intentoEnviar || direccion.isNotEmpty(), Icons.Filled.Home)
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaResumenCosto(resumen)
            TarjetaIndicaciones(indicacionesPreparacion(orden))
            Button(
                onClick = {
                    intentoEnviar = true
                    if (formularioValido) {
                        val nueva = Cita(
                            codigo = generarCodigoCita(),
                            paciente = nombre.trim(),
                            documento = documento,
                            telefono = telefono,
                            correo = correo.trim(),
                            fecha = LocalDate.ofEpochDay(diaElegido!!),
                            hora = hora!!,
                            domicilio = domicilio,
                            direccion = direccion.trim(),
                            examenes = orden.toList(),
                            resumen = resumen
                        )
                        confirmada = nueva
                        onConfirmar(nueva)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirmar cita") }
            if (intentoEnviar && !formularioValido) TextoError("Revisa los campos marcados en rojo.", Modifier)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        contenido()
    }
}

@Composable
private fun TextoError(texto: String, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
private fun ConfirmacionCita(cita: Cita, onIrInicio: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Text("¡Cita confirmada!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Te enviaremos el recordatorio a ${cita.correo} y al ${cita.telefono}.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                FilaResumen("Código", cita.codigo, destacado = true)
                FilaResumen("Paciente", cita.paciente)
                FilaResumen("Documento", cita.documento)
                FilaResumen("Fecha", formatoFecha(cita.fecha))
                FilaResumen("Hora", cita.hora)
                FilaResumen("Modalidad", if (cita.domicilio) "A domicilio" else "En sede")
                if (cita.domicilio) FilaResumen("Dirección", cita.direccion)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Exámenes", style = MaterialTheme.typography.titleMedium)
                cita.examenes.forEach { FilaResumen(it.nombre, formatoPesos(it.precio)) }
            }
        }
        TarjetaResumenCosto(cita.resumen)
        TarjetaIndicaciones(indicacionesPreparacion(cita.examenes))
        Button(onClick = onIrInicio, modifier = Modifier.fillMaxWidth()) { Text("Ir al inicio") }
    }
}
