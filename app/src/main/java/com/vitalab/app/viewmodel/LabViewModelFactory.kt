package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.vitalab.app.AppContainer

/**
 * Fábrica única de ViewModels. Los de detalle/edición reciben un `SavedStateHandle` que ya trae
 * los argumentos de la ruta (`id`) cuando el ViewModel se pide dentro de un `composable(...)` del NavHost.
 */
class LabViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val repo = container.repositorio
        return when (modelClass) {
            InicioViewModel::class.java -> InicioViewModel(repo)
            CatalogoViewModel::class.java -> CatalogoViewModel(repo)
            ExamenFormViewModel::class.java -> ExamenFormViewModel(repo, extras.createSavedStateHandle())
            OrdenViewModel::class.java -> OrdenViewModel(repo)
            AgendarViewModel::class.java -> AgendarViewModel(repo)
            CitasViewModel::class.java -> CitasViewModel(repo)
            CitaDetalleViewModel::class.java -> CitaDetalleViewModel(repo, extras.createSavedStateHandle())
            ReprogramarCitaViewModel::class.java -> ReprogramarCitaViewModel(repo, extras.createSavedStateHandle())
            PacientesViewModel::class.java -> PacientesViewModel(repo)
            PacienteFormViewModel::class.java -> PacienteFormViewModel(repo, extras.createSavedStateHandle())
            EstadisticasViewModel::class.java -> EstadisticasViewModel(repo)
            else -> throw IllegalArgumentException("ViewModel no registrado en LabViewModelFactory: ${modelClass.name}")
        } as T
    }
}
