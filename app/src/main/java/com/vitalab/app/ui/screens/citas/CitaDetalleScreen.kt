package com.vitalab.app.ui.screens.citas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.Etiqueta
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.EtiquetaEstadoCita
import com.vitalab.app.ui.components.FilaExamenCita
import com.vitalab.app.ui.components.FilaDato
import com.vitalab.app.ui.components.TarjetaIndicaciones
import com.vitalab.app.ui.components.TarjetaResumenCosto
import com.vitalab.app.ui.components.citaDeEjemplo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.AccionCita
import com.vitalab.app.viewmodel.CitaDetalleUiState
import com.vitalab.app.viewmodel.CitaDetalleViewModel

/** Detalle de una cita (el id llega por la ruta al ViewModel): datos, exámenes con precio cobrado, costos y acciones. */
@Composable
fun CitaDetalleScreen(
    viewModel: CitaDetalleViewModel,
    onVolver: () -> Unit,
    onReprogramar: (citaId: Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    CitaDetalleContenido(
        estado = estado,
        onVolver = onVolver,
        onReprogramar = onReprogramar,
        onPedirAccion = viewModel::onPedirAccion,
        onDescartarAccion = viewModel::onDescartarAccion,
        onConfirmarAccion = viewModel::onConfirmarAccion,
        onMensajeMostrado = viewModel::onMensajeMostrado
    )
}

@Composable
internal fun CitaDetalleContenido(
    estado: CitaDetalleUiState,
    onVolver: () -> Unit,
    onReprogramar: (Long) -> Unit,
    onPedirAccion: (AccionCita) -> Unit,
    onDescartarAccion: () -> Unit,
    onConfirmarAccion: () -> Unit,
    onMensajeMostrado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    // Eliminada: no hay nada que mostrar, se vuelve a la lista.
    LaunchedEffect(estado.eliminada) { if (estado.eliminada) onVolver() }
    // El aviso se limpia al terminar de mostrarse (limpiarlo antes cancelaría este efecto).
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        val cita = estado.cita
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a mis citas") }
                EncabezadoPantalla(
                    titulo = cita?.let { "Cita ${it.cita.codigo}" } ?: "Detalle de la cita",
                    subtitulo = cita?.let { "${formatoFecha(it.cita.fechaLocal)} · ${it.cita.hora}" } ?: ""
                )
            }
            if (estado.procesando) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                estado.noEncontrada -> EstadoVacio(
                    Icons.Filled.Info, "No encontramos esta cita. Puede que ya se haya eliminado.", "Volver a mis citas", onVolver
                )
                cita == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> DetalleCita(estado, cita, onReprogramar, onPedirAccion)
            }
        }
    }

    estado.accionPendiente?.let { accion ->
        DialogoConfirmacion(accion, estado.cita, onConfirmar = onConfirmarAccion, onDescartar = onDescartarAccion)
    }
}

@Composable
private fun DetalleCita(
    estado: CitaDetalleUiState,
    completa: CitaCompleta,
    onReprogramar: (Long) -> Unit,
    onPedirAccion: (AccionCita) -> Unit
) {
    val cita = completa.cita
    val paciente = completa.paciente
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EtiquetaEstadoCita(cita.estado)
            Etiqueta(if (cita.domicilio) "A domicilio" else "En sede")
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Paciente", style = MaterialTheme.typography.titleMedium)
                Text(paciente.nombre, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                FilaDato("Documento", paciente.documento)
                FilaDato("Celular", paciente.telefono)
                FilaDato("Correo", paciente.correo)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Datos de la cita", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp)) {
                    FilaDato("Fecha", formatoFecha(cita.fechaLocal))
                    FilaDato("Hora", cita.hora)
                    FilaDato("Modalidad", if (cita.domicilio) "A domicilio" else "En sede")
                    if (cita.domicilio) FilaDato("Dirección", cita.direccion)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Exámenes (${estado.lineas.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Precios cobrados al agendar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(Modifier.padding(top = 4.dp)) { estado.lineas.forEach { FilaExamenCita(it) } }
            }
        }

        TarjetaResumenCosto(completa.resumen)
        // La preparación solo importa mientras la toma de muestra sigue pendiente.
        if (estado.puedeModificar) TarjetaIndicaciones(estado.indicaciones)

        AccionesCita(estado, cita.id, onReprogramar, onPedirAccion)
    }
}

@Composable
private fun AccionesCita(
    estado: CitaDetalleUiState,
    citaId: Long,
    onReprogramar: (Long) -> Unit,
    onPedirAccion: (AccionCita) -> Unit
) {
    val libre = !estado.procesando
    val peligro = MaterialTheme.colorScheme.error
    Column(Modifier.padding(top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (estado.puedeModificar) {
            Button(onClick = { onReprogramar(citaId) }, enabled = libre, modifier = Modifier.fillMaxWidth()) { Text("Reprogramar") }
            FilledTonalButton(onClick = { onPedirAccion(AccionCita.COMPLETAR) }, enabled = libre, modifier = Modifier.fillMaxWidth()) {
                Text("Marcar completada")
            }
            OutlinedButton(
                onClick = { onPedirAccion(AccionCita.CANCELAR) },
                enabled = libre,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = peligro),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar cita") }
        }
        TextButton(
            onClick = { onPedirAccion(AccionCita.ELIMINAR) },
            enabled = libre,
            colors = ButtonDefaults.textButtonColors(contentColor = peligro),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Eliminar del historial")
        }
    }
}

@Composable
private fun DialogoConfirmacion(accion: AccionCita, cita: CitaCompleta?, onConfirmar: () -> Unit, onDescartar: () -> Unit) {
    val quien = cita?.let { "${it.cita.codigo} de ${it.paciente.nombre}" } ?: "esta cita"
    val (titulo, texto, botonConfirmar) = when (accion) {
        AccionCita.CANCELAR -> Triple(
            "¿Cancelar la cita?",
            "La cita $quien quedará como cancelada y ya no se podrá reprogramar.",
            "Sí, cancelar"
        )
        AccionCita.COMPLETAR -> Triple(
            "¿Marcar como completada?",
            "La cita $quien quedará como completada y ya no se podrá cambiar.",
            "Sí, completar"
        )
        AccionCita.ELIMINAR -> Triple(
            "¿Eliminar del historial?",
            "La cita $quien se borrará del historial de forma definitiva.",
            "Sí, eliminar"
        )
    }
    val destructiva = accion != AccionCita.COMPLETAR
    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = if (destructiva) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors()
            ) { Text(botonConfirmar) }
        },
        dismissButton = { TextButton(onClick = onDescartar) { Text("No, volver") } }
    )
}

private fun estadoDeEjemplo(cita: CitaCompleta, accion: AccionCita? = null) = CitaDetalleUiState(
    cargando = false,
    cita = cita,
    lineas = cita.detalle,
    indicaciones = indicacionesPreparacion(cita.examenes),
    puedeModificar = cita.cita.estado == EstadoCita.PROGRAMADA,
    accionPendiente = accion
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DetalleProgramadaPreview() {
    VitaLabTheme {
        CitaDetalleContenido(estadoDeEjemplo(citaDeEjemplo(domicilio = true)), {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DetalleCompletadaPreview() {
    VitaLabTheme {
        CitaDetalleContenido(estadoDeEjemplo(citaDeEjemplo(estado = EstadoCita.COMPLETADA)), {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalleConfirmarCancelarPreview() {
    VitaLabTheme {
        CitaDetalleContenido(estadoDeEjemplo(citaDeEjemplo(), AccionCita.CANCELAR), {}, {}, {}, {}, {}, {})
    }
}
