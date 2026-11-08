package com.diego.vitther.domain.repository

import com.diego.vitther.data.local.ConteoFranquicia
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de piezas en domain/repository/ conforme a la sección 6 de la guía MVVM.
 */
interface PiezaRepositoryContract {
    fun observarTodas(): Flow<List<PiezaEntity>>
    fun observarPorEstado(estado: EstadoPieza): Flow<List<PiezaEntity>>
    fun observarPorFranquicia(franquicia: String): Flow<List<PiezaEntity>>
    fun observarConteoPorFranquicia(): Flow<List<ConteoFranquicia>>
    fun observarTotal(): Flow<Int>
    fun observarTotalFranquicias(): Flow<Int>
    fun observarPrimeraAdquisicion(): Flow<Long?>
    fun observarPieza(id: Long): Flow<PiezaEntity?>
    fun observarVitrina(): Flow<List<PiezaEntity>>
    suspend fun obtenerPorId(id: Long): PiezaEntity?
    suspend fun contar(): Int
    suspend fun guardar(pieza: PiezaEntity): Long
    suspend fun actualizar(pieza: PiezaEntity)
    suspend fun eliminar(pieza: PiezaEntity)
    suspend fun eliminarPorId(id: Long)
    suspend fun moverA(id: Long, estado: EstadoPieza)
    suspend fun destacar(id: Long, destacada: Boolean)
}
