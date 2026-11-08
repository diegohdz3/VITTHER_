package com.diego.vitther.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ProximaCapsulaTest {
    @Test fun ignoraDisponiblesYReveladasYEligeLaFuturaMasCercana() {
        val hoy = LocalDate.of(2026, 10, 27)
        val ahora = Fechas.desdeFecha(hoy)
        fun capsula(id: Long, dias: Long, abierta: Boolean = false) = CapsulaVista(
            id, 1, null, Fechas.desdeFecha(hoy.plusDays(dias)), if (abierta) ahora else null, null
        )
        assertEquals(12L, diasHastaProximaCapsula(listOf(
            capsula(1, -1), capsula(2, 0), capsula(3, 2, true), capsula(4, 45), capsula(5, 12)
        ), ahora))
        assertNull(diasHastaProximaCapsula(listOf(capsula(1, 0), capsula(2, 1, true)), ahora))
        assertNull(diasHastaProximaCapsula(emptyList(), ahora))
    }
}
