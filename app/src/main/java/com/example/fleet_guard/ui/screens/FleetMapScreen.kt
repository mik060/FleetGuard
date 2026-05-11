package com.example.fleet_guard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fleet_guard.data.Vehicle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetMapScreen(
    activeVehicles: List<Vehicle>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Phase 1: State Variables
    var startPoint by remember { mutableStateOf<LatLng?>(null) }
    var destinationPoint by remember { mutableStateOf<LatLng?>(null) }
    var destinationName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }

    // Camera state
    val defaultCenter = LatLng(14.5995, 120.9842) // Manila
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 12f)
    }

    // Permission Handler
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation(fusedLocationClient, scope, cameraPositionState) { startPoint = it }
        }
    }

    fun requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation(fusedLocationClient, scope, cameraPositionState) { startPoint = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Phase 2: Geocoder search (Free OS-level conversion)
    fun performSearch(query: String) {
        if (query.length < 3) {
            suggestions = emptyList()
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 5)
                withContext(Dispatchers.Main) {
                    suggestions = results ?: emptyList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    suggestions = emptyList()
                }
            }
        }
    }

    // Phase 3: Distance Calculation (Free OS Utility)
    LaunchedEffect(startPoint, destinationPoint) {
        if (startPoint != null && destinationPoint != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                startPoint!!.latitude, startPoint!!.longitude,
                destinationPoint!!.latitude, destinationPoint!!.longitude,
                results
            )
            distanceKm = results[0].toDouble() / 1000.0 // Convert meters to km
        }
    }

    // Load current location on startup
    LaunchedEffect(Unit) {
        requestLocationUpdate()
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.background(Color(0xFF006064))) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Route Explorer",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { requestLocationUpdate() }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Locate Me", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    
                    // Free Address Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            performSearch(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        placeholder = { Text("Search destination for distance...", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    suggestions = emptyList()
                                    destinationPoint = null
                                    distanceKm = null 
                                }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )
                }
            }
        },
        bottomBar = {
            if (distanceKm != null) {
                BottomAppBar(
                    containerColor = Color(0xFF006064),
                    contentColor = Color.White,
                    modifier = Modifier.height(90.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TOTAL ESTIMATED DISTANCE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                text = String.format("%.2f KM", distanceKm),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                "Air Distance",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                )
            ) {
                // User's Start Point
                startPoint?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Your Start Point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                // Selected Destination
                destinationPoint?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Destination",
                        snippet = destinationName,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // Active Vehicles
                activeVehicles.forEach { vehicle ->
                    val hue = when (vehicle.status) {
                        "In Use" -> BitmapDescriptorFactory.HUE_AZURE
                        "Returning" -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_BLUE
                    }
                    Marker(
                        state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                        title = "${vehicle.model} (${vehicle.plateNumber})",
                        snippet = "Status: ${vehicle.status}",
                        icon = BitmapDescriptorFactory.defaultMarker(hue)
                    )
                }
            }

            // Search Suggestions Overlay
            if (suggestions.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .align(Alignment.TopCenter),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(suggestions) { address ->
                            val line = address.getAddressLine(0) ?: "Unknown Location"
                            ListItem(
                                headlineContent = { Text(line, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.clickable {
                                    val latLng = LatLng(address.latitude, address.longitude)
                                    destinationPoint = latLng
                                    destinationName = line
                                    searchQuery = line
                                    suggestions = emptyList()
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    cameraPositionState: CameraPositionState,
    onLocationFound: (LatLng) -> Unit
) {
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    onLocationFound(userLatLng)
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            }
    } catch (e: SecurityException) {
        // Handle exception
    }
}
