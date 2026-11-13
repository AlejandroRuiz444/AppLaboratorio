package com.vitalab.app.ui.screens.citas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.coincideBusqueda
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.franjasDisponibles
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.proximasFechas
import com.vitalab.app.ui.components.CampoValidado
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.FilaExamenCita
import com.vitalab.app.ui.components.FilaDato
import com.vitalab.app.ui.components.OpcionSeleccionable
import com.vitalab.app.ui.components.PasoFormulario
import com.vitalab.app.ui.components.SelectorFecha
import com.vitalab.app.ui.components.SelectorHora
import com.vitalab.app.ui.components.SelectorModalidad
import com.vitalab.app.ui.components.TarjetaIndicaciones
import com.vitalab.app.ui.components.TarjetaResumenCosto
import com.vitalab.app.ui.components.TextoError
import com.vitalab.app.ui.components.citaDeEjemplo
import com.vitalab.app.ui.components.conteo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.AgendarUiState
import com.vitalab.app.viewmodel.AgendarViewModel
import com.vitalab.app.viewmodel.ModoPaciente
import java.time.LocalDate

/**
 * Agendar la cita de la orden en curso en cuatro pasos: paciente (registrado o nuevo), fecha, hora y modalidad.
 * `onVerExamenes` se usa en el estado vacío (orden sin exámenes); por defecto vuelve atrás.
 */
@Composable
fun AgendarCitaScreen(
    viewModel: AgendarViewModel,
    onVolver: () -> Unit,
    onIrInicio: () -> Unit,
    onVerCita: (citaId: Long) -> Unit,
    onVerExamenes: () -> Unit = onVolver
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    AgendarCitaContenido(
        estado,
        AgendarAcciones(
            onVolver = onVolver, onIrInicio = onIrInicio, onVerCita = onVerCita, onVerExamenes = onVerExamenes,
            onModoPacienteChange = viewModel::onModoPacienteChange, onPacienteSeleccionado = viewModel::onPacienteSeleccionado,
            onNombreChange = viewModel::onNombreChange, onDocumentoChange = viewModel::onDocumentoChange,
            onTelefonoChange = viewModel::onTelefonoChange, onCorreoChange = viewModel::onCorreoChange,
            onFechaChange = viewModel::onFechaChange, onHoraChange = viewModel::onHoraChange,
            onDomicilioChange = viewModel::onDomicilioChange, onDireccionChange = viewModel::onDireccionChange,
            onConfirmar = viewModel::onConfirmar
        )
    )
}

/** Los eventos de la pantalla agrupados: así el composable sin estado se previsualiza con `AgendarAcciones()`. */
internal class AgendarAcciones(
    val onVolver: () -> Unit = {},
    val onIrInicio: () -> Unit = {},
    val onVerCita: (Long) -> Unit = {},
    val onVerExamenes: () -> Unit = {},
    val onModoPacienteChange: (ModoPaciente) -> Unit = {},
    val onPacienteSeleccionado: (Long) -> Unit = {},
    val onNombreChange: (String) -> Unit = {},
    val onDocumentoChange: (String) -> Unit = {},
    val onTelefonoChange: (String) -> Unit = {},
    val onCorreoChange: (String) -> Unit = {},
    val onFechaChange: (Long) -> Unit = {},
    val onHoraChange: (String) -> Unit = {},
    val onDomicilioChange: (Boolean) -> Unit = {},
    val onDireccionChange: (String) -> Unit = {},
    val onConfirmar: () -> Unit = {}
)

@Composable
internal fun AgendarCitaContenido(estado: AgendarUiState, acciones: AgendarAcciones, modifier: Modifier = Modifier) {
    val confirmada = estado.confirmada
    // La confirmación es un estado de esta ruta, no otra ruta: sin esto Atrás caería en Mi orden, ya vacía.
    BackHandler(enabled = confirmada != null, onBack = acciones.onIrInicio)
    Column(modifier.fillMaxSize().imePadding()) {
        when {
            confirmada != null -> ConfirmacionCita(confirmada, acciones)
            estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            estado.ordenVacia -> {
                CabeceraAgendar("Sin exámenes en tu orden", acciones.onVolver)
                EstadoVacio(
                    Icons.Filled.ShoppingCart,
                    "Primero agrega exámenes a tu orden para poder agendar la toma de muestra.",
                    "Ver exámenes",
                    acciones.onVerExamenes
                )
            }
            else -> Formulario(estado, acciones)
        }
    }
}

@Composable
private fun CabeceraAgendar(subtitulo: String, onVolver: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        EncabezadoPantalla("Agendar toma de muestra", subtitulo)
    }
}

