package com.shouvick.mapup.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.shouvick.mapup.LocationManager
import com.shouvick.mapup.service.LocationTracingService

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen() {
    val context = LocalContext.current
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
                // User clicked "OK" in the dialog. Start the service now!
                startService()
            } else {
                // User clicked "No". Show a toast or error.
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        fineLocationPermission,
                        postNotification
                    )
                )
            }
        ) {
            Text("Start Getting Coordinates")
        }
    }

}