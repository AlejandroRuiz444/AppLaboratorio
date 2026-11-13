package com.vitalab.app.ui.catalogo

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.Examen
import com.vitalab.app.navigation.LabScaffold
import com.vitalab.app.navigation.Rutas
import com.vitalab.app.ui.screens.ExamenFormContenido
import com.vitalab.app.ui.screens.ExamenesContenido
import com.vitalab.app.ui.screens.InicioContenido
import com.vitalab.app.ui.screens.OrdenContenido
import com.vitalab.app.ui.screens.citaDeEjemplo
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.CatalogoUiState
import com.vitalab.app.viewmodel.ExamenFormUiState
import com.vitalab.app.viewmodel.InicioUiState
import com.vitalab.app.viewmodel.OrdenUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Pruebas de las pantallas del catálogo, la orden, el inicio y la barra de navegación, con las versiones sin estado
 * (sin Room ni ViewModel): comprueban qué se dibuja y qué eventos se emiten. La conexión con Room ya la cubren
 * `RoomLabRepositoryTest` y `NavegacionArgumentosTest`.
 */
@RunWith(RobolectricTestRunner::class)
class CatalogoUiTest {
    // Misma razón que en NavegacionArgumentosTest: la Activity de prueba se registra aquí y no con `ui-test-manifest`.
    @get:Rule(order = 0) val registrarActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule(order = 1) val compose = createComposeRule()

    private fun catalogoAdmin(vararg examenes: Examen) =
        CatalogoUiState(examenes = examenes.toList(), totalActivos = examenes.count { it.activo }, modoAdmin = true, cargando = false)

    private fun mostrarCatalogo(
        estado: CatalogoUiState,
        cambiados: MutableList<Examen> = mutableListOf(),
        editados: MutableList<Int> = mutableListOf()
    ) = compose.setContent {
        VitaLabTheme {
            ExamenesContenido(
                estado, onBusquedaChange = {}, onCategoriaChange = {}, onLimpiarFiltros = {}, onModoAdminChange = {},
                onAlternarOrden = {}, onCambiarActivo = { cambiados += it }, onVerOrden = {}, onNuevoExamen = {},
                onEditarExamen = { editados += it }
            )
        }
    }

    // Pantalla alta: con la de fábrica (320x470 dp) la fila de acciones del admin queda fuera de la ventana y el clic no llega,
    // por eso fallaron las primeras versiones de estas pruebas. Que el diálogo se abra se comprueba aquí; los botones
    // Desactivar/Cancelar del diálogo no tienen prueba propia (quedan para el emulador).
    @Config(qualifiers = "w411dp-h891dp")
    @Test
    fun elModoAdminOfreceEditarYDesactivarYDesactivarNoCambiaNadaSinConfirmar() {
        val cambiados = mutableListOf<Examen>()
        val editados = mutableListOf<Int>()
        mostrarCatalogo(catalogoAdmin(catalogoExamenes[0]), cambiados, editados)

        compose.onNodeWithText("Editar").performClick()
        assertEquals(listOf(catalogoExamenes[0].id), editados)

        compose.onNodeWithText("Desactivar").performClick()
        assertTrue("Desactivar solo pide confirmación: el cambio lo emite el botón del diálogo", cambiados.isEmpty())
        assertEquals("Desactivar debe abrir el diálogo de confirmación", 1, compose.onAllNodes(isDialog()).fetchSemanticsNodes().size)
    }

    @Test
    fun unExamenDesactivadoSeMarcaSeReactivaYYaNoSeAgrega() {
        mostrarCatalogo(catalogoAdmin(catalogoExamenes[1].copy(activo = false)))

        compose.onNodeWithText("Desactivado").assertExists()
        compose.onNodeWithText("Agregar").assertDoesNotExist()
        compose.onNodeWithText("Reactivar").assertExists()
    }

    @Test
    fun sinModoAdminNoSeOfrecenEditarNiDesactivar() {
        mostrarCatalogo(CatalogoUiState(examenes = listOf(catalogoExamenes[0]), totalActivos = 1, cargando = false), mutableListOf())

        compose.onNodeWithText("Agregar").assertExists()
        compose.onNodeWithText("Nuevo examen").assertExists()
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Desactivar").assertDoesNotExist()
    }

