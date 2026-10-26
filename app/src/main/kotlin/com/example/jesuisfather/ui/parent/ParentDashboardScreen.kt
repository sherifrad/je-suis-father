package com.example.jesuisfather.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jesuisfather.data.model.AllowedApp
import com.example.jesuisfather.ui.theme.AccentBlue
import com.example.jesuisfather.ui.theme.DeviceOwnerActiveBadge
import com.example.jesuisfather.ui.theme.DeviceOwnerInactiveBadge
import com.example.jesuisfather.ui.theme.ExitKioskRed
import com.example.jesuisfather.ui.theme.KeypadBorderGrey
import com.example.jesuisfather.ui.theme.ParentBackgroundSurface
import com.example.jesuisfather.ui.theme.ParentCardSurface
import com.example.jesuisfather.ui.theme.PrimaryBlue
import com.example.jesuisfather.ui.theme.SubtleTextGrey
import com.example.jesuisfather.ui.theme.SwitchUncheckedTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    isDeviceOwner: Boolean,
    allApps: List<AllowedApp>,
    dailyLimitMinutes: Int,
    onToggleApp: (packageName: String, isEnabled: Boolean) -> Unit,
    onDailyLimitChanged: (minutes: Int) -> Unit,
    onLockAndReturn: () -> Unit,
    onExitKiosk: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentLimit by remember { mutableFloatStateOf(dailyLimitMinutes.toFloat()) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParentBackgroundSurface)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Parent Dashboard",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ParentCardSurface
            ),
            actions = {
                Button(
                    onClick = onLockAndReturn,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Lock & Return", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ADB Device Owner Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = if (isDeviceOwner) "🛡️" else "⚠️", fontSize = 28.sp)
                        Column {
                            Text(
                                text = if (isDeviceOwner) "ADB Device Owner: Active" else "Standard Screen Pinning",
                                fontWeight = FontWeight.Bold,
                                color = if (isDeviceOwner) DeviceOwnerActiveBadge else DeviceOwnerInactiveBadge,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isDeviceOwner) {
                                    "Notification pull-down and system shortcuts are completely locked."
                                } else {
                                    "ADB command needed for enterprise-grade tamper resistance."
                                },
                                fontSize = 12.sp,
                                color = SubtleTextGrey
                            )
                        }
                    }
                }
            }

            // Daily Screen Time Limit Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Screen Time Limit",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            val hours = currentLimit.toInt() / 60
                            val minutes = currentLimit.toInt() % 60
                            Text(
                                text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = currentLimit,
                            onValueChange = {
                                val rounded = ((it / 15f).toInt() * 15).coerceIn(15, 240)
                                currentLimit = rounded.toFloat()
                                onDailyLimitChanged(rounded)
                            },
                            valueRange = 15f..240f,
                            steps = 14
                        )
                    }
                }
            }

            // App Whitelist Search and Header
            item {
                Text(
                    text = "Whitelisted Applications",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed applications...", color = KeypadBorderGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ParentCardSurface,
                        unfocusedContainerColor = ParentCardSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Whitelisted App Items
            items(filteredApps, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = app.packageName,
                                color = KeypadBorderGrey,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { isEnabled ->
                                onToggleApp(app.packageName, isEnabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = SubtleTextGrey,
                                uncheckedTrackColor = SwitchUncheckedTrack
                            )
                        )
                    }
                }
            }

            // Exit Kiosk Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onExitKiosk,
                    colors = ButtonDefaults.buttonColors(containerColor = ExitKioskRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exit to Android Home", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
