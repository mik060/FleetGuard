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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

    var scheduleToConfirm by remember { mutableStateOf<ScheduleData?>(null) }
    var scheduleToShowInfo by remember { mutableStateOf<ScheduleData?>(null) }

    val availableCount = vehicles.count { it.status == "Available" }.toString()
    val inRepairCount = vehicles.count { it.status == "In Repair" }.toString()

    val currentTime = Calendar.getInstance().time
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted = isGranted
    }

    val activeSchedule = schedules.find {
        try {
            val scheduleDate = dateFormat.parse("${it.date} ${it.time}")
            scheduleDate != null && currentTime.after(scheduleDate) && !it.isReached && it.status == "APPROVED"
        } catch (e: Exception) { false }
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
                    kotlinx.coroutines.delay(10000)
                }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

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
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToConfirm = null }) { Text("Cancel") }
            }
        )
    }
    
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
                    DashboardDetailRow(icon = Icons.Default.Schedule, label = "Schedule", value = info?.time ?: "")
                    DashboardDetailRow(icon = Icons.Default.Route, label = "Distance", value = info?.distance ?: "Not Calculated")
                    DashboardDetailRow(icon = Icons.Default.Timer, label = "Est. Travel Time", value = info?.estimatedTime ?: "")
                    DashboardDetailRow(icon = Icons.Default.Info, label = "Status", value = info?.status ?: "PENDING")
                }
            },
            confirmButton = {
                Button(onClick = { scheduleToShowInfo = null }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FLEETGUARD", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = Color.White)
                        Surface(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = if (isAdmin) "ADMIN ACCESS" else "DRIVER ACCESS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0288D1))
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
                    val isConnected = !user?.adminId.isNullOrEmpty() && user?.connectionStatus == "ACCEPTED"
                    Text(if (isConnected) "NEW ENTRY" else "CONNECT TO FLEET", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            val isConnected = !user?.adminId.isNullOrEmpty() && user?.connectionStatus == "ACCEPTED"
            val isPending = !user?.adminId.isNullOrEmpty() && user?.connectionStatus == "PENDING"

            if (isAdmin) {
                item {
                    Text("Admin Monitoring Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = darkBlue, modifier = Modifier.padding(vertical = 8.dp))
                }
                
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernStatusCard(modifier = Modifier.weight(1f), icon = Icons.Default.DirectionsCar, count = availableCount, label = "Available", color = Color(0xFF43A047), onClick = { onViewVehiclesClick("Available") })
                        ModernStatusCard(modifier = Modifier.weight(1f), icon = Icons.Default.Build, count = inRepairCount, label = "In Repair", color = accentRed, onClick = { onViewVehiclesClick("In Repair") })
                    }
                }

                val fleetSchedules = schedules.filter { it.status != "PENDING" }
                if (fleetSchedules.isNotEmpty()) {
                    item {
                        ModernDashboardSection(title = "Active Fleet Schedules", icon = Icons.AutoMirrored.Filled.EventNote) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Limit to 5 recent schedules
                                fleetSchedules.take(5).forEach { item ->
                                    val isScheduledTimeReached = try {
                                        val scheduleDate = dateFormat.parse("${item.date} ${item.time}")
                                        scheduleDate != null && currentTime.after(scheduleDate)
                                    } catch (e: Exception) { false }
                                    val isActive = isScheduledTimeReached && !item.isReached && item.status == "APPROVED"
                                    
                                    ScheduleCard(item = item, isActive = isActive, onInfoClick = { scheduleToShowInfo = item })
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onTripLogClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = darkBlue)
                                ) {
                                    Text("VIEW ALL RECORDS", fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else if (!isConnected) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = if (isPending) Icons.Default.HourglassEmpty else Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(80.dp), tint = darkBlue.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = if (isPending) "Connection Request Pending" else "Welcome to FleetGuard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = darkBlue)
                        Text(text = if (isPending) "Your request has been sent to the Admin. Please wait for their approval." else "Get started by connecting to a fleet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernStatusCard(modifier = Modifier.weight(1f), icon = Icons.Default.DirectionsCar, count = availableCount, label = "Available", color = Color(0xFF43A047), onClick = { onViewVehiclesClick("Available") })
                        ModernStatusCard(modifier = Modifier.weight(1f), icon = Icons.Default.Build, count = inRepairCount, label = "In Repair", color = accentRed, onClick = { onViewVehiclesClick("In Repair") })
                    }
                }

                val userPendingSchedules = schedules.filter { it.status == "PENDING" }
                val userApprovedSchedules = schedules.filter { it.status != "PENDING" }

                if (userPendingSchedules.isNotEmpty()) {
                    item {
                        ModernDashboardSection(title = "Pending Approval", icon = Icons.Default.HourglassEmpty) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                userPendingSchedules.forEach { item ->
                                    ScheduleCard(item = item, isPending = true, onInfoClick = { scheduleToShowInfo = item })
                                }
                            }
                        }
                    }
                }

                if (userApprovedSchedules.isNotEmpty()) {
                    item {
                        ModernDashboardSection(title = "Your Recent Schedules", icon = Icons.AutoMirrored.Filled.EventNote) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Limit to 5 recent schedules
                                userApprovedSchedules.take(5).forEach { item ->
                                    val isScheduledTimeReached = try {
                                        val scheduleDate = dateFormat.parse("${item.date} ${item.time}")
                                        scheduleDate != null && currentTime.after(scheduleDate)
                                    } catch (e: Exception) { false }
                                    val isDriving = isScheduledTimeReached && !item.isReached && item.status == "APPROVED"
                                    
                                    ScheduleCard(item = item, isDriving = isDriving, onInfoClick = { scheduleToShowInfo = item }, onReachedDestination = { scheduleToConfirm = item })
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onTripLogClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = darkBlue)
                                ) {
                                    Text("VIEW ALL RECORDS", fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
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
fun ScheduleCard(
    item: ScheduleData,
    isActive: Boolean = false,
    isDriving: Boolean = false,
    isPending: Boolean = false,
    onInfoClick: () -> Unit,
    onReachedDestination: (() -> Unit)? = null
) {
    val accentRed = Color(0xFFD32F2F)
    val darkBlue = Color(0xFF004D61)
    val primaryColor = when {
        isDriving -> Color.Red
        isActive -> Color(0xFF0277BD)
        isPending -> Color(0xFFF9A825)
        else -> Color.Black
    }
    val cardColor = if (isDriving) Color(0xFFFFEBEE) else if (isActive) Color(0xFFE1F5FE) else Color.White

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (isPending) androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Vehicle & Distance
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.vehicle, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = primaryColor, modifier = Modifier.weight(1f))
                if (item.distance.isNotEmpty()) {
                    Surface(color = primaryColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text(text = item.distance, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timeline Section: Route to Destination
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = item.route, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Box(modifier = Modifier.padding(start = 6.dp).width(1.dp).height(12.dp).background(Color.LightGray))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentRed, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = item.destination, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Details, Date/Time and INFO button
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = item.driver, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "${item.date} | ${item.time}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
                
                Button(
                    onClick = onInfoClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isActive || isDriving) primaryColor else darkBlue)
                ) {
                    Text("INFO", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            if (isDriving && onReachedDestination != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onReachedDestination, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accentRed), shape = RoundedCornerShape(12.dp)) {
                    Text("REACHED DESTINATION", fontWeight = FontWeight.Black)
                }
            }
            
            if (isActive || isPending) {
                Surface(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), color = primaryColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(if(isActive) Icons.Default.MotionPhotosAuto else Icons.Default.HourglassTop, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if(isActive) "IN PROGRESS" else "AWAITING APPROVAL", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
