package com.vitalab.app

import android.content.Context
import com.vitalab.app.data.local.AppDatabase
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.RoomLabRepository
import com.vitalab.app.viewmodel.LabViewModelFactory

/** Inyección de dependencias manual: crea una sola vez la base de datos, el repositorio y la fábrica de ViewModels. */
class AppContainer(context: Context) {
    private val database = AppDatabase.crear(context)
    val repositorio: LabRepository = RoomLabRepository(database)
    val viewModelFactory = LabViewModelFactory(this)
}
