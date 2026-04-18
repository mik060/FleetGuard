package com.example.fleet_guard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.Vehicle
import com.example.fleet_guard.data.TripRecord
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
    
    var selectedTrip by remember(initialSelectedTripId, tripHistory) { 
        mutableStateOf(tripHistory.find { it.id == initialSelectedTripId })
    }

    // For User: AlertDialog when a trip is selected
    if (selectedTrip != null && !isAdmin) {
        AlertDialog(
            onDismissRequest = { selectedTrip = null },
            title = { Text("Trip Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(icon = Icons.Default.Person, label = "Driver", value = selectedTrip?.driver ?: "")
                    DetailRow(icon = Icons.Default.DirectionsCar, label = "Vehicle", value = selectedTrip?.vehicle ?: "")
                    DetailRow(icon = Icons.Default.Route, label = "Start", value = selectedTrip?.route ?: "")
                    DetailRow(icon = Icons.Default.LocationOn, label = "Destination", value = selectedTrip?.destination ?: "")
                    DetailRow(icon = Icons.Default.CalendarToday, label = "Date", value = selectedTrip?.date ?: "")
                    DetailRow(icon = Icons.Default.Speed, label = "Mileage", value = selectedTrip?.mileage ?: "")
                    DetailRow(icon = Icons.Default.Info, label = "Status", value = selectedTrip?.status ?: "")
                }
            },
            confirmButton = {
                Button(onClick = { selectedTrip = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "FleetGuard", 
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectedTrip != null && isAdmin) selectedTrip = null else onBackClick() }) {
                        Icon(
                            if (selectedTrip != null && isAdmin) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = headerBlue)
            )
        },
        containerColor = lightBlue
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTrip != null && isAdmin) {
                // Admin Split View: Info on top, Map on bottom
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Half: Information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF006064))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Trip Tracking", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF006064))
                            }
                            
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                            
                            DetailRow(icon = Icons.Default.Person, label = "Driver", value = selectedTrip?.driver ?: "")
                            DetailRow(icon = Icons.Default.DirectionsCar, label = "Vehicle", value = selectedTrip?.vehicle ?: "")
                            DetailRow(icon = Icons.Default.Route, label = "Start", value = selectedTrip?.route ?: "")
                            DetailRow(icon = Icons.Default.LocationOn, label = "Destination", value = selectedTrip?.destination ?: "")
                            DetailRow(icon = Icons.Default.CalendarToday, label = "Date", value = selectedTrip?.date ?: "")
                            DetailRow(icon = Icons.Default.Speed, label = "Mileage", value = selectedTrip?.mileage ?: "")
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Surface(
                                color = if (selectedTrip?.status == "Returned") Color(0xFF43A047).copy(alpha = 0.1f) else Color(0xFFF9A825).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Status: ${selectedTrip?.status}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (selectedTrip?.status == "Returned") Color(0xFF43A047) else Color(0xFFF9A825),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Bottom Half: Map
                    Box(modifier = Modifier.weight(0.55f)) {
                        val currentTrip = selectedTrip
                        val vehicle = if (currentTrip != null) {
                            vehicles.find { "${it.model} (${it.plateNumber})" == currentTrip.vehicle }
                        } else null

                        val tripLocation = if (vehicle != null) LatLng(vehicle.latitude, vehicle.longitude) else LatLng(14.5995, 120.9842)
                        
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(tripLocation, 15f)
                        }
                        
                        // Update camera if vehicle moves
                        LaunchedEffect(vehicle?.latitude, vehicle?.longitude) {
                            if (vehicle != null) {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(vehicle.latitude, vehicle.longitude), 
                                    cameraPositionState.position.zoom
                                )
                            }
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = true)
                        ) {
                            if (vehicle != null) {
                                Marker(
                                    state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                                    title = vehicle.model,
                                    snippet = "Driver: ${selectedTrip?.driver}"
                                )
                            }
                        }
                        
                        if (vehicle == null) {
                            Surface(
                                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Vehicle location data unavailable",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
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
                                onCardClick = { selectedTrip = trip }
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
                    color = Color.Gray
                )
                Text(
                    text = trip.mileage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDE4444)
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
                        Row(
                            modifier = Modifier.padding(8.dp),
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
                    }
                }
            }
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
            tint = Color(0xFF004D61)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
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
