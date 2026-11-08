package com.diego.vitther.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CapsulaDao {

    @Query("SELECT * FROM capsulas WHERE piezaId = :piezaId ORDER BY fechaApertura ASC")
    fun observarPorPieza(piezaId: Long): Flow<List<CapsulaEntity>>

    @Query("SELECT * FROM capsulas ORDER BY fechaApertura ASC")
    fun observarTodas(): Flow<List<CapsulaEntity>>

    /** La siguiente cápsula sellada que se abrirá. Alimenta el aviso del Home. */
    @Query("SELECT * FROM capsulas WHERE abiertaEn IS NULL ORDER BY fechaApertura ASC LIMIT 1")
    fun observarSelladaMasProxima(): Flow<CapsulaEntity?>

    /** Cápsulas que ya pueden abrirse y todavía no han sido avisadas. */
    @Query(
        "SELECT * FROM capsulas " +
            "WHERE abiertaEn IS NULL AND fechaApertura <= :ahora AND notificada = 0"
    )
    suspend fun pendientesDeAviso(ahora: Long): List<CapsulaEntity>

    @Insert
    suspend fun insertar(capsula: CapsulaEntity): Long

    @Insert
    suspend fun insertarTodas(capsulas: List<CapsulaEntity>): List<Long>

    @Update
    suspend fun actualizar(capsula: CapsulaEntity)

    @Delete
    suspend fun eliminar(capsula: CapsulaEntity)

    @Query("SELECT * FROM capsulas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): CapsulaEntity?

    // La fecha se verifica contra la fila persistida, no contra datos enviados por la UI.
    @Query("UPDATE capsulas SET abiertaEn = :cuando WHERE id = :id AND abiertaEn IS NULL AND fechaApertura <= :cuando")
    suspend fun marcarAbierta(id: Long, cuando: Long): Int

    @Query("UPDATE capsulas SET notificada = 1 WHERE id = :id")
    suspend fun marcarNotificada(id: Long)
}
