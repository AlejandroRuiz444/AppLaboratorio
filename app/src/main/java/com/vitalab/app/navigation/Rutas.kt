package com.vitalab.app.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Rutas de la app. Los ids viajan en el argumento `id` (siempre Long) y los ViewModels de
 * detalle/edición lo leen de su `SavedStateHandle`, así que las pantallas no reciben el id por parámetro.
 *
 * En el NavHost: `composable(Rutas.CITA_DETALLE, arguments = Rutas.argId) { ... }`
 * Al navegar:    `navController.navigate(Rutas.citaDetalle(id))`
 */
object Rutas {
    const val ARG_ID = "id"

    const val INICIO = "inicio"
    const val EXAMENES = "examenes"
    const val EXAMEN_NUEVO = "examen/nuevo"
    const val EXAMEN_EDITAR = "examen/{$ARG_ID}/editar"
    const val ORDEN = "orden"
    const val CITAS = "citas"
    const val CITA_NUEVA = "cita/nueva"
    const val CITA_DETALLE = "cita/{$ARG_ID}"
    const val CITA_REPROGRAMAR = "cita/{$ARG_ID}/reprogramar"
    const val PACIENTES = "pacientes"
    const val PACIENTE_NUEVO = "paciente/nuevo"
    const val PACIENTE_EDITAR = "paciente/{$ARG_ID}/editar"
    const val ESTADISTICAS = "estadisticas"

    /** Argumentos de las rutas que llevan `{id}`. */
    val argId = listOf(navArgument(ARG_ID) { type = NavType.LongType })

    fun examenEditar(id: Int) = "examen/$id/editar"
    fun citaDetalle(id: Long) = "cita/$id"
    fun citaReprogramar(id: Long) = "cita/$id/reprogramar"
    fun pacienteEditar(id: Long) = "paciente/$id/editar"
}
