package com.diego.vitther.domain

/** Solo anuncia cápsulas futuras; las disponibles se consultan en su cédula. */
fun diasHastaProximaCapsula(capsulas: List<CapsulaVista>, ahora: Long): Long? =
    capsulas.asSequence()
        .filter { it.abiertaEn == null && it.fechaApertura > ahora }
        .minByOrNull { it.fechaApertura }
        ?.let { Fechas.diasHasta(it.fechaApertura, ahora) }
