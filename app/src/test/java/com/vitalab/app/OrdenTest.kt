package com.vitalab.app

import com.vitalab.app.data.*
import org.junit.Assert.*
import org.junit.Test

class OrdenTest {
    private fun examen(id: Int) = catalogoExamenes.first { it.id == id }

    @Test
    fun descuentoPaqueteSoloConTresOMasExamenes() {
        assertEquals(0, calcularResumen(catalogoExamenes.take(2), false, false).descuentoPaquete)
        val r = calcularResumen(catalogoExamenes.take(3), false, false)
        assertEquals(r.subtotal / 10, r.descuentoPaquete)
    }

    @Test
    fun descuentosYRecargoSeCombinanEnElTotal() {
        val r = calcularResumen(catalogoExamenes.take(3), adultoMayor = true, domicilio = true)
        assertEquals(r.subtotal - r.subtotal / 10 - r.subtotal * 15 / 100 + RECARGO_DOMICILIO, r.total)
    }

    @Test
    fun seUsaElAyunoMasLargoDeLaOrden() {
        val indicaciones = indicacionesPreparacion(listOf(examen(2), examen(3), examen(8)))
        assertEquals("Ayuno de 12 horas (solo puedes tomar agua).", indicaciones.first())
        assertTrue(indicaciones.contains("Muestras a tomar: Sangre y Orina."))
    }

    @Test
    fun conAyunoSoloHayFranjasDeLaManana() {
        assertEquals("9:00 a.m.", franjasDisponibles(8).last())
        assertEquals(FRANJAS, franjasDisponibles(0))
    }

    @Test
    fun validacionesDelFormulario() {
        assertNull(validarTelefono("3001234567"))
        assertNotNull(validarTelefono("12345"))
        assertNull(validarCorreo("ana@correo.com"))
        assertNotNull(validarCorreo("ana@"))
        assertNotNull(validarDocumento("12a456"))
        assertNull(validarDocumento("1020304050"))
    }

    @Test
    fun formatoDePesos() {
        assertEquals("$12.000", formatoPesos(12_000))
        assertEquals("$1.250.000", formatoPesos(1_250_000))
    }
}
