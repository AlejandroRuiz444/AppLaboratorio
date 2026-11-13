package com.vitalab.app.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.vitalab.app.data.Examen
import com.vitalab.app.data.Resumen

// Resultados de consultas con relaciones y agregados. No son tablas: Room los arma a partir de un SELECT.

/** Una línea de la cita: el examen y el precio que se cobró (no el actual del catálogo). */
data class LineaCita(val examen: Examen, val precio: Int)

/** Cita + su paciente + sus exámenes (relación N—M por la tabla puente `cita_examen`). */
data class CitaCompleta(
    @Embedded val cita: Cita,
    @Relation(parentColumn = "pacienteId", entityColumn = "id")
    val paciente: Paciente,
    @Relation(
        parentColumn = "id", entityColumn = "id",
        associateBy = Junction(value = CitaExamen::class, parentColumn = "citaId", entityColumn = "examenId")
    )
    val examenes: List<Examen>,
    @Relation(parentColumn = "id", entityColumn = "citaId")
    val lineas: List<CitaExamen>
) {
    /** Líneas con nombre y precio congelado, ordenadas por nombre. Se calcula en cada llamada: guárdala en una variable. */
    val detalle: List<LineaCita>
        get() {
            val precios = lineas.associate { it.examenId to it.precio }
            return examenes.map { LineaCita(it, precios.getValue(it.id)) }.sortedBy { it.examen.nombre }
        }
    val resumen: Resumen get() = cita.resumen
}

/** Paciente con la cantidad de citas que tiene (para la lista y para avisar antes de eliminar). */
data class PacienteConConteo(
    @Embedded val paciente: Paciente,
    val totalCitas: Int
)

data class ConteoEstado(val estado: EstadoCita, val cantidad: Int)
data class ExamenPopular(val examenId: Int, val nombre: String, val veces: Int)
data class CitasPorPaciente(val pacienteId: Long, val nombre: String, val total: Int)
