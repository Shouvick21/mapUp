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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shouvick.mapup.Location
import com.shouvick.mapup.LocationManager
import com.shouvick.mapup.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LocationTracingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var trackingJob : Job? = null

    companion object {
        const val CHANNEL_ID = "Location_Coordinates_channel"
        const val STOP_ACTION = "stop_Location_Service"
        const val Notification_ID = 91
    }

    override fun onCreate() {
        super.onCreate()
        initialNotificationChannel()
        locationManager = LocationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == STOP_ACTION) {
            stopSelf()
            return START_NOT_STICKY
        }
        triggerNotification()
        startTracking()

        return START_STICKY
    }

    private fun startTracking() {
        // Launch a coroutine to collect the flow
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            locationManager.getLocationFlow()
                .catch { e ->
                    Log.e("LocationService", "Error fetching location", e)
                }
                .collect { location ->
                    // THIS CODE RUNS CONTINUOUSLY FOR EVERY NEW COORDINATE
                    Log.d("LocationService", "New Location: ${location.lat}, ${location.long}")
                    updateNotification(location = location)
                    // TODO: Send this 'location' to your Server or Database
                }
        }
    }

    private fun initialNotificationChannel() {
        val notificationManger =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location_Coordinates_channel",
                NotificationManager.IMPORTANCE_MIN
            )
            notificationManger.createNotificationChannel(channel)
        }
    }

    private fun triggerNotification() {
        val intent = Intent(this, LocationTracingService::class.java).apply {
            action = STOP_ACTION
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Fetching Location")
            .setContentText("we continuously fetching the updating coordinates")
            .addAction(R.drawable.ic_launcher_background, "Stop Location", pendingIntent)
            .setOngoing(true)

//            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notification_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(Notification_ID, notification)
        }

    }
    private fun updateNotification(location: Location) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = PendingIntent.getService(
            this, 0,
            Intent(this, LocationTracingService::class.java).apply { action = STOP_ACTION },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Tracking Active")
            .setContentText("Lat: ${location.lat}, Lng: ${location.long}")
            .addAction(R.drawable.ic_launcher_background, "Stop", pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // PREVENTS SOUND/VIBRATION ON EVERY UPDATE
            .build()

        notificationManager.notify(Notification_ID, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Notification_ID)
    }
}