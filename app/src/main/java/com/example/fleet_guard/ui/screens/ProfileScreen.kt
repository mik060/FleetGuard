package com.example.fleet_guard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleet_guard.data.User
import com.example.fleet_guard.ui.theme.Fleet_GuardTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val darkBlue = Color(0xFF004D61)
    val lightBlue = Color(0xFFE0F7FA)
    val headerBlue = Color(0xFF81D4FA)

    var showLogoutConfirmation by remember { mutableStateOf(false) }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutConfirmation = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture Placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (user?.isAdmin == true) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = headerBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.fullName ?: "Guest User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = darkBlue
            )
            Text(
                text = user?.email ?: "Not logged in",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Details List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDetailItem(icon = Icons.Default.Badge, label = "User ID", value = user?.id?.toString() ?: "N/A")
                ProfileDetailItem(icon = Icons.Default.Email, label = "Email", value = user?.email ?: "N/A")
                
                if (user?.isAdmin == true && user.adminId != null) {
                    ProfileDetailItem(icon = Icons.Default.Key, label = "Admin ID", value = user.adminId)
                }
                
                ProfileDetailItem(icon = Icons.Default.VerifiedUser, label = "Account Status", value = "Active")
                ProfileDetailItem(icon = Icons.Default.Security, label = "Role", value = if(user?.isAdmin == true) "Administrator" else "Fleet User")
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ProfileDetailItem(icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF004D61), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}
