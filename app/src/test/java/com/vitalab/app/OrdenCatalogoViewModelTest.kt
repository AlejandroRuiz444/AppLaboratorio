package com.vitalab.app

import com.vitalab.app.data.Categoria
import com.vitalab.app.viewmodel.CatalogoViewModel
import com.vitalab.app.viewmodel.OrdenViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdenCatalogoViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = FakeLabRepository()

    @Test
    fun ordenRecalculaCostosAlCambiarExamenesYSwitch() = runTest {
        val vm = OrdenViewModel(repo)
        mantener(vm.uiState)

        repo.alternarEnOrden(1) // 25.000
        repo.alternarEnOrden(3) // 48.000
        assertEquals(2, vm.uiState.value.cantidad)
        assertEquals(1, vm.uiState.value.faltanParaPaquete)
        assertEquals(73_000, vm.uiState.value.resumen.subtotal)

        vm.onAdultoMayorChange(true)
        assertEquals(73_000 * 15 / 100, vm.uiState.value.resumen.descuentoAdultoMayor)

        vm.onQuitar(1)
        assertEquals(1, vm.uiState.value.cantidad)
        assertEquals(48_000, vm.uiState.value.resumen.subtotal)
    }

    @Test
    fun catalogoFiltraPorCategoriaYBusquedaSinTildes() = runTest {
        val vm = CatalogoViewModel(repo)
        mantener(vm.uiState)
        assertEquals(11, vm.uiState.value.examenes.size)

        vm.onCategoriaChange(Categoria.QUIMICA)
        assertEquals(listOf(2, 3, 4, 5), vm.uiState.value.examenes.map { it.id })

        vm.onBusquedaChange("LIPIDICO") // sin tilde y en mayúsculas
        assertEquals(listOf(3), vm.uiState.value.examenes.map { it.id })

        vm.onLimpiarFiltros()
        assertEquals(11, vm.uiState.value.examenes.size)
    }

    @Test
    fun examenDesactivadoSoloSeVeEnModoAdminYSaleDeLaOrden() = runTest {
        val vm = CatalogoViewModel(repo)
        mantener(vm.uiState)
        vm.onAlternarOrden(2)
        assertEquals(setOf(2), vm.uiState.value.idsEnOrden)

        vm.onCambiarActivo(vm.uiState.value.examenes.first { it.id == 2 })
        assertFalse(vm.uiState.value.examenes.any { it.id == 2 })
        assertEquals(10, vm.uiState.value.totalActivos)
        assertTrue(vm.uiState.value.idsEnOrden.isEmpty())

        vm.onModoAdminChange(true)
        assertEquals(11, vm.uiState.value.examenes.size)
        assertFalse(vm.uiState.value.examenes.first { it.id == 2 }.activo)
    }
}
