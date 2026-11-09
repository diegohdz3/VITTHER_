package com.diego.vitther.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diego.vitther.domain.EstadoWish

@Entity(
    tableName = "wishes",
    indices = [
        Index(value = ["estado"]),
        Index(value = ["franquicia"])
    ]
)
data class WishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val franquicia: String,
    val linea: String? = null,
    val estado: EstadoWish = EstadoWish.DESEADA,
    val fechaEstimadaLlegada: Long? = null,
    val progresoPorcentaje: Int = 0,
    val metaPesos: Long? = null,
    val fotoPath: String? = null,
    val creadoEn: Long = System.currentTimeMillis()
)
