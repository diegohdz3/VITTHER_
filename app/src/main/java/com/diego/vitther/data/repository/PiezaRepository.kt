package com.diego.vitther.data.repository

import com.diego.vitther.data.local.ConteoFranquicia
import com.diego.vitther.data.local.PiezaDao
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.Fechas
import com.diego.vitther.domain.validarPieza
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Única puerta de entrada a las piezas. La UI nunca toca el DAO directamente.
 */
class PiezaRepository(private val dao: PiezaDao) {

    fun observarTodas(): Flow<List<PiezaEntity>> = dao.observarTodas()

    fun observarPorEstado(estado: EstadoPieza): Flow<List<PiezaEntity>> =
        dao.observarPorEstado(estado)

    fun observarPorFranquicia(franquicia: String): Flow<List<PiezaEntity>> =
        dao.observarPorFranquicia(franquicia)

    fun observarConteoPorFranquicia(): Flow<List<ConteoFranquicia>> =
        dao.observarConteoPorFranquicia()

    fun observarTotal(): Flow<Int> = dao.observarTotal()

    fun observarTotalFranquicias(): Flow<Int> = dao.observarTotalFranquicias()

    fun observarPrimeraAdquisicion(): Flow<Long?> = dao.observarPrimeraAdquisicion()

    fun observarPieza(id: Long): Flow<PiezaEntity?> = dao.observarPorId(id)

    /**
     * Vitrina del Home: las piezas marcadas como destacadas que estén exhibidas.
     * Si el usuario no ha destacado ninguna, cae a las exhibidas más recientes
     * para que el carrusel nunca arranque vacío con una colección existente.
     */
    fun observarVitrina(): Flow<List<PiezaEntity>> = combine(
        dao.observarDestacadas(),
        dao.observarPorEstado(EstadoPieza.EXHIBICION)
    ) { destacadas, exhibidas ->
        val curadas = destacadas.filter { it.estado == EstadoPieza.EXHIBICION }
        if (curadas.isNotEmpty()) curadas else exhibidas.take(6)
    }

    suspend fun obtenerPorId(id: Long): PiezaEntity? = dao.obtenerPorId(id)

    suspend fun contar(): Int = dao.contar()

    suspend fun guardar(pieza: PiezaEntity): Long =
        dao.insertar(normalizar(pieza))

    suspend fun actualizar(pieza: PiezaEntity) =
        dao.actualizar(normalizar(pieza).copy(actualizadoEn = System.currentTimeMillis()))

    suspend fun eliminar(pieza: PiezaEntity) = dao.eliminar(pieza)

    suspend fun eliminarPorId(id: Long) = dao.eliminarPorId(id)

    suspend fun moverA(id: Long, estado: EstadoPieza) =
        dao.cambiarEstado(id, estado, System.currentTimeMillis())

    suspend fun destacar(id: Long, destacada: Boolean) =
        dao.marcarDestacada(id, destacada, System.currentTimeMillis())

    private fun normalizar(pieza: PiezaEntity): PiezaEntity {
        require(!validarPieza(pieza.nombre, pieza.franquicia, pieza.fechaAdquisicion).hayErrores) {
            "Revisa el nombre, la franquicia y la fecha de adquisición."
        }
        return pieza.copy(
        nombre = pieza.nombre.trim(),
        franquicia = pieza.franquicia.trim(),
        linea = pieza.linea?.trim()?.ifBlank { null },
        ubicacion = pieza.ubicacion?.trim()?.ifBlank { null },
        procedencia = pieza.procedencia?.trim()?.ifBlank { null },
        notaPersonal = pieza.notaPersonal?.trim()?.ifBlank { null },
        fechaAdquisicion = Fechas.inicioDelDia(pieza.fechaAdquisicion)
    )
    }
}
