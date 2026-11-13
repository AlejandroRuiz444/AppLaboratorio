package com.vitalab.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.repository.PacienteDestino
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.repository.SolicitudCita
import com.vitalab.app.navigation.Rutas
import com.vitalab.app.ui.labViewModel
import com.vitalab.app.viewmodel.CitaDetalleViewModel
import com.vitalab.app.viewmodel.ExamenFormViewModel
import com.vitalab.app.viewmodel.PacienteFormViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

/**
 * Prueba de punta a punta de la convención de ids: argumento de ruta -> SavedStateHandle -> ViewModel
 * (creado con `labViewModel()` y la fábrica real) -> Room real -> texto en pantalla.
 */
@RunWith(RobolectricTestRunner::class)
class NavegacionArgumentosTest {
    // `createComposeRule` necesita ComponentActivity registrada. Se registra aquí (y no con `ui-test-manifest`)
    // para que esa Activity de prueba no termine exportada dentro del APK de depuración.
    @get:Rule(order = 0) val registrarActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun elIdDeLaRutaLlegaALosViewModelsDeDetalleYEdicion() {
        val repo = ApplicationProvider.getApplicationContext<VitaLabApp>().container.repositorio
        val (pacienteId, cita) = runBlocking {
            val pid = (repo.guardarPaciente(Paciente(nombre = "Marta Ruiz", documento = "9988776655", telefono = "3001112233", correo = "m@m.com")) as Resultado.Exito).valor
            repo.alternarEnOrden(1)
            val solicitud = SolicitudCita(PacienteDestino.Existente(pid), LocalDate.of(2026, 11, 16).toEpochDay(), "7:00 a.m.", false, "")
            pid to (repo.crearCitaDesdeOrden(solicitud) as Resultado.Exito).valor
        }

        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Rutas.INICIO) {
                composable(Rutas.INICIO) { Text("inicio") }
                composable(Rutas.CITA_DETALLE, arguments = Rutas.argId) {
                    val estado by labViewModel<CitaDetalleViewModel>().uiState.collectAsStateWithLifecycle()
                    Text("detalle ${estado.cita?.cita?.codigo}")
                }
                composable(Rutas.PACIENTE_EDITAR, arguments = Rutas.argId) {
                    val estado by labViewModel<PacienteFormViewModel>().uiState.collectAsStateWithLifecycle()
                    Text("paciente ${estado.nombre} edicion=${estado.esEdicion}")
                }
                composable(Rutas.PACIENTE_NUEVO) {
                    val estado by labViewModel<PacienteFormViewModel>().uiState.collectAsStateWithLifecycle()
                    Text("paciente nuevo edicion=${estado.esEdicion}")
                }
                composable(Rutas.EXAMEN_EDITAR, arguments = Rutas.argId) {
                    val estado by labViewModel<ExamenFormViewModel>().uiState.collectAsStateWithLifecycle()
                    Text("examen ${estado.nombre}")
                }
            }
        }

        fun irA(ruta: String, texto: String) {
            compose.runOnUiThread { nav.navigate(ruta) }
            compose.waitUntil(10_000) { runCatching { compose.onNodeWithText(texto).assertExists() }.isSuccess }
        }
        irA(Rutas.citaDetalle(cita.cita.id), "detalle ${cita.cita.codigo}")
        irA(Rutas.pacienteEditar(pacienteId), "paciente Marta Ruiz edicion=true")
        irA(Rutas.PACIENTE_NUEVO, "paciente nuevo edicion=false")
        irA(Rutas.examenEditar(3), "examen Perfil lipídico")
    }
}
