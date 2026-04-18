package com.example.fleet_guard.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.ScheduleData
import com.example.fleet_guard.data.Vehicle
import com.example.fleet_guard.data.TripRecord
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User? = null,
    onAddClick: () -> Unit = {}, 
    onTripLogClick: () -> Unit = {},
    onTripClick: (TripRecord) -> Unit = {},
    onViewVehiclesClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReachedDestination: (ScheduleData) -> Unit = { _ -> },
    onLocationUpdate: (String, Double, Double) -> Unit = { _, _, _ -> },
    onApproveSchedule: (ScheduleData) -> Unit = {},
    onRejectSchedule: (ScheduleData) -> Unit = {},
    pendingUsers: List<User> = emptyList(),
    schedules: List<ScheduleData> = emptyList(),
    tripHistory: List<TripRecord> = emptyList(),
    vehicles: List<Vehicle> = emptyList()
) {
    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)
    val accentRed = Color(0xFFD32F2F)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isAdmin = user?.isAdmin == true

    // Confirmation Dialog for Reached Destination
    var scheduleToConfirm by remember { mutableStateOf<ScheduleData?>(null) }
    
    // Admin View Schedule Info Dialog
    var scheduleToShowInfo by remember { mutableStateOf<ScheduleData?>(null) }

    // Calculate real-time counts
    val availableCount = vehicles.count { it.status == "Available" }.toString()
    val inRepairCount = vehicles.count { it.status == "In Repair" }.toString()

    // Helper to check if schedule time has been reached
    val currentTime = Calendar.getInstance().time
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()) }

    // Location Tracking for Drivers
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
    }

    // Periodically update location if the user is a driver with an active schedule
    val activeSchedule = schedules.find {
        try {
            val scheduleDate = dateFormat.parse("${it.date} ${it.time}")
            scheduleDate != null && currentTime.after(scheduleDate) && !it.isReached && it.status == "APPROVED"
        } catch (e: Exception) {
            false
        }
    }

    LaunchedEffect(locationPermissionGranted, activeSchedule) {
        if (!isAdmin && activeSchedule != null) {
            if (locationPermissionGranted) {
                while (true) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                onLocationUpdate(activeSchedule.vehicle, location.latitude, location.longitude)
                            }
                        }
                    kotlinx.coroutines.delay(10000) // Update every 10 seconds
                }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Reached Destination Confirmation Dialog
    if (scheduleToConfirm != null) {
        AlertDialog(
            onDismissRequest = { scheduleToConfirm = null },
            title = { Text("Confirm Arrival", fontWeight = FontWeight.Bold) },
            text = { Text("Have you reached your destination for the route: ${scheduleToConfirm?.route} to ${scheduleToConfirm?.destination}?") },
            confirmButton = {
                Button(
                    onClick = {
                        scheduleToConfirm?.let { onReachedDestination(it) }
                        scheduleToConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Admin Schedule Info Dialog
    if (scheduleToShowInfo != null) {
        AlertDialog(
            onDismissRequest = { scheduleToShowInfo = null },
            title = { Text("Schedule Information", fontWeight = FontWeight.Bold) },
            text = {
                val info = scheduleToShowInfo
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardDetailRow(icon = Icons.Default.Person, label = "Driver", value = info?.driver ?: "")
                    DashboardDetailRow(icon = Icons.Default.DirectionsCar, label = "Vehicle", value = info?.vehicle ?: "")
                    DashboardDetailRow(icon = Icons.Default.Route, label = "Route", value = info?.route ?: "")
                    DashboardDetailRow(icon = Icons.Default.LocationOn, label = "Destination", value = info?.destination ?: "")
                    DashboardDetailRow(icon = Icons.Default.CalendarToday, label = "Date", value = info?.date ?: "")
                    DashboardDetailRow(icon = Icons.Default.Schedule, label = "Time", value = info?.time ?: "")
                    DashboardDetailRow(icon = Icons.Default.Info, label = "Status", value = info?.status ?: "PENDING")
                }
            },
            confirmButton = {
                Button(onClick = { scheduleToShowInfo = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "FleetGuard", 
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "ROLE: ADMIN" else "ROLE: USER",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = headerBlue
                )
            )
        },
        containerColor = lightBlue,
        floatingActionButton = {
            if (!isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    containerColor = darkBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NEW ENTRY", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            if (isAdmin) {
                item {
                    Text(
                        text = "Admin Monitoring Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = darkBlue,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Status Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernStatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DirectionsCar,
                        count = availableCount,
                        label = "Available",
                        color = Color(0xFF43A047),
                        onClick = { onViewVehiclesClick("Available") }
                    )
                    ModernStatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        count = inRepairCount,
                        label = "In Repair",
                        color = accentRed,
                        onClick = { onViewVehiclesClick("In Repair") }
                    )
                }
            }

            // Admin Dashboard: Fleet Schedules (Global Trip Logs moved to History)
            if (isAdmin) {
                val fleetSchedules = schedules.filter { it.status != "PENDING" }
                if (fleetSchedules.isNotEmpty()) {
                    item {
                        ModernDashboardSection(
                            title = "Active Fleet Schedules",
                            icon = Icons.AutoMirrored.Filled.EventNote
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                fleetSchedules.take(10).forEach { item ->
                                    val isScheduledTimeReached = try {
                                        val scheduleDate = dateFormat.parse("${item.date} ${item.time}")
                                        scheduleDate != null && currentTime.after(scheduleDate)
                                    } catch (e: Exception) { false }
                                    val isActive = isScheduledTimeReached && !item.isReached && item.status == "APPROVED"

                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isActive) Color(0xFFE1F5FE) else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "${item.route} -> ${item.destination}", 
                                                        fontWeight = FontWeight.Bold, 
                                                        fontSize = 16.sp,
                                                        color = if (isActive) Color(0xFF0277BD) else Color.Black
                                                    )
                                                    Text(item.driver, color = Color.Gray, fontSize = 14.sp)
                                                    Text(
                                                        "Vehicle: ${item.vehicle}", 
                                                        color = if (isActive) Color(0xFF0277BD) else darkBlue, 
                                                        fontSize = 12.sp, 
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        item.time, 
                                                        fontWeight = FontWeight.Bold, 
                                                        color = if (isActive) Color(0xFF0277BD) else Color(0xFF004D61)
                                                    )
                                                    Text(item.date, fontSize = 12.sp, color = Color.Gray)
                                                    OutlinedButton(
                                                        onClick = { scheduleToShowInfo = item }, 
                                                        modifier = Modifier.padding(top = 4.dp).height(32.dp), 
                                                        contentPadding = PaddingValues(horizontal = 8.dp), 
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = if (isActive) ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0277BD)) else ButtonDefaults.outlinedButtonColors()
                                                    ) {
                                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("INFO", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                            if (isActive) {
                                                Surface(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    color = Color(0xFF0277BD).copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.MotionPhotosAuto, contentDescription = null, tint = Color(0xFF0277BD), modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("IN PROGRESS", color = Color(0xFF0277BD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // User Dashboard: Trip Logs & Personal Schedules
                item {
                    ModernDashboardSection(
                        title = "Your Recent Trip Logs",
                        icon = Icons.Default.Map
                    ) {
                        if (tripHistory.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.LightGray)
                                    Text("No recent trips recorded", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.padding(8.dp)) {
                                tripHistory.take(3).forEachIndexed { index, trip ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { onTripClick(trip) }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(40.dp).background(if (trip.status == "Returned") Color(0xFF43A047).copy(alpha = 0.1f) else Color(0xFFF9A825).copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(if (trip.status == "Returned") Icons.Default.CheckCircle else Icons.Default.MotionPhotosAuto, contentDescription = null, tint = if (trip.status == "Returned") Color(0xFF43A047) else Color(0xFFF9A825), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${trip.route} -> ${trip.destination}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${trip.driver} • ${trip.vehicle}", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(trip.date, fontSize = 11.sp, color = Color.Gray)
                                            Text(trip.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (trip.status == "Returned") Color(0xFF43A047) else Color(0xFFF9A825))
                                        }
                                    }
                                    if (index < minOf(tripHistory.size, 3) - 1) { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f)) }
                                }
                            }
                        }
                    }
                }

                if (schedules.isNotEmpty()) {
                    item {
                        ModernDashboardSection(
                            title = "Your Recent Schedules",
                            icon = Icons.AutoMirrored.Filled.EventNote
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                schedules.take(10).forEach { item ->
                                    val isScheduledTimeReached = try {
                                        val scheduleDate = dateFormat.parse("${item.date} ${item.time}")
                                        scheduleDate != null && currentTime.after(scheduleDate)
                                    } catch (e: Exception) { false }
                                    val isDriving = isScheduledTimeReached && !item.isReached && item.status == "APPROVED"
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isDriving) Color(0xFFFFEBEE) else Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${item.route} -> ${item.destination}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDriving) Color.Red else Color.Black)
                                                    Text("Vehicle: ${item.vehicle}", color = darkBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(item.time, fontWeight = FontWeight.Medium, color = if (isDriving) Color.Red else Color(0xFF004D61))
                                                    Text(item.date, fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                            if (isDriving) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(onClick = { scheduleToConfirm = item }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(8.dp)) {
                                                    Text("REACHED DESTINATION", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    count: String,
    label: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text(label, fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("VIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModernDashboardSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF006064), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
        }
        content()
    }
}

@Composable
private fun DashboardDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF004D61), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    Fleet_GuardTheme {
        DashboardScreen()
    }
}
