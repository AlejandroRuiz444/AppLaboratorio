@file:OptIn(ExperimentalMaterial3Api::class)

package com.vitalab.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.ui.components.conteo
import com.vitalab.app.ui.labViewModel
import com.vitalab.app.ui.screens.ExamenFormScreen
import com.vitalab.app.ui.screens.ExamenesScreen
import com.vitalab.app.ui.screens.InicioContenido
import com.vitalab.app.ui.screens.InicioScreen
import com.vitalab.app.ui.screens.OrdenScreen
import com.vitalab.app.ui.screens.citaDeEjemplo
import com.vitalab.app.ui.screens.citas.AgendarCitaScreen
import com.vitalab.app.ui.screens.citas.CitaDetalleScreen
import com.vitalab.app.ui.screens.citas.CitasScreen
import com.vitalab.app.ui.screens.citas.ReprogramarCitaScreen
import com.vitalab.app.ui.screens.estadisticas.EstadisticasScreen
import com.vitalab.app.ui.screens.pacientes.PacienteFormScreen
import com.vitalab.app.ui.screens.pacientes.PacientesScreen
import com.vitalab.app.ui.theme.VitaLabTheme
import com.vitalab.app.viewmodel.InicioUiState
import com.vitalab.app.viewmodel.OrdenViewModel

private data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Filled.Home),
    Pestana(Rutas.EXAMENES, "Exámenes", Icons.Filled.Search),
    Pestana(Rutas.ORDEN, "Mi orden", Icons.Filled.ShoppingCart),
    Pestana(Rutas.CITAS, "Citas", Icons.Filled.DateRange),
    Pestana(Rutas.PACIENTES, "Pacientes", Icons.Filled.Person)
)

/**
 * La barra inferior solo se muestra en las 5 pantallas raíz. Formularios, detalle, reprogramar y estadísticas
 * la ocultan: ganan alto (teclado, botón Guardar fijo), no se puede cambiar de pestaña con un formulario a medias
 * y Atrás (flecha o gesto) siempre devuelve a la pantalla raíz de donde se vino.
 */
private val rutasRaiz = pestanas.map { it.ruta }.toSet()

/**
 * Raíz de la app: barra de pestañas + NavHost con todas las rutas.
 *
 * Reglas de pila (para que Atrás se comporte):
 * - Cada pestaña abre su pantalla raíz: `popUpTo(INICIO)` + `launchSingleTop`. Atrás desde una pestaña vuelve a Inicio y
 *   desde Inicio sale de la app. No se usa `saveState/restoreState`: restauraría pilas viejas (p. ej. un formulario de cita ya confirmado).
 * - Formularios y detalles se apilan con `navigate` (singleTop, contra el doble toque) y se cierran con `popBackStack`.
 * - Tras agendar, el detalle de la cita reemplaza a la orden y al formulario: la pila queda Inicio > Citas > Detalle.
 */
