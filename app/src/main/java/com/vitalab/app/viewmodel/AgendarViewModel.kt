package com.vitalab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Examen
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.franjasDisponibles
import com.vitalab.app.data.indicacionesPreparacion
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.proximasFechas
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.PacienteDestino
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.repository.SolicitudCita
import com.vitalab.app.data.validarCorreo
import com.vitalab.app.data.validarDireccion
import com.vitalab.app.data.validarDocumento
import com.vitalab.app.data.validarNombre
import com.vitalab.app.data.validarTelefono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ModoPaciente { EXISTENTE, NUEVO }

data class AgendarUiState(
    val cargando: Boolean = true,

    // La orden en curso (se lee de Room)
    val examenes: List<Examen> = emptyList(),
    val adultoMayor: Boolean = false,
    /** Costos con la modalidad elegida (el domicilio suma recargo). */
    val resumen: Resumen = Resumen(0, 0, 0, 0),
    val indicaciones: List<String> = emptyList(),
    val ayunoHoras: Int = 0,

    // Paciente: uno existente de Room, o uno nuevo que se crea al confirmar
    /** Si el usuario no eligió, es NUEVO cuando aún no hay pacientes y EXISTENTE en otro caso. */
    val modoPaciente: ModoPaciente = ModoPaciente.EXISTENTE,
    val pacientes: List<Paciente> = emptyList(),
    val pacienteId: Long? = null,
    val errorPaciente: String? = null,
    val nombre: String = "",
    val documento: String = "",
    val telefono: String = "",
    val correo: String = "",
    val errorNombre: String? = null,
    val errorDocumento: String? = null,
    val errorTelefono: String? = null,
    val errorCorreo: String? = null,

    // Fecha, hora y modalidad
    /** Días ofrecidos (sin domingos), como `LocalDate.toEpochDay()`. */
    val fechasDisponibles: List<Long> = emptyList(),
    /** Si algún examen exige ayuno, solo hasta las 9:00 a.m. */
    val franjas: List<String> = emptyList(),
    val fecha: Long? = null,
    val hora: String? = null,
    val domicilio: Boolean = false,
    val direccion: String = "",
    val errorFecha: String? = null,
    val errorHora: String? = null,
    val errorDireccion: String? = null,

    val intentado: Boolean = false,
    val enviando: Boolean = false,
    /** Error de la operación (p. ej. documento ya registrado). Se muestra junto al botón Confirmar. */
    val errorGeneral: String? = null,
    /** Cita guardada: la pantalla muestra la confirmación con estos datos. La orden ya quedó vacía. */
    val confirmada: CitaCompleta? = null
) {
    /** No hay nada que agendar (y tampoco se acaba de confirmar una cita). */
    val ordenVacia: Boolean get() = !cargando && examenes.isEmpty() && confirmada == null
}

