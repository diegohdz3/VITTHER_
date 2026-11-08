package com.diego.vitther.domain.repository

import com.diego.vitther.data.local.CapsulaEntity
import com.diego.vitther.domain.CapsulaVista
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de cápsulas en domain/repository/ conforme a la sección 6 de la guía MVVM.
 */
interface CapsulaRepositoryContract {
    fun observarPorPieza(piezaId: Long): Flow<List<CapsulaVista>>
    fun observarTodas(): Flow<List<CapsulaVista>>
    suspend fun sellar(piezaId: Long, titulo: String?, nota: String, fechaApertura: Long): Long
    suspend fun abrir(id: Long): Boolean
    suspend fun marcarNotificada(id: Long)
    suspend fun pendientesDeAviso(ahora: Long = System.currentTimeMillis()): List<CapsulaEntity>
    suspend fun eliminar(capsula: CapsulaEntity)
}
