package com.diego.vitther

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.TestListenableWorkerBuilder
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.notifications.CapsulaWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class CapsulaWorkerTest {
    @Test fun notificaSinRevelarYNoRepite() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as VittherApp
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val piezas = app.container.piezaRepository
        val capsulas = app.container.capsulaRepository
        val piezaId = piezas.guardar(PiezaEntity(nombre = "Worker prueba", franquicia = "Pruebas", fechaAdquisicion = 1700000000000))
        val manager = app.getSystemService(NotificationManager::class.java)
        var id = 0L
        try {
            val apertura = System.currentTimeMillis() + 800
            id = capsulas.sellar(piezaId, "Título privado", "Nota privada", apertura)
            withTimeout(15_000) {
                while (System.currentTimeMillis() < apertura) delay(25)
            }
            val worker = TestListenableWorkerBuilder<CapsulaWorker>(app).build()
            worker.doWork()
            val avisos = manager.activeNotifications.filter { it.tag == "capsula-$id" }
            assertEquals(1, avisos.size)
            assertFalse(avisos.single().notification.extras.toString().contains("Nota privada"))
            assertTrue(capsulas.pendientesDeAviso().none { it.id == id })
            worker.doWork()
            assertEquals(1, manager.activeNotifications.count { it.tag == "capsula-$id" })
        } finally {
            piezas.eliminarPorId(piezaId)
            manager.cancel("capsula-$id", 0)
        }
    }
}
