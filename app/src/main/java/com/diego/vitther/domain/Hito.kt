package com.diego.vitther.domain

import com.diego.vitther.data.local.PiezaEntity

enum class TipoHito { ADQUISICION, REGISTRO, CAPSULA }

data class Hito(val clave: String, val piezaId: Long, val nombrePieza: String,
    val fecha: Long, val tipo: TipoHito)

/** Se deriva de fechas persistidas: refrescar o reabrir una cápsula no duplica hitos. */
fun historiaDe(piezas: List<PiezaEntity>, capsulas: List<CapsulaVista>): List<Hito> {
    val porId = piezas.associateBy { it.id }
    return buildList {
        piezas.forEach { pieza ->
            add(Hito("adquisicion-${pieza.id}", pieza.id, pieza.nombre, pieza.fechaAdquisicion, TipoHito.ADQUISICION))
            // Para la mayoría de piezas adquirir y registrar ocurrió el mismo día:
            // mostrarlo dos veces solo llena la línea de tiempo.
            if (!Fechas.mismoDia(pieza.fechaRegistro, pieza.fechaAdquisicion)) {
                add(Hito("registro-${pieza.id}", pieza.id, pieza.nombre, pieza.fechaRegistro, TipoHito.REGISTRO))
            }
        }
        capsulas.forEach { capsula ->
            val pieza = porId[capsula.piezaId]
            if (capsula.abiertaEn != null && pieza != null)
                add(Hito("capsula-${capsula.id}", pieza.id, pieza.nombre, capsula.abiertaEn, TipoHito.CAPSULA))
        }
    }.sortedWith(compareByDescending<Hito> { it.fecha }.thenBy { it.clave })
}
