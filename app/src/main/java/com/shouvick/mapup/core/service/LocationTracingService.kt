package com.shouvick.mapup.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shouvick.mapup.Location
import com.shouvick.mapup.LocationManager
import com.shouvick.mapup.R
import com.shouvick.mapup.feature.location.data.AppDatabase
import com.shouvick.mapup.feature.location.data.LocationPoint
import com.shouvick.mapup.feature.location.data.TrackingSession
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


    private lateinit var database: AppDatabase
    private var currentSessionId: Long = -1L

    companion object {
        const val CHANNEL_ID = "Location_Coordinates_channel"
        const val STOP_ACTION = "stop_Location_Service"
        const val Notification_ID = 91
    }

    override fun onCreate() {
        super.onCreate()
        initialNotificationChannel()
        locationManager = LocationManager(this)

        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == STOP_ACTION) {
            stopTrackingSession()
            stopSelf()
            return START_NOT_STICKY
        }

        if (currentSessionId == -1L) {
            createNewSession()
        }

        triggerNotification()
        startTracking()

        return START_STICKY
    }

    private fun startTracking() {

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            locationManager.getLocationFlow()
                .catch { e ->
                    Log.e("LocationService", "Error fetching location", e)
                }
                .collect { location ->
                    if (currentSessionId != -1L) {
                        val point = LocationPoint(
                            sessionOwnerId = currentSessionId,
                            latitude = location.lat.toDouble(),
                            longitude = location.long.toDouble(),
                            accuracy = 0f
                        )
                        database.trackingDao().insertLocation(point)
                        Log.d("LocationService", "Saved point to Session $currentSessionId")
                    }

                    Log.d("LocationService", "New Location: ${location.lat}, ${location.long}")
                    updateNotification(location = location)
                }
        }
    }

    private fun createNewSession() {
        serviceScope.launch {

            val newSession = TrackingSession(startTime = System.currentTimeMillis())
            currentSessionId = database.trackingDao().insertSession(newSession)
            Log.d("LocationService", "Started Session ID: $currentSessionId")
        }
    }

    private fun stopTrackingSession() {
        serviceScope.launch {
            if (currentSessionId != -1L) {

                val session = database.trackingDao().getSessionById(currentSessionId)
                session?.let {
                    val endedSession = it.copy(endTime = System.currentTimeMillis())
                    database.trackingDao().updateSession(endedSession)
                }
                currentSessionId = -1L
            }
        }
    }

    private fun initialNotificationChannel() {
        val notificationManger =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notification_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(Notification_ID, notification)
        }

    }
    private fun updateNotification(location: Location) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(Notification_ID, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Notification_ID)
    }
}