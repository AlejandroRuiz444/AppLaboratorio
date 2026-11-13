package com.vitalab.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vitalab.app.data.Categoria
import com.vitalab.app.data.Examen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.local.AppDatabase
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.repository.PacienteDestino
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.repository.RoomLabRepository
import com.vitalab.app.data.repository.SolicitudCita
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Room real (SQLite) corriendo en la JVM con Robolectric: prueba DAO, relaciones, FK y reglas del repositorio. */
@RunWith(RobolectricTestRunner::class)
class RoomLabRepositoryTest {
    private val contexto: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomLabRepository

    private val lunes = LocalDate.of(2026, 11, 16).toEpochDay()

    @Before
    fun abrir() {
        db = AppDatabase.crear(contexto, enMemoria = true)
        repo = RoomLabRepository(db)
    }

    @After
    fun cerrar() = db.close()

    // --- Ayudas ---

    private suspend fun nuevoPaciente(documento: String = "1020304050", nombre: String = "Ana Gómez"): Long {
        val r = repo.guardarPaciente(Paciente(nombre = nombre, documento = documento, telefono = "3001234567", correo = "ana@correo.com"))
        return (r as Resultado.Exito).valor
    }

    private suspend fun agendar(
        pacienteId: Long, examenes: List<Int>, domicilio: Boolean = false, adultoMayor: Boolean = false, fecha: Long = lunes
    ): CitaCompleta {
        examenes.forEach { repo.alternarEnOrden(it) }
        repo.setAdultoMayor(adultoMayor)
        val solicitud = SolicitudCita(PacienteDestino.Existente(pacienteId), fecha, "7:00 a.m.", domicilio, if (domicilio) "Calle 1 # 2-3" else "")
        return (repo.crearCitaDesdeOrden(solicitud) as Resultado.Exito).valor
    }