/** Agendar la cita de la orden en curso: paciente + fecha + hora + modalidad. Guarda en Room al confirmar. */
class AgendarViewModel(
    private val repo: LabRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private var modoElegido: ModoPaciente? = null

    private val _estado = MutableStateFlow(
        AgendarUiState(fechasDisponibles = proximasFechas(hoy()).map { it.toEpochDay() }, franjas = franjasDisponibles(0))
    )
    val uiState: StateFlow<AgendarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.orden(), repo.adultoMayor(), repo.pacientes("")) { orden, adultoMayor, pacientes ->
                Triple(orden, adultoMayor, pacientes.map { it.paciente })
            }.collect { (orden, adultoMayor, pacientes) ->
                _estado.update { recalcular(it.copy(cargando = false, examenes = orden, adultoMayor = adultoMayor, pacientes = pacientes)) }
            }
        }
    }

    fun onModoPacienteChange(modo: ModoPaciente) {
        modoElegido = modo
        cambiar { it }
    }

    fun onPacienteSeleccionado(id: Long) = cambiar { it.copy(pacienteId = id) }
    fun onNombreChange(v: String) = cambiar { it.copy(nombre = v) }
    fun onDocumentoChange(v: String) = cambiar { it.copy(documento = v, errorGeneral = null) }
    fun onTelefonoChange(v: String) = cambiar { it.copy(telefono = v) }
    fun onCorreoChange(v: String) = cambiar { it.copy(correo = v) }
    fun onFechaChange(epochDay: Long) = cambiar { it.copy(fecha = epochDay) }
    fun onHoraChange(hora: String) = cambiar { it.copy(hora = hora) }
    fun onDomicilioChange(valor: Boolean) = cambiar { it.copy(domicilio = valor) }
    fun onDireccionChange(texto: String) = cambiar { it.copy(direccion = texto) }

    fun onConfirmar() {
        cambiar { it.copy(intentado = true, errorGeneral = null) }
        val s = _estado.value
        if (s.cargando || s.enviando || s.confirmada != null || !esValido(s)) return
        _estado.update { it.copy(enviando = true) }
        viewModelScope.launch {
            val destino = if (s.modoPaciente == ModoPaciente.NUEVO) {
                PacienteDestino.Nuevo(
                    Paciente(nombre = s.nombre.trim(), documento = s.documento.trim(), telefono = s.telefono.trim(), correo = s.correo.trim())
                )
            } else {
                PacienteDestino.Existente(s.pacienteId!!)
            }
            val r = repo.crearCitaDesdeOrden(SolicitudCita(destino, s.fecha!!, s.hora!!, s.domicilio, s.direccion))
            _estado.update {
                when (r) {
                    is Resultado.Exito -> it.copy(enviando = false, confirmada = r.valor)
                    is Resultado.Error -> it.copy(enviando = false, errorGeneral = r.mensaje)
                }
            }
        }
    }

    private fun esValido(s: AgendarUiState): Boolean {
        val pacienteOk = if (s.modoPaciente == ModoPaciente.NUEVO) {
            listOf(validarNombre(s.nombre), validarDocumento(s.documento), validarTelefono(s.telefono), validarCorreo(s.correo)).all { it == null }
        } else {
            s.pacientes.any { it.id == s.pacienteId }
        }
        return s.examenes.isNotEmpty() && pacienteOk && s.fecha != null && s.hora in s.franjas &&
            (!s.domicilio || validarDireccion(s.direccion) == null)
    }

    private fun cambiar(transformar: (AgendarUiState) -> AgendarUiState) =
        _estado.update { recalcular(transformar(it)) }

    /** Recalcula todo lo derivado (franjas, costos, errores) a partir de los datos crudos del estado. */
    private fun recalcular(s: AgendarUiState): AgendarUiState {
        val ayuno = s.examenes.maxOfOrNull { it.ayunoHoras } ?: 0
        val modo = modoElegido ?: if (s.pacientes.isEmpty() && !s.cargando) ModoPaciente.NUEVO else ModoPaciente.EXISTENTE
        val nuevo = modo == ModoPaciente.NUEVO
        return s.copy(
            ayunoHoras = ayuno,
            franjas = franjasDisponibles(ayuno),
            resumen = calcularResumen(s.examenes, s.adultoMayor, s.domicilio),
            indicaciones = indicacionesPreparacion(s.examenes),
            modoPaciente = modo,
            errorPaciente = if (s.intentado && !nuevo && s.pacientes.none { it.id == s.pacienteId }) "Selecciona un paciente" else null,
            errorNombre = if (nuevo) errorVisible(s.nombre, s.intentado, ::validarNombre) else null,
            errorDocumento = if (nuevo) errorVisible(s.documento, s.intentado, ::validarDocumento) else null,
            errorTelefono = if (nuevo) errorVisible(s.telefono, s.intentado, ::validarTelefono) else null,
            errorCorreo = if (nuevo) errorVisible(s.correo, s.intentado, ::validarCorreo) else null,
            errorFecha = if (s.intentado && s.fecha == null) "Selecciona una fecha" else null,
            errorHora = if (s.intentado && s.hora !in franjasDisponibles(ayuno)) "Selecciona una hora" else null,
            errorDireccion = if (s.domicilio) errorVisible(s.direccion, s.intentado, ::validarDireccion) else null
        )
    }
}
