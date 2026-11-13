package com.vitalab.app.ui.citas

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.vitalab.app.FakeLabRepository
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.ui.components.citaDeEjemplo
import com.vitalab.app.ui.screens.citas.AgendarCitaScreen
import com.vitalab.app.ui.screens.citas.CitaDetalleScreen
import com.vitalab.app.ui.screens.citas.CitasScreen
import com.vitalab.app.ui.screens.citas.ReprogramarCitaScreen
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.AgendarViewModel
import com.vitalab.app.viewmodel.CitaDetalleViewModel
import com.vitalab.app.viewmodel.CitasViewModel
import com.vitalab.app.viewmodel.ReprogramarCitaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

/** Pantallas de citas en la JVM (Robolectric + Compose) con ViewModels reales y el repositorio en memoria. */
@RunWith(RobolectricTestRunner::class)
class PantallasCitasTest {
    // Misma razón que en NavegacionArgumentosTest: la Activity de prueba se registra aquí, no con ui-test-manifest.
    @get:Rule(order = 0) val registrarActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule(order = 1) val compose = createComposeRule()

    private val repo = FakeLabRepository()
    private val hoy = LocalDate.of(2026, 11, 12) // jueves
    private val lunes16 = formatoFecha(LocalDate.of(2026, 11, 16))

    private fun sembrar(vararg citas: com.vitalab.app.data.local.CitaCompleta) {
        repo.listaCitas.value = citas.toList()
    }

    @Test
    fun consultaFiltraPorEstadoYBuscaYAbreElDetalle() {
        sembrar(
            citaDeEjemplo(1, codigo = "VL-1111", paciente = "Marta Ruiz"),
            citaDeEjemplo(2, EstadoCita.CANCELADA, codigo = "VL-2222", paciente = "Luis Pérez")
        )
        var abierta = -1L
        compose.setContent { VitaLabTheme { CitasScreen(CitasViewModel(repo), { abierta = it }, {}, {}) } }

        compose.onNodeWithText("Marta Ruiz").assertExists()
        compose.onNodeWithText("Luis Pérez").assertExists()

        compose.onNodeWithText("Canceladas").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Marta Ruiz").assertDoesNotExist()
        compose.onNodeWithText("Luis Pérez").assertExists()
        // El estado se dice con texto, no solo con color.
        compose.onNodeWithText("Cancelada").assertExists()

        compose.onNodeWithText("Todas").performClick()
        compose.onNodeWithText("Buscar por paciente o código").performTextInput("1111")
        compose.waitForIdle()
        compose.onNodeWithText("Luis Pérez").assertDoesNotExist()
        compose.onNodeWithText("Marta Ruiz").performClick()
        assertEquals(1L, abierta)

        compose.onNodeWithContentDescription("Borrar búsqueda").performClick()
        compose.onNodeWithText("Buscar por paciente o código").performTextInput("zzz")
        compose.waitForIdle()
        compose.onNodeWithText("No encontramos citas con esos filtros.").assertExists()
        compose.onNodeWithText("Limpiar filtros").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Marta Ruiz").assertExists()
        compose.onNodeWithText("Luis Pérez").assertExists()
    }

    @Test
    fun consultaSinCitasOfreceAgendar() {
        var agendar = false
        compose.setContent { VitaLabTheme { CitasScreen(CitasViewModel(repo), {}, { agendar = true }, {}) } }
        compose.waitForIdle()
        compose.onNodeWithText("Agendar cita").performClick()
        assertTrue(agendar)
    }

    @Test
    fun detalleCancelaConConfirmacionYLuegoElimina() {
        sembrar(citaDeEjemplo(1, codigo = "VL-1111", paciente = "Marta Ruiz"))
        var volvio = false
        compose.setContent {
            VitaLabTheme { CitaDetalleScreen(CitaDetalleViewModel(repo, SavedStateHandle(mapOf("id" to 1L))), { volvio = true }, {}) }
        }
        compose.onNodeWithText("Programada").assertExists()
        compose.onNodeWithText("Reprogramar").performScrollTo().assertExists()

        compose.onNodeWithText("Cancelar cita").performScrollTo().performClick()
        compose.onNodeWithText("¿Cancelar la cita?").assertExists()
        // Descartar el diálogo no cambia nada.
        compose.onNodeWithText("No, volver").performClick()
        assertEquals(EstadoCita.PROGRAMADA, repo.listaCitas.value.single().cita.estado)

        compose.onNodeWithText("Cancelar cita").performScrollTo().performClick()
        compose.onNodeWithText("Sí, cancelar").performClick()
        compose.waitForIdle()
        assertEquals(EstadoCita.CANCELADA, repo.listaCitas.value.single().cita.estado)
        compose.onNodeWithText("Cita cancelada").assertExists()
        // El aviso se retira solo (y el ViewModel lo olvida): ya no tapa el botón de abajo.
        compose.mainClock.advanceTimeBy(6_000)
        compose.waitForIdle()
        compose.onNodeWithText("Cita cancelada").assertDoesNotExist()
        // Ya no es PROGRAMADA: no hay acciones de cambio, solo eliminar.
        compose.onNodeWithText("Reprogramar").assertDoesNotExist()

        compose.onNodeWithText("Eliminar del historial").performScrollTo().performClick()
        compose.onNodeWithText("Sí, eliminar").performClick()
        compose.waitForIdle()
        assertTrue(repo.listaCitas.value.isEmpty())
        assertTrue(volvio)
    }

