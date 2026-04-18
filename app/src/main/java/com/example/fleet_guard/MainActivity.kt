package com.example.fleet_guard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fleet_guard.data.AppDatabase
import com.example.fleet_guard.data.User
import com.example.fleet_guard.data.ScheduleData
import com.example.fleet_guard.data.Vehicle
import com.example.fleet_guard.data.TripRecord
import com.example.fleet_guard.ui.screens.*
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Fleet_GuardTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.getDatabase(context)
    
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    var loggedInUser by remember { mutableStateOf<User?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            firebaseUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            val uid = firebaseUser!!.uid
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Profile Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("connectionStatus")
                        val isAdmin = snapshot.getBoolean("isAdmin") ?: false
                        
                        loggedInUser = User(
                            fullName = snapshot.getString("fullName") ?: "",
                            email = snapshot.getString("email") ?: "",
                            password = "",
                            isAdmin = isAdmin,
                            adminId = snapshot.getString("adminId"),
                            connectionStatus = status ?: "ACCEPTED"
                        )
                    } else {
                        loggedInUser = null
                    }
                }
        } else {
            loggedInUser = null
        }
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(loggedInUser, currentRoute) {
        if (loggedInUser != null) {
            val authRoutes = listOf("login", "registration", "admin_registration")
            if (authRoutes.contains(currentRoute)) {
                navController.navigate("dashboard") {
                    popUpTo(currentRoute!!) { inclusive = true }
                }
            }
        } else if (firebaseUser == null) {
            // When completely logged out, ensure we are on login screen
            val restrictedRoutes = listOf("dashboard", "fleet_map", "trip_summary", "profile", "pending_approvals")
            if (restrictedRoutes.any { currentRoute?.startsWith(it) == true }) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    val schedules by remember(loggedInUser) {
        if (loggedInUser == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val uid = auth.currentUser?.uid ?: ""
                val query = if (loggedInUser?.isAdmin == true) {
                    firestore.collection("schedules")
                        .whereEqualTo("adminId", loggedInUser?.adminId)
                } else {
                    firestore.collection("schedules")
                        .whereEqualTo("userId", uid)
                }

                val listener = query.addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(ScheduleData::class.java)?.copy(id = it.id) }
                            .sortedByDescending { it.timestamp }
                        trySend(list)
                    }
                }
                awaitClose { listener.remove() }
            }
        }
    }.collectAsState(initial = emptyList())
    
    val tripHistory by remember(loggedInUser) {
        if (loggedInUser == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val uid = auth.currentUser?.uid ?: ""
                val query = if (loggedInUser?.isAdmin == true) {
                    firestore.collection("trips")
                        .whereEqualTo("adminId", loggedInUser?.adminId)
                } else {
                    firestore.collection("trips")
                        .whereEqualTo("userId", uid)
                }

                val listener = query.addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(TripRecord::class.java)?.copy(id = it.id) }
                            .sortedByDescending { it.timestamp }
                        trySend(list)
                    }
                }
                awaitClose { listener.remove() }
            }
        }
    }.collectAsState(initial = emptyList())

    val vehicles by remember(loggedInUser) {
        if (loggedInUser == null || loggedInUser?.adminId.isNullOrEmpty() || loggedInUser?.connectionStatus != "ACCEPTED") {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val adminIdToQuery = loggedInUser?.adminId
                val listener = firestore.collection("vehicles")
                    .whereEqualTo("adminId", adminIdToQuery)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { it.toObject(Vehicle::class.java)?.copy(id = it.id) }
                            trySend(list)
                        }
                    }
                awaitClose { listener.remove() }
            }
        }
    }.collectAsState(initial = emptyList())

    val pendingUsers by remember(loggedInUser) {
        if (loggedInUser == null || !loggedInUser!!.isAdmin) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val listener = firestore.collection("users")
                    .whereEqualTo("adminId", loggedInUser?.adminId)
                    .whereEqualTo("connectionStatus", "PENDING")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                User(
                                    fullName = doc.getString("fullName") ?: "",
                                    email = doc.getString("email") ?: "",
                                    password = doc.id, // Store doc ID in password field temporarily
                                    isAdmin = false,
                                    adminId = doc.getString("adminId"),
                                    connectionStatus = doc.getString("connectionStatus")
                                )
                            }
                            trySend(list)
                        }
                    }
                awaitClose { listener.remove() }
            }
        }
    }.collectAsState(initial = emptyList())

    fun updateVehicleLocation(vehicleIdOrName: String, lat: Double, lng: Double) {
        val vehicle = vehicles.find { 
            it.id == vehicleIdOrName || "${it.model} (${it.plateNumber})" == vehicleIdOrName 
        }
        
        if (vehicle != null) {
            scope.launch {
                try {
                    firestore.collection("vehicles").document(vehicle.id)
                        .update("latitude", lat, "longitude", lng)
                } catch (_: Exception) { }
            }
        }
    }

    fun approveSchedule(schedule: ScheduleData) {
        scope.launch {
            try {
                firestore.collection("schedules").document(schedule.id)
                    .update("status", "APPROVED").await()
                
                val vehicleToUpdate = vehicles.find { "${it.model} (${it.plateNumber})" == schedule.vehicle }
                if (vehicleToUpdate != null) {
                    firestore.collection("vehicles").document(vehicleToUpdate.id)
                        .update("status", "In Use").await()
                }
                Toast.makeText(context, "Schedule Approved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun rejectSchedule(schedule: ScheduleData) {
        scope.launch {
            try {
                firestore.collection("schedules").document(schedule.id)
                    .update("status", "REJECTED").await()
                Toast.makeText(context, "Schedule Rejected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute != null && (
                currentRoute == "dashboard" || 
                currentRoute == "fleet_map" || 
                currentRoute.startsWith("trip_summary") || 
                currentRoute == "profile" ||
                currentRoute == "pending_approvals"
            )
            if (showBottomBar && loggedInUser != null) {
                NavigationBar(
                    containerColor = Color(0xFF81D4FA),
                    contentColor = Color.White
                ) {
                    val navItems = mutableListOf(
                        Triple("Dashboard", "dashboard", Icons.Default.Dashboard),
                        Triple("Fleet Map", "fleet_map", Icons.Default.Map),
                        Triple("History", "trip_summary", Icons.Default.History)
                    )
                    
                    if (loggedInUser?.isAdmin == true) {
                        navItems.add(Triple("Pending", "pending_approvals", Icons.Default.Group))
                    }
                    
                    navItems.add(Triple("Profile", "profile", Icons.Default.Person))
                    
                    navItems.forEach { (label, route, icon) ->
                        NavigationBarItem(
                            icon = { 
                                if (route == "pending_approvals" && pendingUsers.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(pendingUsers.size.toString()) } }) {
                                        Icon(icon, contentDescription = label)
                                    }
                                } else {
                                    Icon(icon, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any { 
                                it.route == route || (route == "trip_summary" && it.route?.startsWith("trip_summary") == true)
                            } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF004D61),
                                selectedTextColor = Color(0xFF004D61),
                                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                indicatorColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "login",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable("login") {
                LoginScreen(
                    isLoading = isLoggingIn,
                    onLoginClick = { email, password ->
                        isLoggingIn = true
                        scope.launch {
                            try {
                                auth.signInWithEmailAndPassword(email, password).await()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate("registration")
                    },
                    onAdminRegisterClick = {
                        navController.navigate("admin_registration")
                    }
                )
            }
            composable("registration") {
                RegistrationScreen(
                    onRegisterClick = { fullName, email, password, confirmPassword, _ ->
                        if (password == confirmPassword && fullName.isNotEmpty() && email.isNotEmpty()) {
                            isLoggingIn = true
                            scope.launch {
                                try {
                                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                                    val uid = result.user?.uid
                                    if (uid != null) {
                                        val userData = mapOf(
                                            "fullName" to fullName,
                                            "email" to email,
                                            "isAdmin" to false,
                                            "adminId" to "",
                                            "connectionStatus" to "ACCEPTED"
                                        )
                                        firestore.collection("users").document(uid).set(userData).await()
                                        Toast.makeText(context, "Registration Successful!", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Registration Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please check your inputs", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable("admin_registration") {
                AdminRegistrationScreen(
                    onRegisterClick = { fullName, email, password, confirmPassword, registrationId ->
                        if (password == confirmPassword && fullName.isNotEmpty() && email.isNotEmpty()) {
                            isLoggingIn = true
                            scope.launch {
                                try {
                                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                                    val uid = result.user?.uid
                                    if (uid != null) {
                                        val userData = mapOf(
                                            "fullName" to fullName,
                                            "email" to email,
                                            "isAdmin" to true,
                                            "adminId" to registrationId,
                                            "connectionStatus" to "ACCEPTED"
                                        )
                                        firestore.collection("users").document(uid).set(userData).await()
                                        Toast.makeText(context, "Admin Registered Successfully!", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Admin Registration Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please check your inputs", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    user = loggedInUser,
                    onAddClick = {
                        if (loggedInUser?.adminId.isNullOrEmpty() || loggedInUser?.connectionStatus == "PENDING") {
                            navController.navigate("add_admin")
                        } else {
                            navController.navigate("schedule_form")
                        }
                    },
                    onTripLogClick = {
                        navController.navigate("trip_summary")
                    },
                    onTripClick = { trip ->
                        if (loggedInUser?.isAdmin == true) {
                            navController.navigate("trip_summary?tripId=${trip.id}")
                        } else {
                            navController.navigate("trip_summary")
                        }
                    },
                    onViewVehiclesClick = { status ->
                        navController.navigate("available_vehicles/$status")
                    },
                    onProfileClick = {
                        navController.navigate("profile")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onReachedDestination = { schedule ->
                        val uid = auth.currentUser?.uid ?: ""
                        val newTrip = TripRecord(
                            id = UUID.randomUUID().toString(),
                            driver = schedule.driver,
                            route = schedule.route,
                            destination = schedule.destination,
                            vehicle = schedule.vehicle,
                            date = schedule.date,
                            mileage = "${(50..300).random()} km",
                            status = "Returning",
                            userId = uid,
                            adminId = schedule.adminId,
                            timestamp = System.currentTimeMillis()
                        )
                        
                        scope.launch {
                            try {
                                firestore.collection("trips").document(newTrip.id).set(newTrip).await()
                                firestore.collection("schedules").document(schedule.id)
                                    .update("isReached", true).await()
                                
                                val vehicleToUpdate = vehicles.find { "${it.model} (${it.plateNumber})" == schedule.vehicle }
                                if (vehicleToUpdate != null) {
                                    firestore.collection("vehicles").document(vehicleToUpdate.id)
                                        .update("status", "Returning").await()
                                }
                                
                                Toast.makeText(context, "Trip Logged. Vehicle returning to base.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to log trip: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLocationUpdate = { vehicleName, lat, lng ->
                        updateVehicleLocation(vehicleName, lat, lng)
                    },
                    onApproveSchedule = { approveSchedule(it) },
                    onRejectSchedule = { rejectSchedule(it) },
                    pendingUsers = pendingUsers,
                    schedules = schedules,
                    tripHistory = tripHistory,
                    vehicles = vehicles
                )
            }
            composable("add_admin") {
                AddAdminScreen(
                    onAddClick = { adminId ->
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            scope.launch {
                                try {
                                    firestore.collection("users").document(uid)
                                        .update(
                                            "adminId", adminId,
                                            "connectionStatus", "PENDING"
                                        ).await()
                                    Toast.makeText(context, "Request sent to Admin!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("pending_approvals") {
                PendingApprovalsScreen(
                    pendingUsers = pendingUsers,
                    onAcceptUser = { user ->
                        scope.launch {
                            try {
                                firestore.collection("users").document(user.password) // doc.id is stored in password
                                    .update("connectionStatus", "ACCEPTED").await()
                                Toast.makeText(context, "User accepted!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRejectUser = { user ->
                        scope.launch {
                            try {
                                // Clear their admin request on reject
                                firestore.collection("users").document(user.password)
                                    .update(
                                        "adminId", "",
                                        "connectionStatus", "ACCEPTED" // Back to standalone accepted state
                                    ).await()
                                Toast.makeText(context, "User request rejected.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    pendingSchedules = schedules.filter { it.status == "PENDING" },
                    onApproveSchedule = { approveSchedule(it) },
                    onRejectSchedule = { rejectSchedule(it) },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("fleet_map") {
                val activeVehicles = vehicles.filter { it.status == "In Use" || it.status == "Returning" }
                FleetMapScreen(
                    activeVehicles = activeVehicles,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "trip_summary?tripId={tripId}",
                arguments = listOf(navArgument("tripId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId")
                TripSummaryScreen(
                    user = loggedInUser,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCompleteReturn = { trip ->
                        val vehicleToUpdate = vehicles.find { "${it.model} (${it.plateNumber})" == trip.vehicle }
                        if (vehicleToUpdate != null) {
                            scope.launch {
                                try {
                                    firestore.collection("vehicles").document(vehicleToUpdate.id)
                                        .update("status", "Available").await()
                                    
                                    firestore.collection("trips").document(trip.id)
                                        .update("status", "Returned").await()
                                    
                                    Toast.makeText(context, "Vehicle is now Available", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to complete return: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    tripHistory = tripHistory,
                    vehicles = vehicles,
                    initialSelectedTripId = tripId
                )
            }
            composable("schedule_form") {
                val availableVehicleList = vehicles.filter { it.status == "Available" }.map { "${it.model} (${it.plateNumber})" }
                DriverScheduleFormScreen(
                    user = loggedInUser,
                    availableVehicles = availableVehicleList,
                    onSaveClick = { driver, route, destination, date, time, vehicle, reason ->
                        val uid = auth.currentUser?.uid ?: ""
                        val adminId = loggedInUser?.adminId
                        
                        val newSchedule = ScheduleData(
                            id = UUID.randomUUID().toString(),
                            date = date,
                            route = route,
                            destination = destination,
                            driver = driver,
                            time = time,
                            vehicle = vehicle,
                            reason = reason,
                            userId = uid,
                            adminId = adminId,
                            isReached = false,
                            status = "PENDING",
                            timestamp = System.currentTimeMillis()
                        )
                        
                        scope.launch {
                            try {
                                firestore.collection("schedules").document(newSchedule.id).set(newSchedule).await()
                                Toast.makeText(context, "Schedule submitted for approval", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to save schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                "available_vehicles/{status}",
                arguments = listOf(navArgument("status") { type = NavType.StringType })
            ) { backStackEntry ->
                val status = backStackEntry.arguments?.getString("status") ?: "Available"
                AvailableVehiclesScreen(
                    user = loggedInUser,
                    statusFilter = status,
                    vehicles = vehicles,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onVehicleClick = { vehicle ->
                        if (loggedInUser?.isAdmin == true) {
                            navController.navigate("vehicle_map/${vehicle.id}")
                        }
                    },
                    onAddVehicle = { newVehicle ->
                        scope.launch {
                            try {
                                firestore.collection("vehicles").document(newVehicle.id).set(newVehicle).await()
                                Toast.makeText(context, "Vehicle Added to Fleet", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to add vehicle: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteVehicle = { vehicle ->
                        scope.launch {
                            try {
                                firestore.collection("vehicles").document(vehicle.id).delete().await()
                                Toast.makeText(context, "Vehicle removed from fleet", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            composable(
                "vehicle_map/{vehicleId}",
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                val vehicle = vehicles.find { it.id == vehicleId }
                if (vehicle != null) {
                    VehicleMapScreen(
                        vehicle = vehicle,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable("profile") {
                ProfileScreen(
                    user = loggedInUser,
                    onLogoutClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    user = loggedInUser,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
