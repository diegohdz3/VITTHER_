package com.diego.vitther.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.diego.vitther.MainActivity
import com.diego.vitther.R
import com.diego.vitther.VittherApp
import java.util.concurrent.TimeUnit

const val CANAL_CAPSULAS = "capsulas"
const val EXTRA_PIEZA = "pieza_notificacion"
const val EXTRA_CAPSULA = "capsula_notificacion"

fun programarAvisos(context: Context) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CANAL_CAPSULAS, "Cápsulas de tiempo", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Avisos cuando tus recuerdos están disponibles para abrirse" })
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("revisar-capsulas",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<CapsulaWorker>(1, TimeUnit.HOURS).build())
}

fun revisarAvisos(context: Context) {
    WorkManager.getInstance(context).enqueueUniqueWork("revisar-capsulas-ahora",
        ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<CapsulaWorker>().build())
}

class CapsulaWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        if (!manager.areNotificationsEnabled()) return Result.success()
        val canal = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CANAL_CAPSULAS)
        if (canal?.importance == NotificationManager.IMPORTANCE_NONE) return Result.success()
        val container = (context as VittherApp).container
        return try {
            for (capsula in container.capsulaRepository.pendientesDeAviso()) {
                val pieza = container.piezaRepository.obtenerPorId(capsula.piezaId) ?: continue
                val intent = Intent(context, MainActivity::class.java)
                    .setData(Uri.parse("vitther://capsula/${capsula.id}"))
                    .putExtra(EXTRA_PIEZA, pieza.id)
                    .putExtra(EXTRA_CAPSULA, capsula.id)
                // Un requestCode distinto por cápsula evita depender del data URI
                // para que cada PendingIntent sea único.
                val codigo = (capsula.id xor (capsula.id ushr 32)).toInt()
                val pendiente = PendingIntent.getActivity(context, codigo, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val aviso = NotificationCompat.Builder(context, CANAL_CAPSULAS)
                    .setSmallIcon(R.drawable.ic_notificacion)
                    .setContentTitle("Una cápsula está lista para abrirse")
                    .setContentText("La cápsula de ${pieza.nombre.take(40)} ya puede revelarse.")
                    .setContentIntent(pendiente).setAutoCancel(true).setOnlyAlertOnce(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
                // Identidad estable: un reintento reemplaza el aviso, no crea otro.
                manager.notify("capsula-${capsula.id}", 0, aviso)
                container.capsulaRepository.marcarNotificada(capsula.id)
            }
            Result.success()
        } catch (_: SecurityException) {
            Result.success() // Permiso revocado durante la ejecución: mantener pendientes.
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // No reintentar para siempre ante un error de datos.
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
