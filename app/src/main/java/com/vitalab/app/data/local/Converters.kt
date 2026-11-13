package com.vitalab.app.data.local

import androidx.room.TypeConverter
import com.vitalab.app.data.Categoria

/** Los enums se guardan como texto (su nombre): legible en la base y estable si se reordenan. */
class Converters {
    @TypeConverter fun categoriaATexto(valor: Categoria): String = valor.name
    @TypeConverter fun textoACategoria(valor: String): Categoria = Categoria.valueOf(valor)

    @TypeConverter fun estadoATexto(valor: EstadoCita): String = valor.name
    @TypeConverter fun textoAEstado(valor: String): EstadoCita = EstadoCita.valueOf(valor)
}