    @Test
    fun elFormularioMuestraLosErroresDelViewModelYEmiteLosEventos() {
        var nombre = ""
        var guardados = 0
        compose.setContent {
            VitaLabTheme {
                ExamenFormContenido(
                    ExamenFormUiState(intentado = true, errorNombre = "El nombre debe tener al menos 3 letras", errorPrecio = "Precio en pesos, mayor que 0"),
                    onVolver = {}, onNombreChange = { nombre = it }, onCategoriaChange = {}, onPrecioChange = {}, onDescripcionChange = {},
                    onMuestraChange = {}, onAyunoChange = {}, onEntregaChange = {}, onIndicacionChange = {}, onGuardar = { guardados++ }
                )
            }
        }

        compose.onNodeWithText("Nuevo examen").assertExists()
        compose.onNodeWithText("El nombre debe tener al menos 3 letras").assertExists()
        compose.onNodeWithText("Revisa los campos marcados en rojo.").assertExists()

        compose.onNodeWithText("Nombre del examen").performTextInput("Gl")
        assertEquals("Gl", nombre)

        compose.onNodeWithText("Guardar examen").performClick()
        assertEquals(1, guardados)
    }

    @Test
    fun laOrdenMuestraElTotalAgendaYGuardaElSwitchDeAdultoMayor() {
        val examenes = catalogoExamenes.take(1)
        var agendar = 0
        val adultoMayor = mutableListOf<Boolean>()
        compose.setContent {
            VitaLabTheme {
                OrdenContenido(
                    OrdenUiState(
                        examenes = examenes, resumen = calcularResumen(examenes, adultoMayor = false, domicilio = false),
                        indicaciones = indicacionesPreparacion(examenes), faltanParaPaquete = 2, cargando = false
                    ),
                    onAdultoMayorChange = { adultoMayor += it }, onQuitar = {}, onVerExamenes = {}, onAgendar = { agendar++ }
                )
            }
        }

        compose.onNodeWithText("1 examen seleccionado").assertExists()
        // "Total a pagar" sale dos veces a propósito: en la barra fija de abajo y en la tarjeta de costos.
        compose.onAllNodesWithText("Total a pagar").assertCountEquals(2)
        compose.onNodeWithText("Agrega 2 exámenes más y obtén 10% de descuento por paquete.").assertExists()

        compose.onNodeWithText("Paciente adulto mayor").performClick()
        assertEquals(listOf(true), adultoMayor)

        compose.onNodeWithText("Agendar cita").performClick()
        assertEquals(1, agendar)
    }

    @Test
    fun laOrdenVaciaInvitaAVerExamenes() {
        var verExamenes = 0
        compose.setContent {
            VitaLabTheme {
                OrdenContenido(OrdenUiState(cargando = false), onAdultoMayorChange = {}, onQuitar = {}, onVerExamenes = { verExamenes++ }, onAgendar = {})
            }
        }
        compose.onNodeWithText("Aún no has agregado exámenes").assertExists()
        compose.onNodeWithText("Agendar cita").assertDoesNotExist()
        compose.onNodeWithText("Ver exámenes").performClick()
        assertEquals(1, verExamenes)
    }

    @Test
    fun inicioLlevaALaCitaDeRoomYALasEstadisticas() {
        val cita = citaDeEjemplo()
        val vistas = mutableListOf<Long>()
        var estadisticas = 0
        compose.setContent {
            VitaLabTheme {
                InicioContenido(
                    InicioUiState(proximaCita = cita, totalExamenes = 11, cargando = false),
                    onVerExamenes = {}, onVerOrden = {}, onVerCita = { vistas += it }, onVerEstadisticas = { estadisticas++ }
                )
            }
        }

        compose.onNodeWithText("Tu próxima cita").performClick()
        assertEquals(listOf(cita.cita.id), vistas)

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Estadísticas"))
        compose.onNodeWithText("Estadísticas").performClick()
        assertEquals(1, estadisticas)
    }

    @Test
    fun laBarraInferiorSoloApareceEnLasPantallasRaiz() {
        var ruta by mutableStateOf<String?>(Rutas.EXAMENES)
        compose.setContent { VitaLabTheme { LabScaffold(ruta, cantidadOrden = 3, onPestana = {}) { Text("contenido") } } }

        compose.onNodeWithText("Pacientes").assertExists()

        compose.runOnIdle { ruta = Rutas.EXAMEN_NUEVO }
        compose.onNodeWithText("Pacientes").assertDoesNotExist()

        compose.runOnIdle { ruta = Rutas.CITA_DETALLE }
        compose.onNodeWithText("Pacientes").assertDoesNotExist()

        compose.runOnIdle { ruta = Rutas.PACIENTES }
        compose.onNodeWithText("Pacientes").assertExists()
    }
}
