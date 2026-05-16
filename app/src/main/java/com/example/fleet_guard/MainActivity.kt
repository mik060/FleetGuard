package com.example.fleet_guard

import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.maps.MapsInitializer
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.UUID
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this)
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

    // Authentication State Listener
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            firebaseUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    // User Profile Listener (Safe Cleanup)
    DisposableEffect(firebaseUser) {
        var profileListener: ListenerRegistration? = null

        if (firebaseUser != null) {
            val uid = firebaseUser!!.uid
            profileListener = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        if (auth.currentUser != null) {
                            Toast.makeText(context, "Profile Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
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

        onDispose {
            profileListener?.remove()
        }
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Navigation Logic based on Auth State
    LaunchedEffect(loggedInUser, currentRoute) {
        if (loggedInUser != null) {
            val authRoutes = listOf("login", "registration", "admin_registration")
            if (authRoutes.contains(currentRoute)) {
                navController.navigate("dashboard") {
                    popUpTo(currentRoute!!) { inclusive = true }
                }
            }
        } else if (firebaseUser == null) {
            val restrictedRoutes = listOf("dashboard", "trip_summary", "profile", "pending_approvals")
            if (restrictedRoutes.any { currentRoute?.startsWith(it) == true }) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    // Real-time Flows for Data
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
                                    password = doc.id,
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
        
        if (vehicle != null && auth.currentUser != null) {
            scope.launch {
                try {
                    val newPoint = mapOf("lat" to lat, "lng" to lng)
                    firestore.collection("vehicles").document(vehicle.id)
                        .update(
                            "latitude", lat,
                            "longitude", lng,
                            "locationHistory", FieldValue.arrayUnion(newPoint)
                        )
                } catch (_: Exception) { }
            }
        }
    }

    fun approveSchedule(schedule: ScheduleData) {
        scope.launch {
            try {
                val vehicleToUpdate = vehicles.find { "${it.model} (${it.plateNumber})" == schedule.vehicle }
                
                if (vehicleToUpdate == null) {
                    Toast.makeText(context, "Vehicle information not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (vehicleToUpdate.status != "Available") {
                    Toast.makeText(context, "Vehicle is currently ${vehicleToUpdate.status}. Wait until it is Available.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                firestore.collection("schedules").document(schedule.id)
                    .update("status", "APPROVED").await()
                
                firestore.collection("vehicles").document(vehicleToUpdate.id)
                    .update("status", "In Use").await()

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
                currentRoute.startsWith("trip_summary") || 
                currentRoute == "profile" ||
                currentRoute == "pending_approvals"
            )
            if (showBottomBar && loggedInUser != null) {
                NavigationBar(
                    containerColor = Color(0xFF0288D1),
                    tonalElevation = 8.dp
                ) {
                    val navItems = mutableListOf(
                        Triple("Dashboard", "dashboard", Icons.Default.Dashboard),
                        Triple("History", "trip_summary", Icons.Default.History)
                    )
                    
                    if (loggedInUser?.isAdmin == true) {
                        navItems.add(Triple("Approvals", "pending_approvals", Icons.Default.Group))
                    }
                    
                    navItems.add(Triple("Profile", "profile", Icons.Default.Person))
                    
                    navItems.forEach { (label, route, icon) ->
                        val isSelected = navBackStackEntry?.destination?.hierarchy?.any { 
                            it.route == route || (route == "trip_summary" && it.route?.startsWith("trip_summary") == true)
                        } == true
                        
                        NavigationBarItem(
                            icon = { 
                                if (route == "pending_approvals" && pendingUsers.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(pendingUsers.size.toString()) } }) {
                                        Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
                                    }
                                } else {
                                    Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
                                }
                            },
                            label = { 
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent
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
            enterTransition = { 
                slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400))
            },
            exitTransition = { 
                slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400))
            },
            popEnterTransition = { 
                slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400))
            },
            popExitTransition = { 
                slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400))
            }
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
                    onRegisterClick = { navController.navigate("registration") },
                    onAdminRegisterClick = { navController.navigate("admin_registration") }
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
                    onBackToLogin = { navController.popBackStack() }
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
                    onBackToLogin = { navController.popBackStack() }
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
                    onTripLogClick = { navController.navigate("trip_summary") },
                    onTripClick = { trip ->
                        if (loggedInUser?.isAdmin == true) {
                            navController.navigate("trip_summary?tripId=${trip.id}")
                        } else {
                            navController.navigate("trip_summary")
                        }
                    },
                    onViewVehiclesClick = { status -> navController.navigate("available_vehicles/$status") },
                    onProfileClick = { navController.navigate("profile") },
                    onSettingsClick = { navController.navigate("settings") },
                    onReachedDestination = { schedule ->
                        val uid = auth.currentUser?.uid ?: ""
                        val vehicleToUpdate = vehicles.find { "${it.model} (${it.plateNumber})" == schedule.vehicle }
                        val history = vehicleToUpdate?.locationHistory ?: emptyList()

                        // ACCURACY FIX: Sum distance between every recorded point for the TRUE mileage
                        var totalDistanceMeters = 0f
                        if (history.size > 1) {
                            for (i in 0 until history.size - 1) {
                                val p1 = history[i]
                                val p2 = history[i+1]
                                val res = FloatArray(1)
                                Location.distanceBetween(
                                    p1["lat"] ?: 0.0, p1["lng"] ?: 0.0,
                                    p2["lat"] ?: 0.0, p2["lng"] ?: 0.0,
                                    res
                                )
                                totalDistanceMeters += res[0]
                            }
                        } else {
                            // Fallback with road factor if history is missing
                            val res = FloatArray(1)
                            Location.distanceBetween(schedule.startLat, schedule.startLng, schedule.destLat, schedule.destLng, res)
                            totalDistanceMeters = res[0] * 1.35f
                        }

                        val actualMileage = String.format("%.2f km", totalDistanceMeters / 1000)
                        val arrivalTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        val arrivalDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())

                        val newTrip = TripRecord(
                            id = UUID.randomUUID().toString(),
                            driver = schedule.driver,
                            route = schedule.route,
                            destination = schedule.destination,
                            vehicle = schedule.vehicle,
                            date = "$arrivalDate at $arrivalTime",
                            startLat = schedule.startLat,
                            startLng = schedule.startLng,
                            destLat = schedule.destLat,
                            destLng = schedule.destLng,
                            estimatedTime = schedule.estimatedTime,
                            mileage = actualMileage,
                            status = "Returning",
                            userId = uid,
                            adminId = schedule.adminId,
                            timestamp = System.currentTimeMillis(),
                            locationHistory = history
                        )
                        
                        scope.launch {
                            try {
                                firestore.collection("trips").document(newTrip.id).set(newTrip).await()
                                firestore.collection("schedules").document(schedule.id).update("isReached", true).await()
                                if (vehicleToUpdate != null) {
                                    firestore.collection("vehicles").document(vehicleToUpdate.id).update("status", "Returning").await()
                                }
                                Toast.makeText(context, "Trip Logged: $actualMileage", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to log trip: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLocationUpdate = { name, lat, lng -> updateVehicleLocation(name, lat, lng) },
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
                                    firestore.collection("users").document(uid).update("adminId", adminId, "connectionStatus", "PENDING").await()
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
                                firestore.collection("users").document(user.password).update("connectionStatus", "ACCEPTED").await()
                                Toast.makeText(context, "User accepted!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRejectUser = { user ->
                        scope.launch {
                            try {
                                firestore.collection("users").document(user.password).update("adminId", "", "connectionStatus", "ACCEPTED").await()
                                Toast.makeText(context, "User request rejected.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    pendingSchedules = schedules.filter { it.status == "PENDING" },
                    onApproveSchedule = { approveSchedule(it) },
                    onRejectSchedule = { rejectSchedule(it) },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(route = "trip_summary?tripId={tripId}", arguments = listOf(navArgument("tripId") { type = NavType.StringType; nullable = true; defaultValue = null })) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId")
                TripSummaryScreen(
                    user = loggedInUser,
                    onBackClick = { navController.popBackStack() },
                    onCompleteReturn = { trip ->
                        val v = vehicles.find { "${it.model} (${it.plateNumber})" == trip.vehicle }
                        if (v != null) {
                            scope.launch {
                                try {
                                    firestore.collection("vehicles").document(v.id).update("status", "Available", "locationHistory", emptyList<Map<String, Double>>()).await()
                                    firestore.collection("trips").document(trip.id).update("status", "Returned", "returnTimestamp", System.currentTimeMillis()).await()
                                    Toast.makeText(context, "Vehicle is now Available", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val avail = vehicles.filter { it.status == "Available" }.map { "${it.model} (${it.plateNumber})" }
                DriverScheduleFormScreen(
                    user = loggedInUser,
                    availableVehicles = avail,
                    onSaveClick = { driver, route, dest, date, time, veh, reason, sLat, sLng, dLat, dLng, estTime, dist ->
                        val uid = auth.currentUser?.uid ?: ""
                        val new = ScheduleData(
                            id = UUID.randomUUID().toString(),
                            date = date, route = route, destination = dest, driver = driver, time = time, vehicle = veh,
                            reason = reason, startLat = sLat, startLng = sLng, destLat = dLat, destLng = dLng,
                            estimatedTime = estTime, distance = dist, userId = uid, adminId = loggedInUser?.adminId, isReached = false,
                            status = "PENDING", timestamp = System.currentTimeMillis()
                        )
                        scope.launch {
                            try {
                                firestore.collection("schedules").document(new.id).set(new).await()
                                Toast.makeText(context, "Schedule submitted", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("available_vehicles/{status}", arguments = listOf(navArgument("status") { type = NavType.StringType })) { backStackEntry ->
                val status = backStackEntry.arguments?.getString("status") ?: "Available"
                AvailableVehiclesScreen(
                    user = loggedInUser,
                    statusFilter = status,
                    vehicles = vehicles,
                    onBackClick = { navController.popBackStack() },
                    onVehicleClick = { if (loggedInUser?.isAdmin == true) navController.navigate("vehicle_map/${it.id}") },
                    onAddVehicle = { v ->
                        scope.launch {
                            try {
                                firestore.collection("vehicles").document(v.id).set(v).await()
                                Toast.makeText(context, "Vehicle Added", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteVehicle = { v ->
                        scope.launch {
                            try {
                                firestore.collection("vehicles").document(v.id).delete().await()
                                Toast.makeText(context, "Vehicle Removed", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            composable("vehicle_map/{vehicleId}", arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })) { backStackEntry ->
                val vid = backStackEntry.arguments?.getString("vehicleId")
                val v = vehicles.find { it.id == vid }
                if (v != null) {
                    val active = schedules.find { it.vehicle == "${v.model} (${v.plateNumber})" && it.status == "APPROVED" && !it.isReached }
                    VehicleMapScreen(vehicle = v, activeSchedule = active, onBackClick = { navController.popBackStack() })
                }
            }
            composable("profile") {
                ProfileScreen(user = loggedInUser, onLogoutClick = { auth.signOut(); navController.navigate("login") { popUpTo(0) { inclusive = true } } }, onSettingsClick = { navController.navigate("settings") }, onBackClick = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(user = loggedInUser, onBackClick = { navController.popBackStack() })
            }
        }
    }
}
