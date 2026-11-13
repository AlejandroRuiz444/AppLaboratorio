package com.vitalab.app

import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.calcularResumenDePrecios
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.coincideBusqueda
import com.vitalab.app.data.validarHorasAyuno
import com.vitalab.app.data.validarPrecio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lógica pura nueva del segundo corte (la del primer corte sigue en OrdenTest). */
class LogicaPuraTest {

    @Test
    fun resumenPorPreciosEsIgualAlResumenPorExamenes() {
        val examenes = catalogoExamenes.take(4)
        assertEquals(
            calcularResumen(examenes, adultoMayor = true, domicilio = true),
            calcularResumenDePrecios(examenes.map { it.precio }, adultoMayor = true, domicilio = true)
        )
    }

    @Test
    fun busquedaIgnoraMayusculasYTildes() {
        assertTrue(coincideBusqueda("Ángel Pérez", "angel"))
        assertTrue(coincideBusqueda("Ángel Pérez", "PEREZ"))
        assertTrue(coincideBusqueda("Ángel Pérez", ""))
        assertFalse(coincideBusqueda("Ángel Pérez", "gomez"))
    }

    @Test
    fun validacionesDelFormularioDeExamen() {
        assertNull(validarPrecio("25000"))
        assertNotNull(validarPrecio("0"))
        assertNotNull(validarPrecio("abc"))
        assertNull(validarHorasAyuno("12"))
        assertNotNull(validarHorasAyuno("30"))
    }
}
