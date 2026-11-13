package com.vitalab.app.ui.screens.pacientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalab.app.ui.components.CampoValidado
import com.vitalab.app.ui.components.EncabezadoConAtras
import com.vitalab.app.ui.components.EstadoVacio
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.PacienteFormUiState
import com.vitalab.app.viewmodel.PacienteFormViewModel

/** Crear o editar un paciente (si la ruta trae `id`, es edición). Al guardar vuelve a la lista. */
@Composable
fun PacienteFormScreen(
    viewModel: PacienteFormViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }
    PacienteFormContenido(
        estado,
        onNombreChange = viewModel::onNombreChange,
        onDocumentoChange = viewModel::onDocumentoChange,
        onTelefonoChange = viewModel::onTelefonoChange,
        onCorreoChange = viewModel::onCorreoChange,
        onGuardar = viewModel::onGuardar,
        onVolver = onVolver
    )
}

@Composable
internal fun PacienteFormContenido(
    estado: PacienteFormUiState,
    onNombreChange: (String) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onVolver: () -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        EncabezadoConAtras(
            titulo = if (estado.esEdicion) "Editar paciente" else "Nuevo paciente",
            subtitulo = if (estado.esEdicion) "Actualiza sus datos de contacto" else "Registra los datos del paciente",
            onVolver = onVolver
        )
        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            estado.noEncontrado -> EstadoVacio(
                Icons.Filled.Warning,
                "No encontramos a este paciente. Puede que ya se haya eliminado.",
                "Volver a pacientes",
                onVolver
            )
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Los errores ya vienen filtrados por el ViewModel (solo si escribió algo o ya intentó guardar).
                CampoValidado(estado.nombre, onNombreChange, "Nombre completo", estado.errorNombre, true, Icons.Filled.Person)
                CampoValidado(estado.documento, onDocumentoChange, "Documento de identidad", estado.errorDocumento, true, Icons.Filled.AccountBox, KeyboardType.Number)
                CampoValidado(estado.telefono, onTelefonoChange, "Celular", estado.errorTelefono, true, Icons.Filled.Phone, KeyboardType.Phone)
                CampoValidado(estado.correo, onCorreoChange, "Correo electrónico", estado.errorCorreo, true, Icons.Filled.Email, KeyboardType.Email)
                Spacer(Modifier.size(4.dp))
                Button(
                    onClick = onGuardar,
                    enabled = !estado.guardando && !estado.guardado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        when {
                            estado.guardando -> "Guardando…"
                            estado.esEdicion -> "Guardar cambios"
                            else -> "Guardar paciente"
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PacienteFormNuevoPreview() {
    VitaLabTheme { PacienteFormContenido(PacienteFormUiState(), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PacienteFormErroresPreview() {
    VitaLabTheme {
        PacienteFormContenido(
            PacienteFormUiState(
                esEdicion = true, nombre = "Marta Ruiz", documento = "1020304050", telefono = "12345", correo = "marta@",
                errorDocumento = "Ya existe un paciente con el documento 1020304050.",
                errorTelefono = "Celular de 10 dígitos que empiece por 3", errorCorreo = "Correo electrónico no válido", intentado = true
            ),
            {}, {}, {}, {}, {}, {}
        )
    }
}
