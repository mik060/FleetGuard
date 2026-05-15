package com.example.fleet_guard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.Vehicle
import com.example.fleet_guard.data.TripRecord
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    user: User? = null,
    onBackClick: () -> Unit = {},
    onCompleteReturn: (TripRecord) -> Unit = {},
    tripHistory: List<TripRecord> = emptyList(),
    vehicles: List<Vehicle> = emptyList(),
    initialSelectedTripId: String? = null
) {
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)
    val isAdmin = user?.isAdmin == true
    
    var selectedTripId by rememberSaveable { mutableStateOf(initialSelectedTripId) }
    
    var selectedTrip by remember(selectedTripId, tripHistory) { 
        mutableStateOf(tripHistory.find { it.id == selectedTripId })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "TRIP DETAILS", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectedTrip != null) selectedTripId = null else onBackClick() }) {
                        Icon(
                            if (selectedTrip != null) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0288D1))
            )
        },
        containerColor = lightBlue
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTrip != null) {
                // Trip Detail View - Text Only (Map Removed)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF006064), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("FULL TRIP DETAILS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF006064))
                        }
                        
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 2.dp)
                        
                        DetailRow(icon = Icons.Default.Person, label = "Driver", value = selectedTrip?.driver ?: "")
                        DetailRow(icon = Icons.Default.DirectionsCar, label = "Vehicle", value = selectedTrip?.vehicle ?: "")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("TIMELINE & LOGS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                        
                        // Departure Info (Original Date/Time from Schedule)
                        DetailRow(icon = Icons.Default.DirectionsRun, label = "Departure Date", value = selectedTrip?.date?.substringBefore(" at") ?: "")
                        DetailRow(icon = Icons.Default.Schedule, label = "Departure Time", value = selectedTrip?.date?.substringAfter("at ") ?: "")
                        DetailRow(icon = Icons.Default.Home, label = "From (Start)", value = selectedTrip?.route ?: "")
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Arrival Info (Recorded when Reached Destination)
                        DetailRow(icon = Icons.Default.LocationOn, label = "To (Destination)", value = selectedTrip?.destination ?: "")
                        
                        if (selectedTrip?.status == "Returned" && selectedTrip?.returnTimestamp != null) {
                            DetailRow(icon = Icons.Default.EventAvailable, label = "Return Time", value = formatTime(selectedTrip?.returnTimestamp))
                            DetailRow(icon = Icons.Default.Update, label = "Returned", value = formatRelativeTime(selectedTrip?.returnTimestamp))
                        }
                        
                        // Calculated/Actual Stats
                        DetailRow(icon = Icons.Default.Speed, label = "Total Distance", value = selectedTrip?.mileage ?: "")
                        DetailRow(icon = Icons.Default.Timer, label = "Est. Travel Time", value = selectedTrip?.estimatedTime ?: "")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            color = if (selectedTrip?.status == "Returned") Color(0xFF43A047).copy(alpha = 0.1f) else Color(0xFFF9A825).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (selectedTrip?.status == "Returned") Icons.Default.Verified else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (selectedTrip?.status == "Returned") Color(0xFF43A047) else Color(0xFFF9A825),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STATUS: ${selectedTrip?.status?.uppercase()}",
                                    color = if (selectedTrip?.status == "Returned") Color(0xFF43A047) else Color(0xFFF9A825),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        
                        if (selectedTrip?.status == "Returning" && !isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onCompleteReturn(selectedTrip!!) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D61)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPLETE RETURN TO BASE", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            } else {
                // List View
                // Section Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF006064))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAdmin) "Admin: Fleet History" else "Trip History Summary",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                if (tripHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No trip history yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tripHistory) { trip ->
                            TripCard(
                                trip = trip, 
                                onCompleteReturn = onCompleteReturn, 
                                isAdmin = isAdmin,
                                onCardClick = { selectedTripId = trip.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(
    trip: TripRecord, 
    onCompleteReturn: (TripRecord) -> Unit,
    isAdmin: Boolean,
    onCardClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black, // Changed from Gray
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trip.mileage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black, // Extra bold
                    color = Color(0xFFB71C1C) // Slightly darker red for better contrast
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            DetailRow(icon = Icons.Default.AccountCircle, label = "Driver", value = trip.driver)
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(icon = Icons.Default.DirectionsCar, label = "Vehicle", value = trip.vehicle)
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(icon = Icons.Default.Route, label = "Path", value = "${trip.route} -> ${trip.destination}")
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (trip.status) {
                "Returning" -> {
                    if (!isAdmin) {
                        Button(
                            onClick = { onCompleteReturn(trip) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D61)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Returned Successfully")
                        }
                    } else {
                        Surface(
                            color = Color(0xFFF9A825).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF9A825), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vehicle is Returning...",
                                    color = Color(0xFFF9A825),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                "Returned" -> {
                    Surface(
                        color = Color(0xFF43A047).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF43A047), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Successfully Returned to Base",
                                    color = Color(0xFF43A047),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            if (trip.returnTimestamp != null) {
                                Text(
                                    text = "Returned ${formatRelativeTime(trip.returnTimestamp)}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripMapView(currentTrip: TripRecord, vehicles: List<Vehicle>) {
    val vehicle = vehicles.find { "${it.model} (${it.plateNumber})" == currentTrip.vehicle }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(currentTrip.startLat, currentTrip.startLng), 12f)
    }

    // Auto-zoom to fit the whole route
    LaunchedEffect(currentTrip) {
        try {
            val bounds = LatLngBounds.builder()
                .include(LatLng(currentTrip.startLat, currentTrip.startLng))
                .include(LatLng(currentTrip.destLat, currentTrip.destLng))
            
            // Include history points in bounds
            currentTrip.locationHistory.forEach { 
                bounds.include(LatLng(it["lat"] ?: 0.0, it["lng"] ?: 0.0))
            }

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),
                1000
            )
        } catch (e: Exception) {
            // If Maps/CameraUpdateFactory not ready yet, skip animation
            e.printStackTrace()
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        // Start Point
        Marker(
            state = MarkerState(position = LatLng(currentTrip.startLat, currentTrip.startLng)),
            title = "Start Point",
            snippet = currentTrip.route,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        )

        // Destination
        Marker(
            state = MarkerState(position = LatLng(currentTrip.destLat, currentTrip.destLng)),
            title = "Destination",
            snippet = currentTrip.destination,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        // Actual Path Taken
        val historyPoints = currentTrip.locationHistory.map { LatLng(it["lat"] ?: 0.0, it["lng"] ?: 0.0) }
        if (historyPoints.size > 1) {
            Polyline(
                points = historyPoints,
                color = Color(0xFF0288D1),
                width = 12f
            )
        } else {
            // If no history (static view), show a direct line
            Polyline(
                points = listOf(
                    LatLng(currentTrip.startLat, currentTrip.startLng),
                    LatLng(currentTrip.destLat, currentTrip.destLng)
                ),
                color = Color.Gray.copy(alpha = 0.5f),
                width = 8f
            )
        }

        // Current Vehicle Position (if live)
        if (vehicle != null && currentTrip.status == "Returning") {
            Marker(
                state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                title = "Current Position",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            )
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Black // Changed to Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black, // Extra bold
            color = Color.Black // Set to black
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black // Set to black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TripSummaryPreview() {
    Fleet_GuardTheme {
        TripSummaryScreen()
    }
}

fun formatRelativeTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days day${if (days > 1L) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1L) "s" else ""} ago"
        minutes > 0 -> "$minutes minute${if (minutes > 1L) "s" else ""} ago"
        else -> "Just now"
    }
}

fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

