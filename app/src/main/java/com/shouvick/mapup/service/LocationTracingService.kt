package com.shouvick.mapup.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shouvick.mapup.R

class LocationTracingService : Service() {


    companion object {
        const val CHANNEL_ID = "Location_Coordinates_channel"
        const val STOP_ACTION = "stop_Location_Service"
    }

    override fun onCreate() {
        super.onCreate()
        initialNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        triggerNotification()
        if (intent?.action == STOP_ACTION) {
            stopSelf()
        }

        return START_STICKY
    }

    private fun initialNotificationChannel() {
        val notificationManger = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location_Coordinates_channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManger.createNotificationChannel(channel)
        }
    }

    private fun triggerNotification() {
        val intent = Intent(this, LocationTracingService::class.java).apply {
            action = STOP_ACTION
        }
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Fetching Location")
            .setContentText("we continuously fetching the updating coordinates")
            .addAction(R.drawable.ic_launcher_background, "Stop Location", pendingIntent)
            .setOngoing(true)
//            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

    }

    override fun onBind(p0: Intent?): IBinder? = null
}