package com.diego.vitther.domain

/** La UI recibe metadatos, nunca el texto de una cápsula aún no revelada. */
data class CapsulaVista(
    val id: Long,
    val piezaId: Long,
    val titulo: String?,
    val fechaApertura: Long,
    val abiertaEn: Long?,
    val nota: String?
)
