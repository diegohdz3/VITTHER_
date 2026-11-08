package com.diego.vitther.data.local

import androidx.room.TypeConverter
import com.diego.vitther.domain.EstadoPieza

/**
 * Los enums se guardan como texto, no como ordinal: agregar un estado nuevo
 * en el futuro no corrompe las filas existentes.
 */
class Converters {

    @TypeConverter
    fun estadoATexto(estado: EstadoPieza): String = estado.name

    @TypeConverter
    fun textoAEstado(valor: String): EstadoPieza =
        runCatching { EstadoPieza.valueOf(valor) }.getOrDefault(EstadoPieza.BODEGA)
}
