package com.diego.vitther.domain.usecase

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.CapsulaVista
import com.diego.vitther.domain.Hito
import com.diego.vitther.domain.historiaDe
import com.diego.vitther.domain.model.ResumenMuseoModel
import com.diego.vitther.domain.model.calcularResumenMuseo
import java.time.LocalDate

/**
 * Casos de uso puros en domain/usecase/ conforme a la sección 5 de la guía MVVM.
 */
class ObtenerPiezaDelDiaUseCase {
    operator fun invoke(piezas: List<PiezaEntity>, fecha: LocalDate = LocalDate.now()): PiezaEntity? {
        if (piezas.isEmpty()) return null
        val indice = Math.floorMod(fecha.toEpochDay(), piezas.size.toLong()).toInt()
        return piezas[indice]
    }
}

class CalcularResumenMuseoUseCase {
    operator fun invoke(piezas: List<PiezaEntity>): ResumenMuseoModel =
        piezas.calcularResumenMuseo()
}

class GenerarHistoriaMuseoUseCase {
    operator fun invoke(piezas: List<PiezaEntity>, capsulas: List<CapsulaVista>): List<Hito> =
        historiaDe(piezas, capsulas)
}
