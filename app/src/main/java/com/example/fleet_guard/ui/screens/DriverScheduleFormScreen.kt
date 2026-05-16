package com.example.fleet_guard.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.OrsApiService
import com.example.fleet_guard.data.RoutingRepository
import com.example.fleet_guard.data.LocationSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScheduleFormScreen(
    user: User? = null,
    availableVehicles: List<String> = emptyList(),
    onSaveClick: (String, String, String, String, String, String, String, Double, Double, Double, Double, String, String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Initialize Routing Repository
    val routingRepository = remember { RoutingRepository(OrsApiService.create()) }
    
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    
    val initialDate = remember { dateFormat.format(calendar.time) }
    val initialTime = remember { timeFormat.format(calendar.time) }

    var driverName by remember { mutableStateOf(user?.fullName ?: "") }
    var routeName by remember { mutableStateOf("") }
    var destinationName by remember { mutableStateOf("") }
    var usageReason by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }

    // Precise Coordinate States (Crucial for accuracy)
    var startLocation by remember { mutableStateOf<LocationSuggestion?>(null) }
    var endLocation by remember { mutableStateOf<LocationSuggestion?>(null) }

    // Suggestion States
    var routeSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var destSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var showRouteDropdown by remember { mutableStateOf(false) }
    var showDestDropdown by remember { mutableStateOf(false) }

    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)

    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var calculatedKm by remember { mutableStateOf("") }
    var estimatedTimeStr by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }

    // Handle Route Suggestions (Filtered to Philippines via API)
    LaunchedEffect(routeName) {
        if (routeName.length >= 3 && routeName != startLocation?.label) {
            delay(500)
            routeSuggestions = routingRepository.getAutocompleteSuggestions(routeName)
            showRouteDropdown = routeSuggestions.isNotEmpty()
        } else if (routeName.length < 3) {
            showRouteDropdown = false
        }
    }

    // Handle Destination Suggestions (Filtered to Philippines via API)
    LaunchedEffect(destinationName) {
        if (destinationName.length >= 3 && destinationName != endLocation?.label) {
            delay(500)
            destSuggestions = routingRepository.getAutocompleteSuggestions(destinationName)
            showDestDropdown = destSuggestions.isNotEmpty()
        } else if (destinationName.length < 3) {
            showDestDropdown = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(headerBlue, lightBlue)))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("New Schedule", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White) },
                    navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = headerBlue)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = darkBlue,
                            unfocusedBorderColor = darkBlue.copy(alpha = 0.5f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = darkBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )

                        Text("Schedule Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = darkBlue)

                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            readOnly = true
                        )

                        OutlinedTextField(
                            value = usageReason,
                            onValueChange = { usageReason = it },
                            label = { Text("Reason for Usage", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )

                        // Route with Suggestions
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = routeName,
                                onValueChange = { 
                                    routeName = it
                                    startLocation = null // Clear precise location if user types manually
                                },
                                label = { Text("Route / Start Point", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = darkBlue) },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true
                            )
                            
                            DropdownMenu(
                                expanded = showRouteDropdown,
                                onDismissRequest = { showRouteDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f),
                                properties = PopupProperties(focusable = false)
                            ) {
                                routeSuggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion.label) },
                                        onClick = {
                                            routeName = suggestion.label
                                            startLocation = suggestion
                                            showRouteDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Destination with Suggestions
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = destinationName,
                                onValueChange = { 
                                    destinationName = it 
                                    endLocation = null // Clear precise location if user types manually
                                },
                                label = { Text("Destination", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = darkBlue) },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true
                            )

                            DropdownMenu(
                                expanded = showDestDropdown,
                                onDismissRequest = { showDestDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f),
                                properties = PopupProperties(focusable = false)
                            ) {
                                destSuggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion.label) },
                                        onClick = {
                                            destinationName = suggestion.label
                                            endLocation = suggestion
                                            showDestDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        if (calculatedKm.isNotEmpty()) {
                            Surface(color = darkBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = darkBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Actual Road Distance: $calculatedKm", fontWeight = FontWeight.Bold, color = darkBlue, fontSize = 14.sp)
                                        Text("Estimated Travel Time: $estimatedTimeStr", fontWeight = FontWeight.Medium, color = darkBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Vehicle Selection
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedVehicle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Available Vehicle", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = darkBlue) },
                                trailingIcon = { Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = darkBlue) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                                if (availableVehicles.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No vehicles available") }, onClick = { expanded = false })
                                } else {
                                    availableVehicles.forEach { vehicle ->
                                        DropdownMenuItem(text = { Text(vehicle) }, onClick = { selectedVehicle = vehicle; expanded = false })
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(value = date, onValueChange = { }, label = { Text("DATE", fontWeight = FontWeight.Bold) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = darkBlue) }, shape = RoundedCornerShape(12.dp), colors = textFieldColors, readOnly = true)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(value = time, onValueChange = { }, label = { Text("TIME", fontWeight = FontWeight.Bold) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = darkBlue) }, shape = RoundedCornerShape(12.dp), colors = textFieldColors, readOnly = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                if (driverName.isNotEmpty() && routeName.isNotEmpty() && destinationName.isNotEmpty() && selectedVehicle.isNotEmpty()) {
                                    scope.launch {
                                        isVerifying = true
                                        try {
                                            // FIX: Only proceed if both points were selected from suggestions
                                            if (startLocation != null && endLocation != null) {
                                                val summary = routingRepository.getRoadDistance(
                                                    startLocation!!.latitude, startLocation!!.longitude,
                                                    endLocation!!.latitude, endLocation!!.longitude
                                                )
                                                
                                                if (summary != null) {
                                                    val roadDistanceKm = summary.distance / 1000.0
                                                    calculatedKm = String.format("%.2f km", roadDistanceKm)
                                                    val totalMinutes = (summary.duration / 60).toInt()
                                                    estimatedTimeStr = if (totalMinutes < 60) "$totalMinutes mins" else "${totalMinutes / 60} hr ${totalMinutes % 60} mins"

                                                    onSaveClick(driverName, routeName, destinationName, date, time, selectedVehicle, usageReason, 
                                                        startLocation!!.latitude, startLocation!!.longitude, 
                                                        endLocation!!.latitude, endLocation!!.longitude, estimatedTimeStr, calculatedKm)
                                                } else {
                                                    Toast.makeText(context, "Routing failed. Please check internet.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Please select locations from the suggestions list to ensure accuracy.", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isVerifying = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = darkBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("CALCULATING ROAD ROUTE...", fontWeight = FontWeight.Black)
                            } else {
                                Text("SAVE SCHEDULE", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