@Composable
private fun ColumnScope.Formulario(estado: AgendarUiState, acciones: AgendarAcciones) {
    CabeceraAgendar("${conteo(estado.examenes.size, "examen", "exámenes")} · ${formatoPesos(estado.resumen.total)}", acciones.onVolver)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        PasoFormulario(1, "Paciente", completo = pacienteListo(estado)) { SeccionPaciente(estado, acciones) }
        PasoFormulario(2, "Fecha", completo = estado.fecha != null) {
            SelectorFecha(estado.fechasDisponibles, estado.fecha, acciones.onFechaChange, estado.errorFecha)
        }
        PasoFormulario(3, "Hora", completo = estado.hora != null && estado.hora in estado.franjas) {
            SelectorHora(estado.franjas, estado.hora, acciones.onHoraChange, estado.ayunoHoras, estado.errorHora)
        }
        PasoFormulario(4, "Modalidad", completo = !estado.domicilio || (estado.direccion.isNotBlank() && estado.errorDireccion == null)) {
            SelectorModalidad(estado.domicilio, estado.direccion, estado.errorDireccion, acciones.onDomicilioChange, acciones.onDireccionChange)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaResumenCosto(estado.resumen)
            TarjetaIndicaciones(estado.indicaciones)
            // El error del repositorio (p. ej. documento repetido) va junto al botón y se anuncia a TalkBack.
            estado.errorGeneral?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Button(onClick = acciones.onConfirmar, enabled = !estado.enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.enviando) "Guardando…" else "Confirmar cita")
            }
            if (estado.intentado && hayErrores(estado)) {
                Text("Revisa los campos marcados en rojo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Marca de "paso listo". Solo mira lo que el ViewModel ya validó: sin texto vacío y sin error visible. */
private fun pacienteListo(e: AgendarUiState): Boolean =
    if (e.modoPaciente == ModoPaciente.EXISTENTE) {
        e.pacientes.any { it.id == e.pacienteId }
    } else {
        listOf(e.nombre, e.documento, e.telefono, e.correo).all { it.isNotBlank() } &&
            listOf(e.errorNombre, e.errorDocumento, e.errorTelefono, e.errorCorreo).all { it == null }
    }

private fun hayErrores(e: AgendarUiState): Boolean = listOf(
    e.errorPaciente, e.errorNombre, e.errorDocumento, e.errorTelefono, e.errorCorreo, e.errorFecha, e.errorHora, e.errorDireccion
).any { it != null }

@Composable
private fun SeccionPaciente(estado: AgendarUiState, acciones: AgendarAcciones) {
    if (estado.pacientes.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpcionSeleccionable(estado.modoPaciente == ModoPaciente.EXISTENTE, { acciones.onModoPacienteChange(ModoPaciente.EXISTENTE) }, Modifier.weight(1f)) {
                Text("Ya registrado", style = MaterialTheme.typography.titleSmall)
            }
            OpcionSeleccionable(estado.modoPaciente == ModoPaciente.NUEVO, { acciones.onModoPacienteChange(ModoPaciente.NUEVO) }, Modifier.weight(1f)) {
                Text("Paciente nuevo", style = MaterialTheme.typography.titleSmall)
            }
        }
    } else {
        Text(
            "Aún no hay pacientes registrados. Crea el primero aquí: quedará guardado para tus próximas citas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (estado.modoPaciente == ModoPaciente.EXISTENTE) ElegirPaciente(estado, acciones) else DatosPacienteNuevo(estado, acciones)
}

private const val MAX_PACIENTES_VISIBLES = 5

@Composable
private fun ElegirPaciente(estado: AgendarUiState, acciones: AgendarAcciones) {
    var consulta by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = consulta,
        onValueChange = { consulta = it },
        label = { Text("Nombre o documento") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (consulta.isNotEmpty()) {
            { IconButton(onClick = { consulta = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda") } }
        } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    val coinciden = estado.pacientes.filter { consulta.isBlank() || coincideBusqueda("${it.nombre} ${it.documento}", consulta) }
    // El paciente ya elegido siempre se ve arriba, aunque la búsqueda actual no lo incluya.
    val elegido = estado.pacientes.firstOrNull { it.id == estado.pacienteId }
    val visibles = (listOfNotNull(elegido) + coinciden.filter { it.id != elegido?.id }).take(MAX_PACIENTES_VISIBLES)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibles.forEach { p -> FilaPaciente(p, p.id == estado.pacienteId) { acciones.onPacienteSeleccionado(p.id) } }
        when {
            coinciden.isEmpty() -> {
                Text("Ningún paciente coincide con la búsqueda.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { acciones.onModoPacienteChange(ModoPaciente.NUEVO) }) { Text("Crear paciente nuevo") }
            }
            coinciden.size > visibles.size -> Text(
                "Mostrando ${visibles.size} de ${coinciden.size}. Escribe para afinar la búsqueda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextoError(estado.errorPaciente)
    }
}

@Composable
private fun FilaPaciente(paciente: Paciente, elegido: Boolean, onClick: () -> Unit) {
    OpcionSeleccionable(elegido, onClick, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(paciente.nombre, style = MaterialTheme.typography.titleSmall)
                Text("Doc. ${paciente.documento} · ${paciente.telefono}", style = MaterialTheme.typography.bodySmall)
            }
            if (elegido) Icon(Icons.Filled.Check, contentDescription = "Paciente elegido", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DatosPacienteNuevo(estado: AgendarUiState, acciones: AgendarAcciones) {
    // Los errores ya vienen filtrados por el ViewModel (solo si el campo se tocó o ya se intentó enviar).
    CampoValidado(estado.nombre, acciones.onNombreChange, "Nombre completo", estado.errorNombre, true, Icons.Filled.Person)
    CampoValidado(estado.documento, acciones.onDocumentoChange, "Número de documento", estado.errorDocumento, true, Icons.Filled.AccountBox, KeyboardType.Number)
    CampoValidado(estado.telefono, acciones.onTelefonoChange, "Celular", estado.errorTelefono, true, Icons.Filled.Phone, KeyboardType.Phone)
    CampoValidado(estado.correo, acciones.onCorreoChange, "Correo electrónico", estado.errorCorreo, true, Icons.Filled.Email, KeyboardType.Email)
}

@Composable
private fun ConfirmacionCita(completa: CitaCompleta, acciones: AgendarAcciones) {
    val cita = completa.cita
    val paciente = completa.paciente
    val lineas = remember(completa) { completa.detalle }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Text("¡Cita confirmada!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Cita guardada para ${paciente.nombre}. Puedes verla cuando quieras en la pestaña Citas.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                FilaDato("Código", cita.codigo, destacado = true)
                FilaDato("Paciente", paciente.nombre)
                FilaDato("Documento", paciente.documento)
                FilaDato("Fecha", formatoFecha(cita.fechaLocal))
                FilaDato("Hora", cita.hora)
                FilaDato("Modalidad", if (cita.domicilio) "A domicilio" else "En sede")
                if (cita.domicilio) FilaDato("Dirección", cita.direccion)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Exámenes", style = MaterialTheme.typography.titleMedium)
                lineas.forEach { FilaExamenCita(it) }
            }
        }
        TarjetaResumenCosto(completa.resumen)
        TarjetaIndicaciones(indicacionesPreparacion(completa.examenes))
        Button(onClick = { acciones.onVerCita(cita.id) }, modifier = Modifier.fillMaxWidth()) { Text("Ver cita") }
        OutlinedButton(onClick = acciones.onIrInicio, modifier = Modifier.fillMaxWidth()) { Text("Ir al inicio") }
    }
}

private fun estadoDeEjemplo(
    modo: ModoPaciente = ModoPaciente.EXISTENTE,
    pacientes: List<Paciente> = pacientesDeEjemplo,
    intentado: Boolean = false
): AgendarUiState {
    val examenes = catalogoExamenes.take(3)
    val fechas = proximasFechas(LocalDate.of(2026, 11, 12)).map { it.toEpochDay() }
    val ayuno = examenes.maxOf { it.ayunoHoras }
    return AgendarUiState(
        cargando = false,
        examenes = examenes,
        resumen = calcularResumen(examenes, adultoMayor = false, domicilio = false),
        indicaciones = indicacionesPreparacion(examenes),
        ayunoHoras = ayuno,
        modoPaciente = modo,
        pacientes = pacientes,
        pacienteId = if (modo == ModoPaciente.EXISTENTE && !intentado) pacientes.firstOrNull()?.id else null,
        errorPaciente = if (intentado && modo == ModoPaciente.EXISTENTE) "Selecciona un paciente" else null,
        errorNombre = if (intentado && modo == ModoPaciente.NUEVO) "Escribe tu nombre completo" else null,
        fechasDisponibles = fechas,
        franjas = franjasDisponibles(ayuno),
        fecha = if (intentado) null else fechas[1],
        errorFecha = if (intentado) "Selecciona una fecha" else null,
        errorHora = if (intentado) "Selecciona una hora" else null,
        intentado = intentado
    )
}

private val pacientesDeEjemplo = listOf(
    Paciente(1, "Marta Ruiz Gómez", "1020304050", "3101234567", "marta@correo.com"),
    Paciente(2, "Andrés Felipe Castañeda", "79123456", "3209876543", "andres@correo.com"),
    Paciente(3, "Luz Mery Pardo", "52987654", "3005551234", "luz@correo.com")
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AgendarPacienteRegistradoPreview() {
    VitaLabTheme { AgendarCitaContenido(estadoDeEjemplo(), AgendarAcciones()) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AgendarSinPacientesConErroresPreview() {
    VitaLabTheme {
        AgendarCitaContenido(estadoDeEjemplo(ModoPaciente.NUEVO, pacientes = emptyList(), intentado = true), AgendarAcciones())
    }
}

@Preview(showBackground = true)
@Composable
private fun AgendarOrdenVaciaPreview() {
    VitaLabTheme { AgendarCitaContenido(AgendarUiState(cargando = false), AgendarAcciones()) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AgendarConfirmadaPreview() {
    VitaLabTheme {
        AgendarCitaContenido(AgendarUiState(cargando = false, confirmada = citaDeEjemplo(domicilio = true)), AgendarAcciones())
    }
}
