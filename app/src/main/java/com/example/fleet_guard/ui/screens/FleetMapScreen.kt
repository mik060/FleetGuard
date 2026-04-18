package com.example.fleet_guard.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.fleet_guard.data.Vehicle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetMapScreen(
    activeVehicles: List<Vehicle>,
    onBackClick: () -> Unit
) {
    // Default center if no vehicles (Manila as an example)
    val defaultCenter = LatLng(14.5995, 120.9842)
    val initialLocation = if (activeVehicles.isNotEmpty()) {
        LatLng(activeVehicles[0].latitude, activeVehicles[0].longitude)
    } else {
        defaultCenter
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 12f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Fleet Route View",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF006064)
                )
            )
        }
    ) { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
            activeVehicles.forEach { vehicle ->
                val markerColor = when (vehicle.status) {
                    "In Use" -> BitmapDescriptorFactory.HUE_RED
                    "Returning" -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_BLUE
                }
                
                Marker(
                    state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                    title = "${vehicle.model} (${vehicle.plateNumber})",
                    snippet = "Route view for ${vehicle.model}",
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                )
            }
        }
    }
}
