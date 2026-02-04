package com.shouvick.mapup.feature.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.shouvick.mapup.feature.location.data.AppDatabase
import com.shouvick.mapup.feature.location.data.LocationPoint
import com.shouvick.mapup.feature.location.data.TrackingSession
import com.shouvick.mapup.feature.location.ui.utils.calculateDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: Long) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    var points by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var session by remember { mutableStateOf<TrackingSession?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(sessionId) {
        points = database.trackingDao().getPointsForSession(sessionId)
        session = database.trackingDao().getSessionById(sessionId)

        if (points.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            points.forEach { p ->
                boundsBuilder.include(LatLng(p.latitude, p.longitude))
            }
            try {

                val update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
                cameraPositionState.move(update)
            } catch (e: Exception) {

            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 140.dp,
        sheetContainerColor = Color.White,
        sheetContent = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .padding(horizontal = 16.dp)
            ) {


                session?.let { currentSession ->
                    SessionStatsRow(
                        distance = currentSession.totalDistance,
                        duration = calculateDuration(currentSession.startTime, currentSession.endTime)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Coordinate Logs (${points.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )


                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(points) { point ->
                        LocationPointRow(point)
                    }
                }
            }
        }
    ) { paddingValues ->


        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true)
            ) {
                if (points.isNotEmpty()) {
                    val routeCoordinates = points.map { LatLng(it.latitude, it.longitude) }


                    Polyline(
                        points = routeCoordinates,
                        color = Color.Red,
                        width = 12f
                    )

                    Marker(
                        state = MarkerState(position = routeCoordinates.first()),
                        title = "Start",
                        snippet = formatTimeOnly(points.first().timestamp)
                    )
                    Marker(
                        state = MarkerState(position = routeCoordinates.last()),
                        title = "End",
                        snippet = formatTimeOnly(points.last().timestamp)
                    )
                }
            }
        }
    }
}


@Composable
fun SessionStatsRow(
    distance: Float,
    duration: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.2f km".format(distance / 1000),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(text = "Distance", fontSize = 12.sp, color = Color.Gray)
        }

        Divider(
            modifier = Modifier
                .height(40.dp)
                .width(1.dp),
            color = Color.LightGray
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = duration,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(text = "Duration", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun LocationPointRow(point: LocationPoint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lat: ${point.latitude}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "Lng: ${point.longitude}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimeOnly(point.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "±${point.accuracy.toInt()}m",
                    fontSize = 12.sp,
                    color = Color(0xFF006400)
                )
            }
        }
    }
}

fun formatTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
