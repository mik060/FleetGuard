package com.example.fleet_guard.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Geocoder
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleet_guard.data.User
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScheduleFormScreen(
    user: User? = null,
    availableVehicles: List<String> = emptyList(),
    onSaveClick: (String, String, String, String, String, String, String, Double, Double, Double, Double, String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Automatic Date and Time calculation
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

    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)

    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var calculatedKm by remember { mutableStateOf("") }
    var estimatedTime by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }

    // Date Picker Dialog with zero-padding
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = String.format("%02d/%02d/%d", month + 1, dayOfMonth, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            time = String.format("%02d:%02d %s", hour, minute, amPm)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(headerBlue, lightBlue)
                )
            )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "New Schedule",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = headerBlue
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = darkBlue,
                            unfocusedBorderColor = darkBlue.copy(alpha = 0.5f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black,
                            cursorColor = darkBlue,
                            focusedLabelColor = darkBlue,
                            unfocusedLabelColor = darkBlue,
                            disabledLabelColor = darkBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White
                        )

                        Text(
                            "Schedule Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = darkBlue
                        )

                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            readOnly = true // Automatically named by user profile
                        )

                        OutlinedTextField(
                            value = usageReason,
                            onValueChange = { usageReason = it },
                            label = { Text("Reason for Usage", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            placeholder = { Text("e.g. Delivery to Client X") }
                        )

                        OutlinedTextField(
                            value = routeName,
                            onValueChange = { routeName = it },
                            label = { Text("Route / Start Point", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = destinationName,
                            onValueChange = { destinationName = it },
                            label = { Text("Destination", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = darkBlue) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        if (calculatedKm.isNotEmpty()) {
                            Surface(
                                color = darkBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = darkBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Estimated Road Distance: $calculatedKm",
                                            fontWeight = FontWeight.Bold,
                                            color = darkBlue,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Estimated Travel Time: $estimatedTime",
                                            fontWeight = FontWeight.Medium,
                                            color = darkBlue.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Vehicle Selection Dropdown - Fixed crash by using standard Box + DropdownMenu
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedVehicle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Available Vehicle", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = darkBlue) },
                                trailingIcon = { 
                                    Icon(
                                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, 
                                        contentDescription = null, 
                                        tint = darkBlue
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            // Overlay to catch clicks on the entire field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expanded = !expanded }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                if (availableVehicles.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No vehicles available") },
                                        onClick = { expanded = false }
                                    )
                                } else {
                                    availableVehicles.forEach { vehicle ->
                                        DropdownMenuItem(
                                            text = { Text(vehicle) },
                                            onClick = {
                                                selectedVehicle = vehicle
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Date Selector (Now Automatic)
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = { },
                                    label = { Text("AUTO DATE", fontWeight = FontWeight.Black, color = darkBlue) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = darkBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    readOnly = true
                                )
                            }

                            // Time Selector (Now Automatic)
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = { },
                                    label = { Text("AUTO TIME", fontWeight = FontWeight.Black, color = darkBlue) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = darkBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    readOnly = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                if (driverName.isNotEmpty() && usageReason.isNotEmpty() && routeName.isNotEmpty() && destinationName.isNotEmpty() && selectedVehicle.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
                                    scope.launch {
                                        isVerifying = true
                                        try {
                                            val geocoder = Geocoder(context)
                                            val startAddrs = withContext(Dispatchers.IO) { geocoder.getFromLocationName(routeName, 1) }
                                            val destAddrs = withContext(Dispatchers.IO) { geocoder.getFromLocationName(destinationName, 1) }

                                            if (!startAddrs.isNullOrEmpty() && !destAddrs.isNullOrEmpty()) {
                                                val sLat = startAddrs[0].latitude
                                                val sLng = startAddrs[0].longitude
                                                val dLat = destAddrs[0].latitude
                                                val dLng = destAddrs[0].longitude

                                                val results = FloatArray(1)
                                                android.location.Location.distanceBetween(sLat, sLng, dLat, dLng, results)
                                                
                                                // ROAD ACCURACY FACTOR: Google Maps routes are usually ~25-30% longer than straight lines
                                                val roadDistanceKm = (results[0] / 1000) * 1.28
                                                calculatedKm = String.format("%.2f km", roadDistanceKm)

                                                // TIME ESTIMATION: Average City Speed 30 km/h (includes traffic/lights)
                                                val totalMinutes = (roadDistanceKm / 30.0) * 60.0
                                                estimatedTime = if (totalMinutes < 60) {
                                                    "${totalMinutes.toInt()} mins"
                                                } else {
                                                    val hours = (totalMinutes / 60).toInt()
                                                    val mins = (totalMinutes % 60).toInt()
                                                    "$hours hr $mins mins"
                                                }

                                                onSaveClick(driverName, routeName, destinationName, date, time, selectedVehicle, usageReason, sLat, sLng, dLat, dLng, estimatedTime)
                                            } else {
                                                Toast.makeText(context, "Could not find locations. Please be more specific (e.g., City, Street).", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error verifying locations: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isVerifying = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = darkBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("VERIFYING...", fontWeight = FontWeight.Black)
                            } else {
                                Text(
                                    "SAVE SCHEDULE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DriverScheduleFormPreview() {
    Fleet_GuardTheme {
        DriverScheduleFormScreen(availableVehicles = listOf("Toyota Hiace (ABC-1234)"))
    }
}