    @Test
    fun reprogramarRespetaElAyunoYGuardaLaNuevaHora() {
        sembrar(citaDeEjemplo(1, codigo = "VL-1111")) // incluye perfil lipídico: ayuno de 12 h
        var volvio = false
        compose.setContent {
            VitaLabTheme {
                ReprogramarCitaScreen(
                    ReprogramarCitaViewModel(repo, SavedStateHandle(mapOf("id" to 1L)), hoy = { hoy }),
                    onVolver = { volvio = true }
                )
            }
        }
        compose.onNodeWithText("7:00 a.m.").performScrollTo().assertIsSelected() // precargada
        compose.onNodeWithText("10:00 a.m.").assertDoesNotExist() // con ayuno solo hasta las 9:00
        compose.onNodeWithContentDescription(lunes16).performScrollTo().performClick()
        compose.onNodeWithText("8:00 a.m.").performScrollTo().performClick()
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.waitForIdle()

        val guardada = repo.listaCitas.value.single().cita
        assertEquals("8:00 a.m.", guardada.hora)
        assertEquals(LocalDate.of(2026, 11, 16), guardada.fechaLocal)
        assertTrue(volvio)
    }

    @Test
    fun agendarValidaYConfirmaConUnPacienteRegistrado() {
        repo.ordenIds.value = listOf(1, 2)
        repo.listaPacientes.value = listOf(Paciente(1, "Ana Gómez", "1020304050", "3001234567", "ana@correo.com"))
        var verCita = -1L
        compose.setContent {
            VitaLabTheme { AgendarCitaScreen(AgendarViewModel(repo, hoy = { hoy }), {}, {}, { verCita = it }) }
        }
        // Sin elegir nada, el intento muestra los errores del ViewModel.
        compose.onNodeWithText("Confirmar cita").performScrollTo().performClick()
        compose.onNodeWithText("Selecciona un paciente").assertExists()
        compose.onNodeWithText("Revisa los campos marcados en rojo.").assertExists()

        compose.onNodeWithText("Ana Gómez").performScrollTo().performClick()
        compose.onNodeWithContentDescription(lunes16).performScrollTo().performClick()
        compose.onNodeWithText("7:00 a.m.").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar cita").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("¡Cita confirmada!").assertExists()
        assertFalse(repo.listaCitas.value.isEmpty())
        compose.onNodeWithText("Ver cita").performScrollTo().performClick()
        assertEquals(repo.listaCitas.value.single().cita.id, verCita)
    }

    @Test
    fun agendarSinPacientesCreaUnoNuevoYSinOrdenVaAlCatalogo() {
        // Sin exámenes en la orden: estado vacío con salida al catálogo.
        var catalogo = false
        compose.setContent {
            VitaLabTheme { AgendarCitaScreen(AgendarViewModel(repo, hoy = { hoy }), {}, {}, {}, onVerExamenes = { catalogo = true }) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Ver exámenes").performClick()
        assertTrue(catalogo)
    }

    @Test
    fun agendarSinPacientesMuestraElFormularioDePacienteNuevo() {
        repo.ordenIds.value = listOf(1)
        compose.setContent { VitaLabTheme { AgendarCitaScreen(AgendarViewModel(repo, hoy = { hoy }), {}, {}, {}) } }
        compose.waitForIdle()
        compose.onNodeWithText("Nombre completo").assertExists()
        compose.onNodeWithText("Nombre completo").performTextInput("Ana Gómez")
        compose.onNodeWithText("Número de documento").performTextInput("123")
        compose.waitForIdle()
        compose.onNodeWithText("Solo números, entre 6 y 10 dígitos").assertExists()
    }
}