    private fun contarFilas(tabla: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabla").use { it.moveToFirst(); it.getInt(0) }

    // --- Siembra ---

    @Test
    fun laBaseNaceConElCatalogoCompletoYDeterminista() = runBlocking {
        val examenes = repo.todosLosExamenes().first()
        assertEquals(11, examenes.size)
        assertEquals(catalogoExamenes, examenes) // mismos ids 1..11 y mismos datos, sin sorpresas

        // El autoincremento sigue después del 11: el siguiente examen creado por el admin es el 12.
        val id = repo.guardarExamen(Examen(nombre = "Ferritina", categoria = Categoria.QUIMICA, precio = 35_000,
            descripcion = "Reserva de hierro.", muestra = "Sangre", ayunoHoras = 8, horasEntrega = 24))
        assertEquals(12, id)
    }

    @Test
    fun ordenYCatalogoSobrevivenACerrarYReabrirLaBase() = runBlocking {
        // Base en archivo (no en memoria): se cierra y se vuelve a abrir como al reiniciar la app.
        var archivo = AppDatabase.crear(contexto)
        var r = RoomLabRepository(archivo)
        r.alternarEnOrden(3)
        r.alternarEnOrden(8)
        r.setAdultoMayor(true)
        archivo.close()

        archivo = AppDatabase.crear(contexto)
        r = RoomLabRepository(archivo)
        assertEquals(listOf(3, 8), r.orden().first().map { it.id })
        assertTrue(r.adultoMayor().first())
        assertEquals(11, r.todosLosExamenes().first().size) // no se sembró dos veces
        archivo.close()
    }

    // --- Pacientes ---

    @Test
    fun crudDePacienteConDocumentoUnico() = runBlocking {
        val id = nuevoPaciente("1020304050")
        assertEquals("Ana Gómez", repo.obtenerPaciente(id)!!.nombre)

        // Documento repetido: Error claro, no excepción, y no se crea nada.
        val repetido = repo.guardarPaciente(Paciente(nombre = "Otra Persona", documento = "1020304050", telefono = "3000000000", correo = "o@o.com"))
        assertTrue(repetido is Resultado.Error)
        assertTrue((repetido as Resultado.Error).mensaje.contains("1020304050"))
        assertEquals(1, contarFilas("pacientes"))

        // Editar al mismo paciente conservando su documento sí es válido.
        val editado = repo.guardarPaciente(repo.obtenerPaciente(id)!!.copy(nombre = "Ana María Gómez"))
        assertTrue(editado is Resultado.Exito)
        assertEquals("Ana María Gómez", repo.obtenerPaciente(id)!!.nombre)

        // Pero no puede tomar el documento de otro.
        val otro = nuevoPaciente("5566778899", "Luis Mora")
        assertTrue(repo.guardarPaciente(repo.obtenerPaciente(otro)!!.copy(documento = "1020304050")) is Resultado.Error)

        repo.eliminarPaciente(otro)
        assertNull(repo.obtenerPaciente(otro))
        assertEquals(listOf("Ana María Gómez"), repo.pacientes("").first().map { it.paciente.nombre })
    }

    @Test
    fun busquedaDePacientesIgnoraTildesYMayusculas() = runBlocking {
        nuevoPaciente("1020304050", "Ángel Pérez")
        nuevoPaciente("5566778899", "Luis Mora")
        assertEquals(listOf("Ángel Pérez"), repo.pacientes("ANGEL").first().map { it.paciente.nombre })
        assertEquals(listOf("Luis Mora"), repo.pacientes("5566").first().map { it.paciente.nombre })
    }

    @Test
    fun borrarUnPacienteEliminaSusCitasYLineasPeroNoLasDeOtros() = runBlocking {
        val ana = nuevoPaciente("1020304050")
        val luis = nuevoPaciente("5566778899", "Luis Mora")
        val citaAna = agendar(ana, listOf(1, 2))
        val citaLuis = agendar(luis, listOf(3))
        assertEquals(3, contarFilas("cita_examen"))

        repo.eliminarPaciente(ana)

        assertNull(repo.cita(citaAna.cita.id).first())
        assertNotNull(repo.cita(citaLuis.cita.id).first())
        assertEquals(1, contarFilas("citas"))
        assertEquals(1, contarFilas("cita_examen")) // las líneas de Ana se fueron con su cita
    }

    // --- Citas ---

    @Test
    fun crearCitaConDosExamenesGuardaTotalesLineasYVaciaLaOrden() = runBlocking {
        val ana = nuevoPaciente()
        // 25.000 + 48.000 = 73.000; sin paquete (son 2); adulto mayor 15% = 10.950; domicilio +20.000
        val cita = agendar(ana, listOf(1, 3), domicilio = true, adultoMayor = true)

        assertEquals(73_000, cita.cita.subtotal)
        assertEquals(0, cita.cita.descuentoPaquete)
        assertEquals(10_950, cita.cita.descuentoAdultoMayor)
        assertEquals(20_000, cita.cita.recargoDomicilio)
        assertEquals(82_050, cita.cita.total)
        assertEquals(EstadoCita.PROGRAMADA, cita.cita.estado)
        assertTrue(cita.cita.codigo.matches(Regex("VL-\\d{4}")))
        assertEquals("Ana Gómez", cita.paciente.nombre)
        assertEquals(listOf("Cuadro hemático" to 25_000, "Perfil lipídico" to 48_000), cita.detalle.map { it.examen.nombre to it.precio })

        // La orden quedó vacía y el switch en su valor inicial, dentro de la misma transacción.
        assertTrue(repo.orden().first().isEmpty())
        assertFalse(repo.adultoMayor().first())
    }

    @Test
    fun crearCitaConOrdenVaciaOConDocumentoRepetidoDevuelveErrorYNoGuardaNada() = runBlocking {
        val sinOrden = repo.crearCitaDesdeOrden(SolicitudCita(PacienteDestino.Existente(1), lunes, "7:00 a.m.", false, ""))
        assertTrue(sinOrden is Resultado.Error)

        nuevoPaciente("1020304050")
        repo.alternarEnOrden(1)
        val duplicado = repo.crearCitaDesdeOrden(
            SolicitudCita(
                PacienteDestino.Nuevo(Paciente(nombre = "Otra Persona", documento = "1020304050", telefono = "3000000000", correo = "o@o.com")),
                lunes, "7:00 a.m.", false, ""
            )
        )
        assertTrue(duplicado is Resultado.Error)
        assertEquals(0, contarFilas("citas"))
        assertEquals(1, contarFilas("pacientes"))
        assertEquals(listOf(1), repo.orden().first().map { it.id }) // la orden sigue ahí para reintentar
    }

    @Test
    fun crearCitaConPacienteNuevoLoCreaEnLaMismaTransaccion() = runBlocking {
        repo.alternarEnOrden(2)
        val r = repo.crearCitaDesdeOrden(
            SolicitudCita(
                PacienteDestino.Nuevo(Paciente(nombre = "Marta Ruiz", documento = "9988776655", telefono = "3001112233", correo = "m@m.com")),
                lunes, "6:00 a.m.", false, ""
            )
        )
        assertTrue(r is Resultado.Exito)
        assertEquals("Marta Ruiz", (r as Resultado.Exito).valor.paciente.nombre)
        assertEquals(1, contarFilas("pacientes"))
    }

    @Test
    fun borradoLogicoDeUnExamenNoRompeUnaCitaVieja() = runBlocking {
        val cita = agendar(nuevoPaciente(), listOf(1, 2))

        repo.cambiarActivoExamen(1, activo = false)

        assertEquals(10, repo.examenesActivos().first().size)
        assertEquals(11, repo.todosLosExamenes().first().size)
        val abierta = repo.cita(cita.cita.id).first()!!
        assertEquals(setOf(1, 2), abierta.detalle.map { it.examen.id }.toSet()) // el examen desactivado sigue en la cita
        assertEquals(25_000, abierta.detalle.first { it.examen.id == 1 }.precio)

        // Desactivado no se puede volver a pedir, y reactivarlo lo devuelve al catálogo.
        repo.alternarEnOrden(1)
        assertTrue(repo.orden().first().isEmpty())
        repo.cambiarActivoExamen(1, activo = true)
        assertEquals(11, repo.examenesActivos().first().size)
    }

    @Test
    fun desactivarUnExamenLoSacaDeLaOrdenEnCurso() = runBlocking {
        repo.alternarEnOrden(2)
        repo.alternarEnOrden(3)
        repo.cambiarActivoExamen(2, activo = false)
        assertEquals(listOf(3), repo.orden().first().map { it.id })
    }

    @Test
    fun cambiarElPrecioDelCatalogoNoAlteraLasCitasYaAgendadas() = runBlocking {
        val cita = agendar(nuevoPaciente(), listOf(1))

        repo.guardarExamen(repo.obtenerExamen(1)!!.copy(precio = 99_999))

        val abierta = repo.cita(cita.cita.id).first()!!
        assertEquals(25_000, abierta.detalle.single().precio) // precio congelado en la línea
        assertEquals(25_000, abierta.cita.total)
        assertEquals(99_999, repo.obtenerExamen(1)!!.precio)
    }

    @Test
    fun reprogramarRecalculaElRecargoYSoloAplicaAProgramadas() = runBlocking {
        val cita = agendar(nuevoPaciente(), listOf(1, 3)) // sede: 73.000
        val id = cita.cita.id

        val aDomicilio = repo.reprogramarCita(id, lunes + 1, "8:00 a.m.", domicilio = true, direccion = " Calle 9 # 8-7 ")
        assertTrue(aDomicilio is Resultado.Exito)
        var actual = repo.cita(id).first()!!.cita
        assertEquals(lunes + 1, actual.fecha)
        assertEquals("8:00 a.m.", actual.hora)
        assertEquals("Calle 9 # 8-7", actual.direccion)
        assertEquals(93_000, actual.total)

        repo.reprogramarCita(id, lunes + 1, "8:00 a.m.", domicilio = false, direccion = "")
        actual = repo.cita(id).first()!!.cita
        assertEquals(73_000, actual.total)
        assertEquals("", actual.direccion)

        assertTrue(repo.cambiarEstadoCita(id, EstadoCita.COMPLETADA) is Resultado.Exito)
        assertTrue(repo.reprogramarCita(id, lunes + 2, "9:00 a.m.", false, "") is Resultado.Error)
        assertTrue(repo.cambiarEstadoCita(id, EstadoCita.CANCELADA) is Resultado.Error) // ya no está programada
        assertEquals(EstadoCita.COMPLETADA, repo.cita(id).first()!!.cita.estado)
    }

    @Test
    fun listaDeCitasFiltraBuscaYOrdenaProgramadasPrimero() = runBlocking {
        val ana = nuevoPaciente("1020304050", "Ángel Pérez")
        val luis = nuevoPaciente("5566778899", "Luis Mora")
        val lejana = agendar(ana, listOf(1), fecha = lunes + 4)
        val cercana = agendar(luis, listOf(2), fecha = lunes)
        val vieja = agendar(ana, listOf(3), fecha = lunes - 6)
        repo.cambiarEstadoCita(vieja.cita.id, EstadoCita.COMPLETADA)

        // Programadas por fecha ascendente, luego el historial.
        assertEquals(listOf(cercana, lejana, vieja).map { it.cita.id }, repo.citas(null, "").first().map { it.cita.id })
        assertEquals(listOf(vieja.cita.id), repo.citas(EstadoCita.COMPLETADA, "").first().map { it.cita.id })
        assertEquals(listOf(lejana, vieja).map { it.cita.id }.toSet(), repo.citas(null, "angel").first().map { it.cita.id }.toSet())
        assertEquals(listOf(cercana.cita.id), repo.citas(null, cercana.cita.codigo.lowercase()).first().map { it.cita.id })
        assertEquals(lunes, repo.citasProgramadasDesde(lunes).first().first().cita.fecha)
    }

    @Test
    fun eliminarUnaCitaBorraSusLineas() = runBlocking {
        val cita = agendar(nuevoPaciente(), listOf(1, 2))
        repo.eliminarCita(cita.cita.id)
        assertEquals(0, contarFilas("citas"))
        assertEquals(0, contarFilas("cita_examen"))
        assertEquals(1, contarFilas("pacientes"))
    }

    // --- Estadísticas ---

    @Test
    fun estadisticasSonCoherentesConLasCitasGuardadas() = runBlocking {
        val ana = nuevoPaciente("1020304050", "Ana Gómez")
        val luis = nuevoPaciente("5566778899", "Luis Mora")
        val completada = agendar(ana, listOf(1, 2))                                      // 37.000, sin descuentos
        agendar(ana, listOf(1, 3, 4), adultoMayor = true)                               // 118.000 - 11.800 - 17.700 = 88.500
        val cancelada = agendar(luis, listOf(2), domicilio = true)                       // 12.000 + 20.000
        repo.cambiarEstadoCita(completada.cita.id, EstadoCita.COMPLETADA)
        repo.cambiarEstadoCita(cancelada.cita.id, EstadoCita.CANCELADA)

        val e = repo.estadisticas().first()

        assertEquals(mapOf(EstadoCita.PROGRAMADA to 1, EstadoCita.COMPLETADA to 1, EstadoCita.CANCELADA to 1), e.citasPorEstado)
        assertEquals(3, e.totalCitas)
        assertEquals(37_000, e.ingresos)                    // solo lo cobrado en completadas
        assertEquals(29_500, e.ahorro)                      // descuentos de la programada; la cancelada no cuenta
        assertEquals("Cuadro hemático", e.topExamenes.first().nombre)
        assertEquals(2, e.topExamenes.first().veces)
        assertEquals(listOf("Ana Gómez" to 2, "Luis Mora" to 1), e.citasPorPaciente.map { it.nombre to it.total })
        assertEquals(e.totalCitas, e.citasPorPaciente.sumOf { it.total }) // las dos vistas cuentan las mismas citas
    }

    @Test
    fun estadisticasSinDatosSonCeroYNoFallan() = runBlocking {
        val e = repo.estadisticas().first()
        assertEquals(0, e.totalCitas)
        assertEquals(0, e.ingresos)
        assertEquals(0, e.ahorro)
        assertTrue(e.topExamenes.isEmpty())
        assertTrue(e.citasPorPaciente.isEmpty())
    }
}
