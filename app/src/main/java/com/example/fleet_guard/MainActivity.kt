package com.example.fleet_guard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
import com.google.firebase.firestore.Query
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
    val userDao = db.userDao()
    
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    // State to keep track of the Firebase User
    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    // State to keep track of the logged-in user profile from Firestore
    var loggedInUser by remember { mutableStateOf<User?>(null) }
    // Loading state for login/registration
    var isLoggingIn by remember { mutableStateOf(false) }

    // Listen for Authentication changes
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            firebaseUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    // Sync profile from Firestore when firebaseUser changes
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
                        loggedInUser = User(
                            fullName = snapshot.getString("fullName") ?: "",
                            email = snapshot.getString("email") ?: "",
                            password = "",
                            isAdmin = snapshot.getBoolean("isAdmin") ?: false,
                            adminId = snapshot.getString("adminId"),
                            connectionStatus = snapshot.getString("connectionStatus")
                        )
                    } else {
                        loggedInUser = null
                    }
                }
        } else {
            loggedInUser = null
        }
    }
    
    // Redirection Logic
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
        }
    }
    
    // State to store schedules - Removed sorting temporarily to avoid index requirement
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
                    if (e != null) {
                        // Error likely due to missing index if sorting was used
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(ScheduleData::class.java)?.copy(id = it.id) }
                            .sortedByDescending { it.timestamp } // Sort in memory instead
                        trySend(list)
                    }
                }
                awaitClose { listener.remove() }
            }
        }
    }.collectAsState(initial = emptyList())
    
    // State to store trip history - Sorted in memory
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

    // State to manage vehicles
    val vehicles by remember(loggedInUser) {
        if (loggedInUser == null || loggedInUser?.adminId == null) {
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

    NavHost(navController = navController, startDestination = "login") {
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
                onRegisterClick = { fullName, email, password, confirmPassword, fleetId ->
                    if (password == confirmPassword && fullName.isNotEmpty() && email.isNotEmpty() && fleetId.isNotEmpty()) {
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
                                        "adminId" to fleetId,
                                        "connectionStatus" to "ACCEPTED"
                                    )
                                    firestore.collection("users").document(uid).set(userData).await()
                                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Registration Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please check your inputs and ensure Fleet ID is provided", Toast.LENGTH_SHORT).show()
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
                                        "connectionStatus" to null
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
                    navController.navigate("schedule_form")
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
                onApproveSchedule = { schedule ->
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
                },
                onRejectSchedule = { schedule ->
                    scope.launch {
                        try {
                            firestore.collection("schedules").document(schedule.id)
                                .update("status", "REJECTED").await()
                            Toast.makeText(context, "Schedule Rejected", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                schedules = schedules,
                tripHistory = tripHistory,
                vehicles = vehicles
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
                onSaveClick = { driver, route, destination, date, time, vehicle ->
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
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
