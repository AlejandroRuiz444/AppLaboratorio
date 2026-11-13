package com.vitalab.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalab.app.VitaLabApp

/**
 * Obtiene un ViewModel con la fábrica de la app. Dentro de un `composable(ruta)` del NavHost queda atado a esa
 * entrada de la pila (y recibe los argumentos de la ruta); fuera del NavHost queda atado a la Activity.
 *
 * Uso: `val vm = labViewModel<CitasViewModel>()`
 */
@Composable
inline fun <reified VM : ViewModel> labViewModel(): VM {
    val app = LocalContext.current.applicationContext as VitaLabApp
    return viewModel(factory = app.container.viewModelFactory)
}
