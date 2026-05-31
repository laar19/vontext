package com.vontext.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vontext.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProcessingNotificationService : android.app.Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra("job_id") ?: return START_NOT_STICKY

        val notification = createNotification(jobId, 0, "Iniciando...")
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    fun updateProgress(jobId: String, progress: Int, message: String) {
        val notification = createNotification(jobId, progress, message)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(jobId: String, progress: Int, message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("job_id", jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Procesando video")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Procesamiento de Video",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra el progreso del procesamiento de video"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "processing_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
