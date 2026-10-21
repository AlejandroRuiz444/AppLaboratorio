@file:OptIn(ExperimentalMaterial3Api::class)

package com.vitalab.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.vitalab.app.ui.theme.VitaLabTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitalab.app.data.Cita
import com.vitalab.app.data.Examen
import com.vitalab.app.ui.screens.CitaScreen
import com.vitalab.app.ui.screens.ExamenesScreen
import com.vitalab.app.ui.screens.InicioScreen
import com.vitalab.app.ui.screens.OrdenScreen

object Rutas {
    const val INICIO = "inicio"
    const val EXAMENES = "examenes"
    const val ORDEN = "orden"
    const val CITA = "cita"
}

private data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Filled.Home),
    Pestana(Rutas.EXAMENES, "Exámenes", Icons.Filled.Search),
    Pestana(Rutas.ORDEN, "Mi orden", Icons.Filled.ShoppingCart)
)

@Composable
fun LabApp() {
    val navController = rememberNavController()

    // Estado compartido entre pantallas, elevado aquí para que todas lo vean.
    val orden = remember { mutableStateListOf<Examen>() }
    var adultoMayor by remember { mutableStateOf(false) }
    var cita by remember { mutableStateOf<Cita?>(null) }

    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

    fun irA(ruta: String) = navController.navigate(ruta) {
        popUpTo(Rutas.INICIO)
        launchSingleTop = true
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                pestanas.forEach { p ->
                    NavigationBarItem(
                        selected = rutaActual == p.ruta,
                        onClick = { irA(p.ruta) },
                        icon = {
                            if (p.ruta == Rutas.ORDEN && orden.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${orden.size}") } }) { Icon(p.icono, contentDescription = p.titulo) }
                            } else {
                                Icon(p.icono, contentDescription = p.titulo)
                            }
                        },
                        label = { Text(p.titulo) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Rutas.INICIO, modifier = Modifier.padding(padding)) {
            composable(Rutas.INICIO) {
                InicioScreen(
                    cita = cita,
                    examenesEnOrden = orden.size,
                    onVerExamenes = { irA(Rutas.EXAMENES) },
                    onVerOrden = { irA(Rutas.ORDEN) }
                )
            }
            composable(Rutas.EXAMENES) {
                ExamenesScreen(
                    orden = orden,
                    onAgregarQuitar = { examen -> if (examen in orden) orden.remove(examen) else orden.add(examen) },
                    onVerOrden = { irA(Rutas.ORDEN) }
                )
            }
            composable(Rutas.ORDEN) {
                OrdenScreen(
                    orden = orden,
                    adultoMayor = adultoMayor,
                    onAdultoMayorChange = { adultoMayor = it },
                    onQuitar = { orden.remove(it) },
                    onVerExamenes = { irA(Rutas.EXAMENES) },
                    onAgendar = { navController.navigate(Rutas.CITA) }
                )
            }
            composable(Rutas.CITA) {
                CitaScreen(
                    orden = orden,
                    adultoMayor = adultoMayor,
                    onConfirmar = { nueva ->
                        cita = nueva
                        orden.clear()
                        adultoMayor = false
                    },
                    onVolver = { navController.popBackStack() },
                    onIrInicio = { irA(Rutas.INICIO) }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LabAppPreview() {
    VitaLabTheme {
        LabApp()
    }
}
