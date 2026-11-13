package com.vitalab.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalab.app.data.Resumen
import com.vitalab.app.data.calcularResumenDePrecios
import com.vitalab.app.data.franjasDisponibles
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.proximasFechas
import com.vitalab.app.data.repository.LabRepository
import com.vitalab.app.data.repository.Resultado
import com.vitalab.app.data.validarDireccion
import com.vitalab.app.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReprogramarUiState(
    val cargando: Boolean = true,
    val noEncontrada: Boolean = false,
    /** La cita existe pero ya no está PROGRAMADA: no se puede reprogramar. */
    val noReprogramable: Boolean = false,
    val codigo: String = "",
    val paciente: String = "",
    /** Días ofrecidos (sin domingos), como `LocalDate.toEpochDay()`; incluye el día actual de la cita si es futuro. */
    val fechasDisponibles: List<Long> = emptyList(),
    /** Si algún examen exige ayuno, solo hasta las 9:00 a.m. */
    val franjas: List<String> = emptyList(),
    val ayunoHoras: Int = 0,
    val fecha: Long? = null,
    val hora: String? = null,
    val domicilio: Boolean = false,
    val direccion: String = "",
    /** Costos con el recargo según la modalidad elegida (precios congelados al agendar). */
    val resumen: Resumen = Resumen(0, 0, 0, 0),
    val errorFecha: String? = null,
    val errorHora: String? = null,
    val errorDireccion: String? = null,
    /** Error que no pertenece a un campo (p. ej. la cita cambió de estado mientras editabas). */
    val errorGeneral: String? = null,
    val intentado: Boolean = false,
    val guardando: Boolean = false,
    /** Pasa a true cuando se guardó: la pantalla debe volver atrás. */
    val guardado: Boolean = false
)

/** Reprogramar una cita PROGRAMADA (id en la ruta): fecha, hora y modalidad/dirección. */
class ReprogramarCitaViewModel(
    private val repo: LabRepository,
    savedStateHandle: SavedStateHandle,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>(Rutas.ARG_ID) ?: -1L
    private var original: CitaCompleta? = null

    private val _estado = MutableStateFlow(ReprogramarUiState())
    val uiState: StateFlow<ReprogramarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            // Una sola lectura: el formulario no debe pisarse con cambios posteriores de Room.
            val cita = repo.cita(id).first()
            original = cita
            _estado.update {
                when {
                    cita == null -> it.copy(cargando = false, noEncontrada = true)
                    cita.cita.estado != EstadoCita.PROGRAMADA -> it.copy(cargando = false, noReprogramable = true)
                    else -> cargarFormulario(it, cita)
                }
            }
        }
    }

    fun onFechaChange(epochDay: Long) = cambiar { it.copy(fecha = epochDay) }
    fun onHoraChange(hora: String) = cambiar { it.copy(hora = hora) }
    fun onDomicilioChange(valor: Boolean) = cambiar { it.copy(domicilio = valor) }
    fun onDireccionChange(texto: String) = cambiar { it.copy(direccion = texto) }

    fun onGuardar() {
        cambiar { it.copy(intentado = true, errorGeneral = null) }
        val s = _estado.value
        if (s.cargando || s.guardando || s.guardado || !esValido(s)) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val r = repo.reprogramarCita(id, s.fecha!!, s.hora!!, s.domicilio, s.direccion)
            _estado.update {
                when (r) {
                    is Resultado.Exito -> it.copy(guardando = false, guardado = true)
                    is Resultado.Error -> it.copy(guardando = false, errorGeneral = r.mensaje)
                }
            }
        }
    }

    private fun cargarFormulario(base: ReprogramarUiState, cita: CitaCompleta): ReprogramarUiState {
        val ayuno = cita.examenes.maxOfOrNull { it.ayunoHoras } ?: 0
        val franjas = franjasDisponibles(ayuno)
        val hoyDia = hoy().toEpochDay()
        val fechas = (proximasFechas(hoy()).map { it.toEpochDay() } + cita.cita.fecha.takeIf { it > hoyDia })
            .filterNotNull().distinct().sorted()
        return conErrores(
            base.copy(
                cargando = false,
                codigo = cita.cita.codigo,
                paciente = cita.paciente.nombre,
                fechasDisponibles = fechas,
                franjas = franjas,
                ayunoHoras = ayuno,
                // Se preseleccionan los valores actuales solo si siguen siendo válidos.
                fecha = cita.cita.fecha.takeIf { it in fechas },
                hora = cita.cita.hora.takeIf { it in franjas },
                domicilio = cita.cita.domicilio,
                direccion = cita.cita.direccion
            )
        )
    }

    private fun esValido(s: ReprogramarUiState) =
        s.fecha != null && s.hora in s.franjas && (!s.domicilio || validarDireccion(s.direccion) == null)

    private fun cambiar(transformar: (ReprogramarUiState) -> ReprogramarUiState) =
        _estado.update { conErrores(transformar(it)) }

    private fun conErrores(s: ReprogramarUiState): ReprogramarUiState {
        val precios = original?.lineas?.map { it.precio }.orEmpty()
        return s.copy(
            resumen = calcularResumenDePrecios(precios, original?.cita?.adultoMayor ?: false, s.domicilio),
            errorFecha = if (s.intentado && s.fecha == null) "Selecciona una fecha" else null,
            errorHora = if (s.intentado && s.hora !in s.franjas) "Selecciona una hora" else null,
            errorDireccion = if (s.domicilio) errorVisible(s.direccion, s.intentado, ::validarDireccion) else null
        )
    }
}
