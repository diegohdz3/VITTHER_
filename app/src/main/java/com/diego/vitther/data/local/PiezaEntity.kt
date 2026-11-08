package com.diego.vitther.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diego.vitther.domain.EstadoPieza

@Entity(
    tableName = "piezas",
    indices = [Index("franquicia"), Index("estado")]
)
data class PiezaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val franquicia: String,
    /** Línea o fabricante: "S.H.Figuarts", "Hot Toys MMS". */
    val linea: String? = null,
    /** Obligatoria. Normalizada al inicio del día local. */
    val fechaAdquisicion: Long,
    /** Cuándo se registró en el museo (hito "agregada" de la Historia). */
    val fechaRegistro: Long = System.currentTimeMillis(),
    /** Archivo en filesDir/fotos, nunca una URI del picker. */
    val fotoPath: String? = null,
    val estado: EstadoPieza = EstadoPieza.EXHIBICION,
    /** Sale en la vitrina del Home. */
    val destacada: Boolean = false,
    /** "Estante 3 · caja B". */
    val ubicacion: String? = null,
    val procedencia: String? = null,
    val notaPersonal: String? = null,
    val edicionLimitada: Boolean = false,
    val numeroEdicion: String? = null,
    val valorEstimadoCentavos: Long? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
