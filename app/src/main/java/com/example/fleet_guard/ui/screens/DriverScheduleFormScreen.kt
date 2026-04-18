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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleet_guard.data.User
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScheduleFormScreen(
    user: User? = null,
    availableVehicles: List<String> = emptyList(),
    onSaveClick: (String, String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onBackClick: () -> Unit = {}
) {
    var driverName by remember { mutableStateOf(user?.fullName ?: "") }
    var routeName by remember { mutableStateOf("") }
    var destinationName by remember { mutableStateOf("") }
    var usageReason by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)

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
                            // Date Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = { },
                                    label = { Text("DATE", fontWeight = FontWeight.Black, color = darkBlue) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = darkBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    readOnly = true,
                                    placeholder = { Text("MM/DD/YYYY", color = Color.Gray) }
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { datePickerDialog.show() }
                                )
                            }

                            // Time Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = { },
                                    label = { Text("TIME", fontWeight = FontWeight.Black, color = darkBlue) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = darkBlue) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    readOnly = true,
                                    placeholder = { Text("HH:MM AM/PM", color = Color.Gray) }
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { timePickerDialog.show() }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                if (driverName.isNotEmpty() && usageReason.isNotEmpty() && routeName.isNotEmpty() && destinationName.isNotEmpty() && selectedVehicle.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
                                    onSaveClick(driverName, routeName, destinationName, date, time, selectedVehicle, usageReason)
                                } else {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                }
                            },
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

@Preview(showBackground = true)
@Composable
fun DriverScheduleFormPreview() {
    Fleet_GuardTheme {
        DriverScheduleFormScreen(availableVehicles = listOf("Toyota Hiace (ABC-1234)"))
    }
}
