package com.diego.vitther.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "capsulas",
    foreignKeys = [
        ForeignKey(
            entity = PiezaEntity::class,
            parentColumns = ["id"],
            childColumns = ["piezaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("piezaId")]
)
data class CapsulaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val piezaId: Long,
    val titulo: String? = null,
    val nota: String,
    /** Momento a partir del cual puede revelarse. */
    val fechaApertura: Long,
    val creadaEn: Long = System.currentTimeMillis(),
    /** Null mientras siga sellada. Se llena al abrirla. */
    val abiertaEn: Long? = null,
    /** Evita notificar dos veces la misma cápsula. */
    val notificada: Boolean = false
)
