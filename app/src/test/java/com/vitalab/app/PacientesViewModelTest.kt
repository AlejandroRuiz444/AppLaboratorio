package com.vitalab.app

import androidx.lifecycle.SavedStateHandle
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.viewmodel.PacienteFormViewModel
import com.vitalab.app.viewmodel.PacientesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PacientesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = FakeLabRepository()

    private fun PacienteFormViewModel.llenar(documento: String = "1020304050") {
        onNombreChange("Ana Gómez")
        onDocumentoChange(documento)
        onTelefonoChange("3001234567")
        onCorreoChange("ana@correo.com")
    }

    @Test
    fun formularioNoMuestraErroresHastaTocarloOIntentarGuardar() = runTest {
        val vm = PacienteFormViewModel(repo, SavedStateHandle())
        assertNull(vm.uiState.value.errorNombre)

        vm.onNombreChange("ab") // ya escribió algo inválido
        assertNotNull(vm.uiState.value.errorNombre)
        assertNull(vm.uiState.value.errorDocumento)

        vm.onGuardar()
        assertTrue(vm.uiState.value.hayErrores)
        assertNotNull(vm.uiState.value.errorDocumento)
        assertFalse(vm.uiState.value.guardado)
        assertTrue(repo.listaPacientes.value.isEmpty())
    }

    @Test
    fun guardaPacienteValido() = runTest {
        val vm = PacienteFormViewModel(repo, SavedStateHandle())
        vm.llenar()
        vm.onGuardar()
        assertTrue(vm.uiState.value.guardado)
        assertEquals("Ana Gómez", repo.listaPacientes.value.single().nombre)
    }

    @Test
    fun documentoRepetidoMuestraElErrorEnElCampoYSeQuitaAlCambiarlo() = runTest {
        repo.guardarPaciente(Paciente(nombre = "Otra Persona", documento = "1020304050", telefono = "3000000000", correo = "o@o.com"))
        val vm = PacienteFormViewModel(repo, SavedStateHandle())
        vm.llenar("1020304050")
        vm.onGuardar()

        assertFalse(vm.uiState.value.guardado)
        assertTrue(vm.uiState.value.errorDocumento!!.contains("Ya existe"))

        vm.onDocumentoChange("1020304051")
        assertNull(vm.uiState.value.errorDocumento)
    }

    @Test
    fun editarCargaLosDatosDelIdDeLaRuta() = runTest {
        val id = (repo.guardarPaciente(Paciente(nombre = "Luis Mora", documento = "5566778899", telefono = "3111111111", correo = "l@l.com")) as com.vitalab.app.data.repository.Resultado.Exito).valor
        val vm = PacienteFormViewModel(repo, SavedStateHandle(mapOf("id" to id)))
        assertTrue(vm.uiState.value.esEdicion)
        assertEquals("Luis Mora", vm.uiState.value.nombre)

        vm.onNombreChange("Luis Mora Díaz")
        vm.onGuardar() // el mismo documento del propio paciente NO cuenta como repetido
        assertTrue(vm.uiState.value.guardado)
        assertEquals("Luis Mora Díaz", repo.listaPacientes.value.single().nombre)
    }

    @Test
    fun eliminarPideConfirmacionAntesDeBorrar() = runTest {
        repo.guardarPaciente(Paciente(nombre = "Luis Mora", documento = "5566778899", telefono = "3111111111", correo = "l@l.com"))
        val vm = PacientesViewModel(repo)
        mantener(vm.uiState)
        val paciente = vm.uiState.value.pacientes.single()

        vm.onPedirEliminar(paciente)
        assertEquals(paciente, vm.uiState.value.pacientePorEliminar)
        assertEquals(1, repo.listaPacientes.value.size) // todavía no se borró

        vm.onConfirmarEliminar()
        assertTrue(repo.listaPacientes.value.isEmpty())
        assertNull(vm.uiState.value.pacientePorEliminar)
    }
}
