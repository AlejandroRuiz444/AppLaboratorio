package com.vitalab.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Cambia Dispatchers.Main por uno de prueba para que `viewModelScope` corra en la JVM y sin esperas. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Los StateFlow de los ViewModels usan `WhileSubscribed`: no calculan nada hasta que alguien los colecta.
 * Esto simula a la pantalla suscrita durante toda la prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.mantener(flow: StateFlow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
}
