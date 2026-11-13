package com.vitalab.app.ui.screens.pacientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.local.PacienteConConteo
import com.vitalab.app.ui.components.DialogoEliminarPaciente
import com.vitalab.app.ui.components.EncabezadoPantalla
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.components.TarjetaPaciente
import com.vitalab.app.ui.components.textoConteo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.PacientesUiState
import com.vitalab.app.viewmodel.PacientesViewModel

/** Pacientes registrados (consulta): búsqueda, alta, edición y baja con confirmación. */
@Composable
fun PacientesScreen(
    viewModel: PacientesViewModel,
    onNuevo: () -> Unit,
    onEditar: (pacienteId: Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PacientesContenido(
        estado,
        onConsultaChange = viewModel::onConsultaChange,
        onNuevo = onNuevo,
        onEditar = onEditar,
        onPedirEliminar = viewModel::onPedirEliminar,
        onConfirmarEliminar = viewModel::onConfirmarEliminar,
        onDescartarEliminar = viewModel::onDescartarEliminar,
        onMensajeMostrado = viewModel::onMensajeMostrado
    )
}

@Composable
internal fun PacientesContenido(
    estado: PacientesUiState,
    onConsultaChange: (String) -> Unit,
    onNuevo: () -> Unit,
    onEditar: (Long) -> Unit,
    onPedirEliminar: (PacienteConConteo) -> Unit,
    onConfirmarEliminar: () -> Unit,
    onDescartarEliminar: () -> Unit,
    onMensajeMostrado: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val foco = LocalFocusManager.current
    // Se muestra y después se limpia: limpiarlo antes cancelaría el Snackbar.
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    val enBusqueda = estado.consulta.isNotBlank()
    val sinPacientes = !estado.cargando && estado.pacientes.isEmpty() && !enBusqueda
    val subtitulo = when {
        estado.cargando -> "Cargando…"
        enBusqueda -> textoConteo(estado.pacientes.size, "resultado", "resultados")
        else -> textoConteo(estado.pacientes.size, "paciente registrado", "pacientes registrados")
    }

    // El Scaffold de LabApp ya reserva las barras del sistema: aquí no se vuelven a sumar.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // Con la lista vacía el botón de acción ya está en el estado vacío: no se repite.
            if (!estado.cargando && !sinPacientes) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo paciente") },
                    // Material3 1.3 oculta el texto del FAB extendido al lector de pantalla: sin esto TalkBack solo diría "Botón".
                    modifier = Modifier.semantics { contentDescription = "Nuevo paciente" }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            EncabezadoPantalla("Pacientes", subtitulo)
            if (!sinPacientes) {
                OutlinedTextField(
                    value = estado.consulta,
                    onValueChange = onConsultaChange,
                    label = { Text("Buscar por nombre o documento") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (enBusqueda) {
                            IconButton(onClick = { onConsultaChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda") }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    sinPacientes -> EstadoVacio(
                        Icons.Filled.Person,
                        "Aún no hay pacientes registrados. Registra el primero para poder agendarle citas.",
                        "Registrar paciente",
                        onNuevo
                    )
                    estado.pacientes.isEmpty() -> EstadoVacio(
                        Icons.Filled.Search,
                        "No encontramos pacientes con “${estado.consulta.trim()}”.",
                        "Limpiar búsqueda",
                        { onConsultaChange("") }
                    )
                    else -> LazyColumn(
                        // 96 dp abajo: el botón flotante no tapa la última tarjeta.
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(estado.pacientes, key = { it.paciente.id }) { item ->
                            TarjetaPaciente(item, onEditar = { onEditar(item.paciente.id) }, onEliminar = { onPedirEliminar(item) })
                        }
                    }
                }
            }
        }
    }

    estado.pacientePorEliminar?.let { DialogoEliminarPaciente(it, onConfirmarEliminar, onDescartarEliminar) }
}

private val pacientesDeMuestra = listOf(
    PacienteConConteo(Paciente(id = 1, nombre = "Marta Lucía Ruiz", documento = "1020304050", telefono = "3001112233", correo = "marta.ruiz@correo.com"), totalCitas = 3),
    PacienteConConteo(Paciente(id = 2, nombre = "Andrés Felipe Gómez", documento = "79854123", telefono = "3157894561", correo = "andres@correo.com"), totalCitas = 1),
    PacienteConConteo(Paciente(id = 3, nombre = "Camila Torres", documento = "1098765432", telefono = "3204567890", correo = "camila.torres@correo.com"), totalCitas = 0)
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PacientesScreenPreview() {
    VitaLabTheme {
        PacientesContenido(PacientesUiState(pacientes = pacientesDeMuestra, cargando = false), {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PacientesScreenVaciaPreview() {
    VitaLabTheme {
        PacientesContenido(PacientesUiState(cargando = false), {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PacientesScreenConfirmarPreview() {
    VitaLabTheme {
        PacientesContenido(
            PacientesUiState(pacientes = pacientesDeMuestra, cargando = false, pacientePorEliminar = pacientesDeMuestra.first()),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
