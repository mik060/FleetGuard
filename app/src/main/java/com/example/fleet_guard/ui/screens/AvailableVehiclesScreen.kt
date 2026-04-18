package com.example.fleet_guard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.Vehicle
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableVehiclesScreen(
    user: User? = null,
    statusFilter: String = "Available",
    vehicles: List<Vehicle> = emptyList(),
    onBackClick: () -> Unit = {},
    onVehicleClick: (Vehicle) -> Unit = {},
    onAddVehicle: (Vehicle) -> Unit = {},
    onDeleteVehicle: (Vehicle) -> Unit = {}
) {
    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)

    val isAdmin = user?.isAdmin == true
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredVehicles = if (statusFilter == "All") vehicles else vehicles.filter { it.status == statusFilter }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { model, plate, type, status, reason ->
                onAddVehicle(
                    Vehicle(
                        id = UUID.randomUUID().toString(),
                        model = model,
                        plateNumber = plate,
                        type = type,
                        status = status,
                        maintenanceReason = if (status == "In Repair") reason else null,
                        adminId = user?.adminId
                    )
                )
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (statusFilter == "All") "Fleet List" else statusFilter, 
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = headerBlue)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = darkBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                }
            }
        },
        containerColor = lightBlue
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredVehicles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No vehicles found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredVehicles) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle, 
                            onClick = { onVehicleClick(vehicle) },
                            isAdmin = isAdmin,
                            onDelete = { onDeleteVehicle(vehicle) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 for Available, 1 for Maintenance
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Van") }
    var maintenanceReason by remember { mutableStateOf("") }

    val vehicleTypes = listOf("Van", "Truck", "Pickup", "Car", "Motorcycle")
    val darkBlue = Color(0xFF004D61)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Vehicle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedTab == 0) darkBlue.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = darkBlue
                        ),
                        border = if (selectedTab == 0) BorderStroke(2.dp, darkBlue) else BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Available")
                    }
                    OutlinedButton(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedTab == 1) darkBlue.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = darkBlue
                        ),
                        border = if (selectedTab == 1) BorderStroke(2.dp, darkBlue) else BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Maintenance")
                    }
                }

                OutlinedTextField(
                    value = model, 
                    onValueChange = { model = it }, 
                    label = { Text("Vehicle Name / Model") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = plate, 
                    onValueChange = { plate = it }, 
                    label = { Text("Plate Number") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Vehicle Kind", fontWeight = FontWeight.Bold)
                
                // Simple basic selector to avoid crashes
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        vehicleTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedTab == 1) {
                    OutlinedTextField(
                        value = maintenanceReason, 
                        onValueChange = { maintenanceReason = it }, 
                        label = { Text("Reason for Repair") }, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Engine failure") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (model.isNotEmpty() && plate.isNotEmpty()) {
                        val status = if (selectedTab == 0) "Available" else "In Repair"
                        onConfirm(model, plate, type, status, if (selectedTab == 1) maintenanceReason else null)
                    }
                },
                enabled = model.isNotEmpty() && plate.isNotEmpty() && (selectedTab == 0 || maintenanceReason.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
            ) {
                Text("Add Vehicle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VehicleCard(
    vehicle: Vehicle, 
    onClick: () -> Unit,
    isAdmin: Boolean = false,
    onDelete: () -> Unit = {}
) {
    val statusColor = when (vehicle.status) {
        "Available" -> Color(0xFF43A047)
        "In Use" -> Color(0xFFD32F2F)
        "Returning" -> Color(0xFFF9A825)
        "In Repair" -> Color(0xFF757575)
        else -> Color.Gray
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Vehicle") },
            text = { Text("Are you sure you want to remove ${vehicle.model} (${vehicle.plateNumber}) from the fleet?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar, 
                    contentDescription = null, 
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.model,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Plate: ${vehicle.plateNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "Kind: ${vehicle.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (vehicle.status == "In Repair" && !vehicle.maintenanceReason.isNullOrEmpty()) {
                    Text(
                        text = "Reason: ${vehicle.maintenanceReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = vehicle.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Icon(
                        Icons.Default.LocationOn, 
                        contentDescription = "View Map",
                        tint = Color(0xFF004D61),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
