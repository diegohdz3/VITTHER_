package com.diego.vitther.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.flow.Flow

@Dao
interface PiezaDao {

    @Query("SELECT * FROM piezas ORDER BY fechaRegistro DESC")
    fun observarTodas(): Flow<List<PiezaEntity>>

    @Query("SELECT * FROM piezas WHERE estado = :estado ORDER BY fechaRegistro DESC")
    fun observarPorEstado(estado: EstadoPieza): Flow<List<PiezaEntity>>

    @Query("SELECT * FROM piezas WHERE franquicia = :franquicia ORDER BY fechaRegistro DESC")
    fun observarPorFranquicia(franquicia: String): Flow<List<PiezaEntity>>

    @Query("SELECT * FROM piezas WHERE destacada = 1 ORDER BY fechaRegistro DESC")
    fun observarDestacadas(): Flow<List<PiezaEntity>>

    @Query(
        "SELECT franquicia, COUNT(*) AS total FROM piezas " +
            "GROUP BY franquicia ORDER BY franquicia COLLATE NOCASE ASC"
    )
    fun observarConteoPorFranquicia(): Flow<List<ConteoFranquicia>>

    @Query("SELECT * FROM piezas WHERE id = :id")
    fun observarPorId(id: Long): Flow<PiezaEntity?>

    @Query("SELECT * FROM piezas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): PiezaEntity?

    @Query("SELECT COUNT(*) FROM piezas")
    fun observarTotal(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT franquicia) FROM piezas")
    fun observarTotalFranquicias(): Flow<Int>

    @Query("SELECT MIN(fechaAdquisicion) FROM piezas")
    fun observarPrimeraAdquisicion(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM piezas")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(pieza: PiezaEntity): Long

    @Insert
    suspend fun insertarTodas(piezas: List<PiezaEntity>): List<Long>

    @Update
    suspend fun actualizar(pieza: PiezaEntity)

    @Delete
    suspend fun eliminar(pieza: PiezaEntity)

    @Query("DELETE FROM piezas WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("UPDATE piezas SET estado = :estado, actualizadoEn = :cuando WHERE id = :id")
    suspend fun cambiarEstado(id: Long, estado: EstadoPieza, cuando: Long)

    @Query("UPDATE piezas SET destacada = :destacada, actualizadoEn = :cuando WHERE id = :id")
    suspend fun marcarDestacada(id: Long, destacada: Boolean, cuando: Long)
}
