package com.vitalab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vitalab.app.data.Examen
import kotlinx.coroutines.flow.Flow

// Lecturas -> Flow (la UI se actualiza sola cuando cambia la tabla). Escrituras -> suspend.

@Dao
interface ExamenDao {
    @Query("SELECT * FROM examenes WHERE activo = 1 ORDER BY id")
    fun observarActivos(): Flow<List<Examen>>

    @Query("SELECT * FROM examenes ORDER BY id")
    fun observarTodos(): Flow<List<Examen>>

    @Query("SELECT * FROM examenes WHERE id = :id")
    suspend fun obtener(id: Int): Examen?

    @Insert
    suspend fun insertar(examen: Examen): Long

    @Update
    suspend fun actualizar(examen: Examen)

    @Query("UPDATE examenes SET activo = :activo WHERE id = :id")
    suspend fun cambiarActivo(id: Int, activo: Boolean)
}

@Dao
interface PacienteDao {
    @Query(
        """SELECT p.*, (SELECT COUNT(*) FROM citas c WHERE c.pacienteId = p.id) AS totalCitas
           FROM pacientes p ORDER BY p.nombre COLLATE NOCASE"""
    )
    fun observarConConteo(): Flow<List<PacienteConConteo>>

    @Query("SELECT * FROM pacientes WHERE id = :id")
    suspend fun obtener(id: Long): Paciente?

    @Query("SELECT * FROM pacientes WHERE documento = :documento LIMIT 1")
    suspend fun porDocumento(documento: String): Paciente?

    @Insert
    suspend fun insertar(paciente: Paciente): Long

    @Update
    suspend fun actualizar(paciente: Paciente)

    /** Las citas del paciente se eliminan solas (FK con CASCADE). */
    @Query("DELETE FROM pacientes WHERE id = :id")
    suspend fun eliminar(id: Long)
}

@Dao
abstract class CitaDao {
    @Insert
    abstract suspend fun insertar(cita: Cita): Long

    @Insert
    abstract suspend fun insertarLineas(lineas: List<CitaExamen>)

    /** Cita y sus líneas se guardan juntas o no se guarda nada. `precios`: examenId -> precio de ese momento. */
    @Transaction
    open suspend fun crearConLineas(cita: Cita, precios: Map<Int, Int>): Long {
        val id = insertar(cita)
        insertarLineas(precios.map { (examenId, precio) -> CitaExamen(id, examenId, precio) })
        return id
    }

    @Query("SELECT COUNT(*) FROM citas WHERE codigo = :codigo")
    abstract suspend fun contarPorCodigo(codigo: String): Int

    // Primero las programadas (la más próxima arriba), después el historial (lo más reciente arriba).
    @Transaction
    @Query(
        """SELECT * FROM citas WHERE (:estado IS NULL OR estado = :estado)
           ORDER BY (estado != 'PROGRAMADA'), CASE WHEN estado = 'PROGRAMADA' THEN fecha ELSE -fecha END, id"""
    )
    abstract fun observarCompletas(estado: EstadoCita?): Flow<List<CitaCompleta>>

    @Transaction
    @Query("SELECT * FROM citas WHERE id = :id")
    abstract fun observarCompleta(id: Long): Flow<CitaCompleta?>

    @Transaction
    @Query("SELECT * FROM citas WHERE id = :id")
    abstract suspend fun obtenerCompleta(id: Long): CitaCompleta?

    @Transaction
    @Query("SELECT * FROM citas WHERE estado = 'PROGRAMADA' AND fecha >= :desde ORDER BY fecha, id")
    abstract fun observarProgramadasDesde(desde: Long): Flow<List<CitaCompleta>>

    /** Devuelve cuántas filas cambió: 0 si la cita no existe o ya no está PROGRAMADA. */
    @Query("UPDATE citas SET estado = :nuevo WHERE id = :id AND estado = 'PROGRAMADA'")
    abstract suspend fun cambiarEstadoSiProgramada(id: Long, nuevo: EstadoCita): Int

    @Query(
        """UPDATE citas SET fecha = :fecha, hora = :hora, domicilio = :domicilio, direccion = :direccion,
           recargoDomicilio = :recargo WHERE id = :id AND estado = 'PROGRAMADA'"""
    )
    abstract suspend fun reprogramarSiProgramada(
        id: Long, fecha: Long, hora: String, domicilio: Boolean, direccion: String, recargo: Int
    ): Int

    @Query("DELETE FROM citas WHERE id = :id")
    abstract suspend fun eliminar(id: Long): Int
}

@Dao
abstract class OrdenDao {
    @Query("SELECT e.* FROM examenes e JOIN orden_items o ON o.examenId = e.id ORDER BY e.id")
    abstract fun observarExamenes(): Flow<List<Examen>>

    @Query("SELECT e.* FROM examenes e JOIN orden_items o ON o.examenId = e.id ORDER BY e.id")
    abstract suspend fun examenes(): List<Examen>

    @Query("SELECT adultoMayor FROM orden_ajustes WHERE id = 1")
    abstract fun observarAdultoMayor(): Flow<Boolean?>

    @Query("SELECT adultoMayor FROM orden_ajustes WHERE id = 1")
    abstract suspend fun adultoMayor(): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarAjuste(ajuste: OrdenAjuste)

    @Query("DELETE FROM orden_items WHERE examenId = :examenId")
    abstract suspend fun quitar(examenId: Int): Int

    // Solo agrega exámenes activos: un examen desactivado no se puede pedir.
    @Query("INSERT OR IGNORE INTO orden_items(examenId) SELECT id FROM examenes WHERE id = :examenId AND activo = 1")
    abstract suspend fun agregarSiActivo(examenId: Int)

    /** Agrega el examen si no está en la orden y lo quita si ya estaba. */
    @Transaction
    open suspend fun alternar(examenId: Int) {
        if (quitar(examenId) == 0) agregarSiActivo(examenId)
    }

    @Query("DELETE FROM orden_items")
    abstract suspend fun vaciar()
}

@Dao
interface EstadisticasDao {
    @Query("SELECT estado, COUNT(*) AS cantidad FROM citas GROUP BY estado")
    fun citasPorEstado(): Flow<List<ConteoEstado>>

    @Query(
        """SELECT COALESCE(SUM(subtotal - descuentoPaquete - descuentoAdultoMayor + recargoDomicilio), 0)
           FROM citas WHERE estado = 'COMPLETADA'"""
    )
    fun ingresosCompletadas(): Flow<Int>

    @Query("SELECT COALESCE(SUM(descuentoPaquete + descuentoAdultoMayor), 0) FROM citas WHERE estado != 'CANCELADA'")
    fun ahorroPorDescuentos(): Flow<Int>

    @Query(
        """SELECT e.id AS examenId, e.nombre AS nombre, COUNT(*) AS veces
           FROM cita_examen ce
           JOIN citas c ON c.id = ce.citaId
           JOIN examenes e ON e.id = ce.examenId
           WHERE c.estado != 'CANCELADA'
           GROUP BY e.id ORDER BY veces DESC, e.nombre LIMIT :limite"""
    )
    fun examenesMasSolicitados(limite: Int): Flow<List<ExamenPopular>>

    @Query(
        """SELECT p.id AS pacienteId, p.nombre AS nombre, COUNT(c.id) AS total
           FROM pacientes p JOIN citas c ON c.pacienteId = p.id
           GROUP BY p.id ORDER BY total DESC, p.nombre"""
    )
    fun citasPorPaciente(): Flow<List<CitasPorPaciente>>
}
