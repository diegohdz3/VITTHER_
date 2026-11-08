package com.diego.vitther.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {

    @Query("SELECT * FROM wishes ORDER BY creadoEn DESC")
    fun observarTodos(): Flow<List<WishEntity>>

    @Query("SELECT COUNT(*) FROM wishes")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(wish: WishEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(wishes: List<WishEntity>): List<Long>

    @Update
    suspend fun actualizar(wish: WishEntity)

    @Delete
    suspend fun eliminar(wish: WishEntity)

    @Query("DELETE FROM wishes WHERE id = :id")
    suspend fun eliminarPorId(id: Long)
}
