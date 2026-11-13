package com.vitalab.app.ui.pacientes

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.vitalab.app.data.local.CitasPorPaciente
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.ExamenPopular
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.local.PacienteConConteo
import com.vitalab.app.ui.screens.estadisticas.EstadisticasContenido
import com.vitalab.app.ui.screens.pacientes.PacienteFormContenido
import com.vitalab.app.ui.screens.pacientes.PacientesContenido
import com.vitalab.app.viewmodel.EstadisticasUiState
import com.vitalab.app.viewmodel.PacienteFormUiState
import com.vitalab.app.viewmodel.PacientesUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Pantallas de pacientes y estadísticas sin estado: lo que se ve y qué callbacks dispara cada control. */
@RunWith(RobolectricTestRunner::class)
class PantallasPacientesUiTest {
    // Misma razón que en NavegacionArgumentosTest: registrar la Activity de prueba sin `ui-test-manifest`.
    @get:Rule(order = 0) val registrarActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule(order = 1) val compose = createComposeRule()

    private val marta = PacienteConConteo(
        Paciente(id = 7, nombre = "Marta Ruiz", documento = "1020304050", telefono = "3001112233", correo = "marta@correo.com"),
        totalCitas = 3
    )

    @Test
    fun listaMuestraElPacienteYDisparaEditarYNuevo() {
        var editado: Long? = null
        var nuevos = 0
        compose.setContent {
            MaterialTheme {
                PacientesContenido(
                    PacientesUiState(pacientes = listOf(marta), cargando = false),
                    onConsultaChange = {}, onNuevo = { nuevos++ }, onEditar = { editado = it },
                    onPedirEliminar = {}, onConfirmarEliminar = {}, onDescartarEliminar = {}, onMensajeMostrado = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Marta Ruiz. Documento 1020304050.", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar a Marta Ruiz").performClick()
        // El texto del FAB no está en el árbol semántico (lo oculta Material3): se busca por su descripción.
        compose.onNodeWithContentDescription("Nuevo paciente").performClick()
        assertEquals(7L, editado)
        assertEquals(1, nuevos)
    }

    @Test
    fun dialogoDeEliminarAvisaQueSeBorranLasCitasYConfirma() {
        var confirmado = 0
        compose.setContent {
            MaterialTheme {
                PacientesContenido(
                    PacientesUiState(consulta = "zzz", cargando = false, pacientePorEliminar = marta),
                    onConsultaChange = {}, onNuevo = {}, onEditar = {},
                    onPedirEliminar = {}, onConfirmarEliminar = { confirmado++ }, onDescartarEliminar = {}, onMensajeMostrado = {}
                )
            }
        }
        compose.onNodeWithText("¿Eliminar a Marta Ruiz?").assertIsDisplayed()
        compose.onNodeWithText("Se borrarán sus datos y sus 3 citas.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        assertEquals(1, confirmado)
    }

    @Test
    fun formularioMuestraElErrorDeDocumentoYGuarda() {
        var guardados = 0
        var volvio = 0
        compose.setContent {
            MaterialTheme {
                PacienteFormContenido(
                    PacienteFormUiState(
                        esEdicion = true, nombre = "Marta Ruiz", documento = "1020304050", intentado = true,
                        errorDocumento = "Ya existe un paciente con el documento 1020304050."
                    ),
                    onNombreChange = {}, onDocumentoChange = {}, onTelefonoChange = {}, onCorreoChange = {},
                    onGuardar = { guardados++ }, onVolver = { volvio++ }
                )
            }
        }
        compose.onNodeWithText("Editar paciente").assertIsDisplayed()
        compose.onNodeWithText("Ya existe un paciente con el documento 1020304050.").assertIsDisplayed()
        // El botón queda debajo de los cuatro campos, fuera de la pantalla pequeña de Robolectric: hay que desplazarse.
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, guardados)
        assertEquals(1, volvio)
    }

    @Test
    fun estadisticasMuestranValoresEnTextoYEstadoVacio() {
        val conDatos = EstadisticasUiState(
            cargando = false,
            citasPorEstado = mapOf(EstadoCita.PROGRAMADA to 4, EstadoCita.COMPLETADA to 7, EstadoCita.CANCELADA to 1),
            totalCitas = 12, ingresos = 1_245_000, ahorro = 186_750,
            topExamenes = listOf(ExamenPopular(3, "Perfil lipídico", 6), ExamenPopular(1, "Hemograma", 1)),
            citasPorPaciente = listOf(CitasPorPaciente(1, "Marta Ruiz", 5))
        )
        var volvio = 0
        var estado by mutableStateOf(conDatos)
        compose.setContent { MaterialTheme { EstadisticasContenido(estado, onVolver = { volvio++ }) } }

        compose.onNodeWithText("$1.245.000").assertIsDisplayed()
        compose.onNodeWithContentDescription("Perfil lipídico: 6 veces").assertExists()
        compose.onNodeWithContentDescription("Hemograma: 1 vez").assertExists()
        compose.onNodeWithContentDescription("Marta Ruiz: 5 citas").assertExists()

        compose.runOnIdle { estado = EstadisticasUiState(cargando = false) }
        compose.onNodeWithText("Todavía no hay citas", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Volver").performClick()
        assertEquals(1, volvio)
    }
}
