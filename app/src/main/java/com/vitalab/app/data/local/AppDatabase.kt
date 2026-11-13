package com.vitalab.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vitalab.app.data.Examen
import com.vitalab.app.data.catalogoExamenes

@Database(
    entities = [Examen::class, Paciente::class, Cita::class, CitaExamen::class, OrdenItem::class, OrdenAjuste::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examenDao(): ExamenDao
    abstract fun pacienteDao(): PacienteDao
    abstract fun citaDao(): CitaDao
    abstract fun ordenDao(): OrdenDao
    abstract fun estadisticasDao(): EstadisticasDao

    companion object {
        const val NOMBRE = "vitalab.db"

        /** `enMemoria = true` crea una base que se descarta al cerrar (pruebas); también se siembra. */
        fun crear(context: Context, enMemoria: Boolean = false): AppDatabase {
            val constructor = if (enMemoria) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, NOMBRE)
            }
            return constructor.addCallback(SembrarCatalogo).build()
        }
    }
}

/**
 * Siembra el catálogo una sola vez, cuando Room crea el archivo de la base.
 *
 * Se escribe con el `db` que llega a `onCreate` y NO con un DAO: en ese momento la instancia de
 * AppDatabase todavía no está lista (usar `instancia.examenDao()` aquí lanza error o se cuelga).
 * Corre dentro de la transacción de creación: o quedan los 11 exámenes o no queda nada.
 */
private object SembrarCatalogo : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        catalogoExamenes.forEach { e ->
            val fila = ContentValues().apply {
                put("id", e.id)
                put("nombre", e.nombre)
                put("categoria", e.categoria.name)
                put("precio", e.precio)
                put("descripcion", e.descripcion)
                put("muestra", e.muestra)
                put("ayunoHoras", e.ayunoHoras)
                put("horasEntrega", e.horasEntrega)
                put("indicacion", e.indicacion)
                put("activo", if (e.activo) 1 else 0)
            }
            db.insert("examenes", SQLiteDatabase.CONFLICT_ABORT, fila)
        }
    }
}
