package com.diego.vitther.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs de respuesta y petición para el servicio externo de catálogo y
 * la sincronización completa (piezas, cápsulas y wishes) con Firebase Realtime Database / REST API.
 */
@Serializable
data class JikanCharacterSearchResponseDto(
    @SerialName("data") val data: List<JikanCharacterDto> = emptyList()
)

@Serializable
data class JikanCharacterDto(
    @SerialName("mal_id") val malId: Long = 0L,
    @SerialName("name") val name: String = "",
    @SerialName("nicknames") val nicknames: List<String> = emptyList(),
    @SerialName("favorites") val favorites: Int = 0,
    @SerialName("about") val about: String? = null
)

@Serializable
data class PiezaCloudDto(
    val idLocal: Long,
    val nombre: String,
    val franquicia: String,
    val linea: String? = null,
    val fechaAdquisicion: Long,
    val estado: String,
    val ubicacion: String? = null,
    val procedencia: String? = null,
    val edicionLimitada: Boolean = false,
    val numeroEdicion: String? = null,
    val valorEstimadoCentavos: Long? = null,
    val notaPersonal: String? = null,
    val destacada: Boolean = false,
    val sincronizadoEn: Long
)

@Serializable
data class CapsulaCloudDto(
    val idLocal: Long,
    val piezaId: Long,
    val titulo: String? = null,
    val fechaApertura: Long,
    val abiertaEn: Long? = null,
    val notaRevelada: String? = null
)

@Serializable
data class WishCloudDto(
    val idLocal: Long,
    val nombre: String,
    val franquicia: String,
    val linea: String? = null,
    val estado: String,
    val metaPesos: Long? = null,
    val progresoPorcentaje: Int = 0,
    val fechaEstimadaLlegada: Long? = null,
    val creadoEn: Long
)

@Serializable
data class RespaldoFirebaseDto(
    val coleccionista: String,
    val versionApp: String,
    val actualizadoEn: Long,
    val totalPiezas: Int,
    val totalCapsulas: Int = 0,
    val totalWishes: Int = 0,
    val piezas: List<PiezaCloudDto> = emptyList(),
    val capsulas: List<CapsulaCloudDto> = emptyList(),
    val wishes: List<WishCloudDto> = emptyList()
)
