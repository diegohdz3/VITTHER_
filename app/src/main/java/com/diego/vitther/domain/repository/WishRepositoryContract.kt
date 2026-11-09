package com.diego.vitther.domain.repository

import com.diego.vitther.data.local.WishEntity
import kotlinx.coroutines.flow.Flow

interface WishRepositoryContract {
    fun observarTodos(): Flow<List<WishEntity>>
    suspend fun guardar(wish: WishEntity): Long
    suspend fun actualizar(wish: WishEntity)
    suspend fun eliminar(wish: WishEntity)
    suspend fun pasarAPorLlegar(wish: WishEntity, fechaEstimada: Long)
}
