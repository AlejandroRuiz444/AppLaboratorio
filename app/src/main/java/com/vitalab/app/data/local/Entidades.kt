package com.vitalab.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalab.app.data.Examen
import com.vitalab.app.data.Resumen
import java.time.LocalDate

enum class EstadoCita(val etiqueta: String) {
    PROGRAMADA("Programada"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada")
}

/** Quien se hace los exámenes. El documento es único (índice en la base de datos). */
@Entity(tableName = "pacientes", indices = [Index(value = ["documento"], unique = true)])
data class Paciente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val documento: String,
    val telefono: String,
    val correo: String,
    val creadoEn: Long = System.currentTimeMillis()
)

/**
 * Toma de muestra agendada. Guarda una "foto" del costo (subtotal, descuentos, recargo) para que
 * cambiar precios del catálogo después no altere citas ya agendadas.
 * Si se elimina el paciente, sus citas se eliminan con él (CASCADE).
 */
@Entity(
    tableName = "citas",
    foreignKeys = [ForeignKey(Paciente::class, parentColumns = ["id"], childColumns = ["pacienteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("pacienteId"), Index(value = ["codigo"], unique = true)]
)
data class Cita(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val pacienteId: Long,
    /** Día de la cita como `LocalDate.toEpochDay()`. */
    val fecha: Long,
    /** Texto tomado de `FRANJAS`, p. ej. "7:30 a.m.". */
    val hora: String,
    val domicilio: Boolean,
    val direccion: String,
    val estado: EstadoCita = EstadoCita.PROGRAMADA,
    val adultoMayor: Boolean,
    val subtotal: Int,
    val descuentoPaquete: Int,
    val descuentoAdultoMayor: Int,
    val recargoDomicilio: Int,
    val creadaEn: Long = System.currentTimeMillis()
) {
    // Propiedades derivadas (sin campo): Room no las guarda.
    val fechaLocal: LocalDate get() = LocalDate.ofEpochDay(fecha)
    val resumen: Resumen get() = Resumen(subtotal, descuentoPaquete, descuentoAdultoMayor, recargoDomicilio)
    val total: Int get() = resumen.total
}

/** Tabla puente Cita N—M Examen, con el precio que tenía el examen al agendar. */
@Entity(
    tableName = "cita_examen",
    primaryKeys = ["citaId", "examenId"],
    foreignKeys = [
        ForeignKey(Cita::class, parentColumns = ["id"], childColumns = ["citaId"], onDelete = ForeignKey.CASCADE),
        // Sin CASCADE: un examen con citas no se puede borrar de verdad (solo desactivar).
        ForeignKey(Examen::class, parentColumns = ["id"], childColumns = ["examenId"])
    ],
    indices = [Index("examenId")]
)
data class CitaExamen(
    val citaId: Long,
    val examenId: Int,
    val precio: Int
)

/** Un examen dentro de la orden en curso (el "carrito"). Persiste al cerrar la app. */
@Entity(
    tableName = "orden_items",
    foreignKeys = [ForeignKey(Examen::class, parentColumns = ["id"], childColumns = ["examenId"], onDelete = ForeignKey.CASCADE)]
)
data class OrdenItem(@PrimaryKey val examenId: Int)

/**
 * Ajustes de la orden en curso: una sola fila (id = 1). Vive en Room y no en un ViewModel porque
 * el switch "adulto mayor" lo leen varias pantallas, cada una con su propio ViewModel.
 */
@Entity(tableName = "orden_ajustes")
data class OrdenAjuste(@PrimaryKey val id: Int = 1, val adultoMayor: Boolean)
