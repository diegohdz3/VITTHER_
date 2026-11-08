package com.diego.vitther.data.repository

import com.diego.vitther.data.local.CapsulaDao
import com.diego.vitther.data.local.CapsulaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.diego.vitther.domain.CapsulaVista
import java.time.Clock

class CapsulaRepository(private val dao: CapsulaDao, private val reloj: Clock = Clock.systemUTC()) {

    fun observarPorPieza(piezaId: Long): Flow<List<CapsulaVista>> =
        dao.observarPorPieza(piezaId).map { lista -> lista.map { it.vista() } }

    fun observarTodas(): Flow<List<CapsulaVista>> = dao.observarTodas().map { lista -> lista.map { it.vista() } }

    private fun CapsulaEntity.vista() = CapsulaVista(id, piezaId, titulo, fechaApertura, abiertaEn,
        nota.takeIf { abiertaEn != null })

    suspend fun sellar(
        piezaId: Long,
        titulo: String?,
        nota: String,
        fechaApertura: Long
    ): Long {
        require(nota.isNotBlank()) { "La cápsula necesita una nota." }
        require(fechaApertura > reloj.millis()) { "La cápsula debe abrirse en el futuro." }
        require(nota.length <= 5000) { "La nota puede tener hasta 5000 caracteres." }
        require((titulo?.length ?: 0) <= 80) { "El título puede tener hasta 80 caracteres." }

        return dao.insertar(
            CapsulaEntity(
                piezaId = piezaId,
                titulo = titulo?.trim()?.ifBlank { null },
                nota = nota.trim(),
                fechaApertura = fechaApertura,
                creadaEn = reloj.millis()
            )
        )
    }

    /**
     * La regla "no se puede leer antes de la fecha" se aplica aquí, no solo en
     * la UI: cualquier llamador futuro (notificación, widget) queda cubierto.
     */
    suspend fun abrir(
        id: Long
    ): Boolean {
        dao.marcarAbierta(id, reloj.millis())
        return dao.obtenerPorId(id)?.abiertaEn != null
    }

    suspend fun marcarNotificada(id: Long) = dao.marcarNotificada(id)

    suspend fun pendientesDeAviso(ahora: Long = System.currentTimeMillis()): List<CapsulaEntity> =
        dao.pendientesDeAviso(ahora)

    suspend fun eliminar(capsula: CapsulaEntity) = dao.eliminar(capsula)
}
