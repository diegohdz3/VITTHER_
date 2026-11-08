package com.diego.vitther.domain.model

import com.diego.vitther.data.local.PiezaEntity

/**
 * Modelos de dominio expuestos en domain/model/ conforme a la guía técnica MVVM.
 */
data class ResumenMuseoModel(
    val totalPiezas: Int = 0,
    val enExhibicion: Int = 0,
    val enBodega: Int = 0,
    val totalFranquicias: Int = 0,
    val valorTotalCentavos: Long = 0L
)

data class SugerenciaCatalogo(
    val idRemoto: String,
    val nombre: String,
    val franquicia: String,
    val linea: String,
    val anioLanzamiento: Int,
    val edicionLimitada: Boolean,
    val valorEstimadoCentavos: Long,
    val descripcion: String
)

fun List<PiezaEntity>.calcularResumenMuseo(): ResumenMuseoModel {
    val exhibidas = count { it.estado == com.diego.vitther.domain.EstadoPieza.EXHIBICION }
    val bodega = count { it.estado == com.diego.vitther.domain.EstadoPieza.BODEGA }
    val franquicias = map { it.franquicia.trim() }.distinct().size
    val valorTotal = sumOf { it.valorEstimadoCentavos ?: 0L }
    return ResumenMuseoModel(
        totalPiezas = size,
        enExhibicion = exhibidas,
        enBodega = bodega,
        totalFranquicias = franquicias,
        valorTotalCentavos = valorTotal
    )
}
