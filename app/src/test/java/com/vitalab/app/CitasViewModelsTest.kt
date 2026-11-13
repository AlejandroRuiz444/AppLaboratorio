package com.vitalab.app

import androidx.lifecycle.SavedStateHandle
import com.vitalab.app.data.FRANJAS
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.repository.PacienteDestino
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.repository.SolicitudCita
import com.vitalab.app.viewmodel.AccionCita
import com.vitalab.app.viewmodel.AgendarViewModel
import com.vitalab.app.viewmodel.CitaDetalleViewModel
import com.vitalab.app.viewmodel.CitasViewModel
import com.vitalab.app.viewmodel.InicioViewModel
import com.vitalab.app.viewmodel.ModoPaciente
import com.vitalab.app.viewmodel.ReprogramarCitaViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CitasViewModelsTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = FakeLabRepository()

    // Jueves 12 de noviembre de 2026: el domingo 15 no se ofrece, el lunes 16 sí.
    private val hoy = LocalDate.of(2026, 11, 12)
    private val lunes = LocalDate.of(2026, 11, 16)

    private suspend fun pacienteExistente(): Long =
        (repo.guardarPaciente(Paciente(nombre = "Ana Gómez", documento = "1020304050", telefono = "3001234567", correo = "ana@correo.com")) as Resultado.Exito).valor

    private suspend fun crearCita(fecha: LocalDate, hora: String, examenIds: List<Int> = listOf(1, 3)): CitaCompleta {
        val pacienteId = repo.listaPacientes.value.firstOrNull()?.id ?: pacienteExistente()
        repo.ordenIds.value = examenIds
        val r = repo.crearCitaDesdeOrden(SolicitudCita(PacienteDestino.Existente(pacienteId), fecha.toEpochDay(), hora, false, ""))
        return (r as Resultado.Exito).valor
    }

    // --- Agendar ---

    @Test
    fun agendarOfreceSoloDiasHabilesYLimitaFranjasConAyuno() = runTest {
        repo.ordenIds.value = listOf(2) // glucosa en ayunas: 8 h de ayuno
        val vm = AgendarViewModel(repo, hoy = { hoy })
        assertEquals(6, vm.uiState.value.fechasDisponibles.size)
        assertTrue(vm.uiState.value.fechasDisponibles.none { LocalDate.ofEpochDay(it).dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(8, vm.uiState.value.ayunoHoras)
        assertEquals("9:00 a.m.", vm.uiState.value.franjas.last())
    }

    @Test
    fun agendarSinPacientesEmpiezaEnModoNuevoYConPacientesEnExistente() = runTest {
        repo.ordenIds.value = listOf(1)
        val vm = AgendarViewModel(repo, hoy = { hoy })
        assertEquals(ModoPaciente.NUEVO, vm.uiState.value.modoPaciente)

        pacienteExistente()
        assertEquals(ModoPaciente.EXISTENTE, vm.uiState.value.modoPaciente)
        assertEquals(1, vm.uiState.value.pacientes.size)
    }

    @Test
    fun confirmarConFormularioIncompletoMuestraErroresYNoGuarda() = runTest {
        repo.ordenIds.value = listOf(1, 3)
        pacienteExistente()
        val vm = AgendarViewModel(repo, hoy = { hoy })
        assertNull(vm.uiState.value.errorFecha) // aún no intentó enviar

        vm.onDomicilioChange(true)
        vm.onConfirmar()

        val s = vm.uiState.value
        assertEquals("Selecciona un paciente", s.errorPaciente)
        assertNotNull(s.errorFecha)
        assertNotNull(s.errorHora)
        assertNotNull(s.errorDireccion)
        assertNull(s.confirmada)
        assertNull(repo.ultimaSolicitud) // nunca llegó al repositorio
    }

    @Test
    fun confirmarConPacienteNuevoGuardaLaCitaYVaciaLaOrden() = runTest {
        repo.ordenIds.value = listOf(1, 3)
        val vm = AgendarViewModel(repo, hoy = { hoy })
        vm.onNombreChange("Ana Gómez")
        vm.onDocumentoChange("1020304050")
        vm.onTelefonoChange("3001234567")
        vm.onCorreoChange("ana@correo.com")
        vm.onFechaChange(lunes.toEpochDay())
        vm.onHoraChange("7:00 a.m.")
        vm.onDomicilioChange(true)
        vm.onDireccionChange("Calle 1 # 2-3")
        assertEquals(20_000, vm.uiState.value.resumen.recargoDomicilio)

        vm.onConfirmar()

        assertNotNull(vm.uiState.value.confirmada)
        assertTrue(repo.ultimaSolicitud!!.paciente is PacienteDestino.Nuevo)
        assertEquals(1, repo.listaCitas.value.size)
        assertTrue(repo.ordenIds.value.isEmpty())
        assertFalse(vm.uiState.value.ordenVacia) // hay una confirmación que mostrar, no es "orden vacía"
    }

    @Test
    fun errorDelRepositorioSeMuestraSinCerrarElFormulario() = runTest {
        repo.ordenIds.value = listOf(1)
        val id = pacienteExistente()
        repo.resultadoForzado = Resultado.Error("No se pudo guardar")
        val vm = AgendarViewModel(repo, hoy = { hoy })
        vm.onPacienteSeleccionado(id)
        vm.onFechaChange(lunes.toEpochDay())
        vm.onHoraChange(FRANJAS.first())
        vm.onConfirmar()

        assertEquals("No se pudo guardar", vm.uiState.value.errorGeneral)
        assertNull(vm.uiState.value.confirmada)
        assertFalse(vm.uiState.value.enviando)
    }

    // --- Lista y detalle ---

    @Test
    fun listaFiltraPorEstadoYPorTextoDelPaciente() = runTest {
        val a = crearCita(lunes, "7:00 a.m.")
        crearCita(lunes.plusDays(1), "8:00 a.m.")
        repo.cambiarEstadoCita(a.cita.id, EstadoCita.CANCELADA)
        val vm = CitasViewModel(repo)
        mantener(vm.uiState)
        assertEquals(2, vm.uiState.value.citas.size)

        vm.onEstadoFiltroChange(EstadoCita.CANCELADA)
        assertEquals(listOf(a.cita.id), vm.uiState.value.citas.map { it.cita.id })

        vm.onEstadoFiltroChange(null)
        vm.onBusquedaChange("gomez") // el paciente se llama "Gómez"
        assertEquals(2, vm.uiState.value.citas.size)
        vm.onBusquedaChange("nadie")
        assertTrue(vm.uiState.value.citas.isEmpty())
        assertTrue(vm.uiState.value.hayFiltros)
    }

    @Test
    fun detalleCancelaConConfirmacionYBloqueaMasCambios() = runTest {
        val cita = crearCita(lunes, "7:00 a.m.")
        val vm = CitaDetalleViewModel(repo, SavedStateHandle(mapOf("id" to cita.cita.id)))
        mantener(vm.uiState)
        assertTrue(vm.uiState.value.puedeModificar)
        assertEquals(2, vm.uiState.value.lineas.size)

        vm.onPedirAccion(AccionCita.CANCELAR)
        assertEquals(AccionCita.CANCELAR, vm.uiState.value.accionPendiente)
        assertEquals(EstadoCita.PROGRAMADA, repo.listaCitas.value.single().cita.estado) // todavía no cambió

        vm.onConfirmarAccion()
        assertEquals(EstadoCita.CANCELADA, vm.uiState.value.cita!!.cita.estado)
        assertFalse(vm.uiState.value.puedeModificar)
        assertNull(vm.uiState.value.accionPendiente)
    }

    @Test
    fun detalleEliminarMarcaEliminadaYNoApareceComoNoEncontrada() = runTest {
        val cita = crearCita(lunes, "7:00 a.m.")
        val vm = CitaDetalleViewModel(repo, SavedStateHandle(mapOf("id" to cita.cita.id)))
        mantener(vm.uiState)

        vm.onPedirAccion(AccionCita.ELIMINAR)
        vm.onConfirmarAccion()

        assertTrue(vm.uiState.value.eliminada)
        assertFalse(vm.uiState.value.noEncontrada)
        assertTrue(repo.listaCitas.value.isEmpty())
    }

    @Test
    fun detalleDeIdInexistenteEsNoEncontrada() = runTest {
        val vm = CitaDetalleViewModel(repo, SavedStateHandle(mapOf("id" to 999L)))
        mantener(vm.uiState)
        assertTrue(vm.uiState.value.noEncontrada)
    }

    // --- Reprogramar ---

    @Test
    fun reprogramarPrecargaLaCitaYExigeDireccionSiEsDomicilio() = runTest {
        val cita = crearCita(lunes, "7:00 a.m.")
        val vm = ReprogramarCitaViewModel(repo, SavedStateHandle(mapOf("id" to cita.cita.id)), hoy = { hoy })
        assertEquals(lunes.toEpochDay(), vm.uiState.value.fecha)
        assertEquals("7:00 a.m.", vm.uiState.value.hora)

        vm.onDomicilioChange(true)
        assertEquals(20_000, vm.uiState.value.resumen.recargoDomicilio) // el costo de la vista previa cambia
        vm.onGuardar()
        assertNotNull(vm.uiState.value.errorDireccion)
        assertFalse(vm.uiState.value.guardado)

        vm.onDireccionChange("Calle 1 # 2-3")
        vm.onHoraChange("8:00 a.m.")
        vm.onGuardar()
        assertTrue(vm.uiState.value.guardado)
        val guardada = repo.listaCitas.value.single().cita
        assertEquals("8:00 a.m.", guardada.hora)
        assertTrue(guardada.domicilio)
    }

    @Test
    fun reprogramarUnaCitaCanceladaNoSePermite() = runTest {
        val cita = crearCita(lunes, "7:00 a.m.")
        repo.cambiarEstadoCita(cita.cita.id, EstadoCita.CANCELADA)
        val vm = ReprogramarCitaViewModel(repo, SavedStateHandle(mapOf("id" to cita.cita.id)), hoy = { hoy })
        assertTrue(vm.uiState.value.noReprogramable)
    }

    // --- Inicio ---

    @Test
    fun inicioMuestraLaCitaProgramadaMasProxima() = runTest {
        crearCita(lunes.plusDays(1), "7:00 a.m.")
        crearCita(lunes, "3:00 p.m.")
        val temprano = crearCita(lunes, "6:30 a.m.") // mismo día: gana la hora más temprana
        val cancelada = crearCita(hoy.plusDays(1), "6:00 a.m.")
        repo.cambiarEstadoCita(cancelada.cita.id, EstadoCita.CANCELADA)

        val vm = InicioViewModel(repo, hoy = { hoy })
        mantener(vm.uiState)
        assertEquals(temprano.cita.id, vm.uiState.value.proximaCita!!.cita.id)
        assertEquals(11, vm.uiState.value.totalExamenes)
    }
}
