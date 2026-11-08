package com.diego.vitther

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.diego.vitther.data.local.*
import com.diego.vitther.data.media.FotoStore
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PersistenciaTest {
    @Test fun fotoYDatosPersistenSinElArchivoOriginal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombreDb = "prueba-${System.nanoTime()}.db"
        val original = File(context.cacheDir, "prueba-${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(1800, 2400, Bitmap.Config.ARGB_8888)
        original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val fotos = FotoStore(context)
        val ruta = fotos.importar(Uri.fromFile(original))
        original.delete()
        var db = Room.databaseBuilder(context, VittherDatabase::class.java, nombreDb).build()
        try {
            val repo = PiezaRepository(db.piezaDao())
            val id = repo.guardar(PiezaEntity(nombre = "Prueba", franquicia = "Museo", fechaAdquisicion = 1700000000000L, fotoPath = ruta))
            db.capsulaDao().insertar(CapsulaEntity(piezaId = id, nota = "Recuerdo", fechaApertura = Long.MAX_VALUE))
            repo.moverA(id, EstadoPieza.BODEGA)
            repo.destacar(id, true)
            db.close()
            db = Room.databaseBuilder(context, VittherDatabase::class.java, nombreDb).build()
            val recuperada = db.piezaDao().obtenerPorId(id)!!
            assertEquals(EstadoPieza.BODEGA, recuperada.estado)
            assertTrue(recuperada.destacada)
            assertEquals(ruta, recuperada.fotoPath)
            assertTrue(fotos.archivo(recuperada.fotoPath)!!.exists())
            val dimensiones = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(fotos.archivo(ruta)!!.path, dimensiones)
            assertEquals(1280, maxOf(dimensiones.outWidth, dimensiones.outHeight))
            db.piezaDao().eliminarPorId(id)
            assertTrue(db.capsulaDao().observarPorPieza(id).first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(nombreDb)
            fotos.archivo(ruta)?.delete()
        }
    }
}
