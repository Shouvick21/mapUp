package com.shouvick.mapup.feature.location.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.shouvick.mapup.LocationManager
import com.shouvick.mapup.feature.location.data.AppDatabase
import com.shouvick.mapup.feature.location.data.TrackingSession
import com.shouvick.mapup.core.service.LocationTracingService
import com.shouvick.mapup.feature.location.ui.utils.calculateDuration
import com.shouvick.mapup.feature.location.ui.utils.formatTimestamp

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(
    navigateToNextScreen: (Long) -> Unit,
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: MainViewModel = viewModel(
        factory = HistoryViewModelFactory(database.trackingDao())
    )
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    val locationManager = remember { LocationManager(context) }
    val fineLocationPermission = remember { Manifest.permission.ACCESS_FINE_LOCATION }
    val postNotification = remember { Manifest.permission.POST_NOTIFICATIONS }
    val activity = context as Activity
    val startService: () -> Unit = remember {
        {
            val intent = Intent(context, LocationTracingService::class.java)
            context.startForegroundService(intent)
        }
    }

    val gpsSettingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startService()
            } else {
                Toast.makeText(context, "GPS is required!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val checkHighAccurecy = remember {
        {
            locationManager.checkLocationSettings(
                onSuccess = {
                    startService()
                },
                onFailure = { exception ->

                    if (exception is ResolvableApiException) {
                        try {
                            val intentSenderRequest = IntentSenderRequest
                                .Builder(exception.resolution)
                                .build()

                            gpsSettingLauncher.launch(intentSenderRequest)

                        } catch (e: Exception) {

                        }
                    }
                }
            )
        }
    }


    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            val fineLocationRuntimeAccepted = isGranted[fineLocationPermission] ?: false
            val notificationRuntimeAccepted = isGranted[postNotification] ?: false
            Log.d(
                "tag",
                "permission fineLocationRuntimeAccepted -> $fineLocationRuntimeAccepted. notificationRuntimeAccepted ->$notificationRuntimeAccepted "
            )
            if (!fineLocationRuntimeAccepted || !notificationRuntimeAccepted) {
                val shouldShowRational =
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                if (!shouldShowRational) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } else {
                checkHighAccurecy()
            }
        }
    MainScreenContent(
        btnClick = {
            permissionLauncher.launch(
                arrayOf(
                    fineLocationPermission,
                    postNotification
                )
            )
        },
        sessions = sessions,
        sessionCardClick = navigateToNextScreen
    )


}

@Composable
fun MainScreenContent(
    btnClick: () -> Unit,
    sessions: List<TrackingSession>,
    sessionCardClick: (Long) -> Unit,
) {

    Scaffold(
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = btnClick
            ) {
                Text("Start Getting Coordinates")
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Trip History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No trips yet. Start tracking!")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session ->
                            SessionItem(session = session, onClick = {
                                sessionCardClick(session.sessionId)
                            })
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SessionItem(session: TrackingSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Session #${session.sessionId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (session.endTime == null) {
                    Text("ACTIVE", color = Color.Green, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column() {
                    Text(text = "Date: ${formatTimestamp(session.startTime)}")
                    Text(
                        text = "Duration: ${
                            calculateDuration(
                                session.startTime,
                                session.endTime
                            )
                        }"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Click",)
            }
        }
    }
}