@Composable
fun LabApp() {
    val navController = rememberNavController()

    // Fuera del NavHost, atado a la Activity: lo usan la insignia de la barra y la pantalla "Mi orden".
    val ordenVm: OrdenViewModel = labViewModel()
    val orden by ordenVm.uiState.collectAsStateWithLifecycle()

    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

    fun irAPestana(ruta: String) = navController.navigate(ruta) {
        popUpTo(Rutas.INICIO)
        launchSingleTop = true
    }
    fun abrir(ruta: String) = navController.navigate(ruta) { launchSingleTop = true }
    val volver: () -> Unit = { navController.popBackStack() }

    LabScaffold(rutaActual, cantidadOrden = orden.cantidad, onPestana = ::irAPestana) { padding ->
        NavHost(
            navController,
            startDestination = Rutas.INICIO,
            // `consumeWindowInsets`: las pantallas hijas (con su propio Scaffold o `imePadding`) no repiten el margen de las barras del sistema.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            // Cruce rápido (Operate): el de fábrica dura 700 ms.
            enterTransition = { fadeIn(tween(210, delayMillis = 60)) },
            exitTransition = { fadeOut(tween(60)) },
            popEnterTransition = { fadeIn(tween(210, delayMillis = 60)) },
            popExitTransition = { fadeOut(tween(60)) }
        ) {
            composable(Rutas.INICIO) {
                InicioScreen(
                    viewModel = labViewModel(),
                    onVerExamenes = { irAPestana(Rutas.EXAMENES) },
                    onVerOrden = { irAPestana(Rutas.ORDEN) },
                    onVerCita = { abrir(Rutas.citaDetalle(it)) },
                    onVerEstadisticas = { abrir(Rutas.ESTADISTICAS) }
                )
            }
            composable(Rutas.EXAMENES) {
                ExamenesScreen(
                    viewModel = labViewModel(),
                    onVerOrden = { irAPestana(Rutas.ORDEN) },
                    onNuevoExamen = { abrir(Rutas.EXAMEN_NUEVO) },
                    onEditarExamen = { abrir(Rutas.examenEditar(it)) }
                )
            }
            composable(Rutas.EXAMEN_NUEVO) { ExamenFormScreen(viewModel = labViewModel(), onVolver = volver) }
            composable(Rutas.EXAMEN_EDITAR, arguments = Rutas.argId) { ExamenFormScreen(viewModel = labViewModel(), onVolver = volver) }

            composable(Rutas.ORDEN) {
                OrdenScreen(viewModel = ordenVm, onVerExamenes = { irAPestana(Rutas.EXAMENES) }, onAgendar = { abrir(Rutas.CITA_NUEVA) })
            }

            composable(Rutas.CITAS) {
                CitasScreen(
                    viewModel = labViewModel(),
                    onVerCita = { abrir(Rutas.citaDetalle(it)) },
                    onAgendar = { abrir(Rutas.CITA_NUEVA) },
                    onVerEstadisticas = { abrir(Rutas.ESTADISTICAS) }
                )
            }
            composable(Rutas.CITA_NUEVA) {
                AgendarCitaScreen(
                    viewModel = labViewModel(),
                    onVolver = volver,
                    onIrInicio = { irAPestana(Rutas.INICIO) },
                    onVerExamenes = { irAPestana(Rutas.EXAMENES) },
                    onVerCita = { id ->
                        // Se limpia lo que hubiera encima de Inicio (orden, formulario ya confirmado), se pone Citas
                        // como pantalla intermedia y encima el detalle: Atrás desde el detalle lleva a la lista de citas.
                        irAPestana(Rutas.CITAS)
                        navController.navigate(Rutas.citaDetalle(id))
                    }
                )
            }
            composable(Rutas.CITA_DETALLE, arguments = Rutas.argId) {
                CitaDetalleScreen(
                    viewModel = labViewModel(),
                    onVolver = volver,
                    onReprogramar = { abrir(Rutas.citaReprogramar(it)) }
                )
            }
            composable(Rutas.CITA_REPROGRAMAR, arguments = Rutas.argId) {
                ReprogramarCitaScreen(viewModel = labViewModel(), onVolver = volver)
            }

            composable(Rutas.PACIENTES) {
                PacientesScreen(
                    viewModel = labViewModel(),
                    onNuevo = { abrir(Rutas.PACIENTE_NUEVO) },
                    onEditar = { abrir(Rutas.pacienteEditar(it)) }
                )
            }
            composable(Rutas.PACIENTE_NUEVO) { PacienteFormScreen(viewModel = labViewModel(), onVolver = volver) }
            composable(Rutas.PACIENTE_EDITAR, arguments = Rutas.argId) { PacienteFormScreen(viewModel = labViewModel(), onVolver = volver) }

            composable(Rutas.ESTADISTICAS) { EstadisticasScreen(viewModel = labViewModel(), onVolver = volver) }
        }
    }
}

/** Marco de la app sin navegación real (por eso se puede previsualizar): barra de pestañas condicionada a la ruta y contenido. */
@Composable
internal fun LabScaffold(
    rutaActual: String?,
    cantidadOrden: Int,
    onPestana: (String) -> Unit,
    contenido: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            // `null` = el NavHost aún no emitió su primera entrada: se muestra la barra para que no aparezca de golpe.
            if (rutaActual == null || rutaActual in rutasRaiz) {
                NavigationBar {
                    pestanas.forEach { p ->
                        val conInsignia = p.ruta == Rutas.ORDEN && cantidadOrden > 0
                        NavigationBarItem(
                            selected = rutaActual == p.ruta,
                            onClick = { onPestana(p.ruta) },
                            // El número de la insignia y el ícono no entran en la semántica fusionada del ítem (comprobado con una
                            // prueba de Compose): la cantidad va en la descripción del propio ítem para que el lector de pantalla la diga.
                            modifier = if (conInsignia) {
                                Modifier.semantics { contentDescription = "${p.titulo}, ${conteo(cantidadOrden, "examen", "exámenes")}" }
                            } else Modifier,
                            icon = {
                                if (conInsignia) {
                                    BadgedBox(badge = { Badge { Text("$cantidadOrden") } }) { Icon(p.icono, contentDescription = null) }
                                } else {
                                    Icon(p.icono, contentDescription = null)
                                }
                            },
                            label = { Text(p.titulo) }
                        )
                    }
                }
            }
        },
        content = contenido
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LabAppPreview() {
    VitaLabTheme {
        LabScaffold(rutaActual = Rutas.INICIO, cantidadOrden = 3, onPestana = {}) { padding ->
            Box(Modifier.padding(padding)) {
                InicioContenido(
                    InicioUiState(
                        proximaCita = citaDeEjemplo(), examenesEnOrden = 3, totalExamenes = 11,
                        destacados = catalogoExamenes.take(4), cargando = false
                    ),
                    onVerExamenes = {}, onVerOrden = {}, onVerCita = {}, onVerEstadisticas = {}
                )
            }
        }
    }
}
