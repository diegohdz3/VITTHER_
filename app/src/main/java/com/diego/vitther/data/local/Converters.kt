package com.diego.vitther.data.local

import androidx.room.TypeConverter
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.EstadoWish

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

    @TypeConverter
    fun estadoWishATexto(estado: EstadoWish): String = estado.name

    @TypeConverter
    fun textoAEstadoWish(valor: String): EstadoWish =
        runCatching { EstadoWish.valueOf(valor) }.getOrDefault(EstadoWish.DESEADA)
}
