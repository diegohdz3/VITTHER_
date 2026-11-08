package com.diego.vitther.data.repository

import com.diego.vitther.data.local.WishDao
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.domain.repository.WishRepositoryContract
import kotlinx.coroutines.flow.Flow

class WishRepository(private val dao: WishDao) : WishRepositoryContract {

    override fun observarTodos(): Flow<List<WishEntity>> = dao.observarTodos()

    override suspend fun guardar(wish: WishEntity): Long {
        require(wish.nombre.isNotBlank()) { "Escribe el nombre de la pieza." }
        require(wish.franquicia.isNotBlank()) { "Escribe la franquicia." }
        return dao.insertar(
            wish.copy(
                nombre = wish.nombre.trim(),
                franquicia = wish.franquicia.trim(),
                linea = wish.linea?.trim()?.ifBlank { null },
                progresoPorcentaje = wish.progresoPorcentaje.coerceIn(5, 95)
            )
        )
    }

    override suspend fun actualizar(wish: WishEntity) {
        dao.actualizar(
            wish.copy(
                nombre = wish.nombre.trim(),
                franquicia = wish.franquicia.trim(),
                linea = wish.linea?.trim()?.ifBlank { null },
                progresoPorcentaje = wish.progresoPorcentaje.coerceIn(5, 95)
            )
        )
    }

    override suspend fun eliminar(wish: WishEntity) = dao.eliminar(wish)

    override suspend fun pasarAPorLlegar(wish: WishEntity, fechaEstimada: Long) {
        dao.actualizar(
            wish.copy(
                estado = EstadoWish.POR_LLEGAR,
                fechaEstimadaLlegada = fechaEstimada,
                progresoPorcentaje = 15
            )
        )
    }
}
