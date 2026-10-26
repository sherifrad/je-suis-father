package com.example.jesuisfather.ui.onboarding

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jesuisfather.data.model.AllowedApp
import com.example.jesuisfather.ui.theme.AccentBlue
import com.example.jesuisfather.ui.theme.AuthErrorRed
import com.example.jesuisfather.ui.theme.DeviceOwnerActiveBadge
import com.example.jesuisfather.ui.theme.DeviceOwnerInactiveBadge
import com.example.jesuisfather.ui.theme.ParentBackgroundSurface
import com.example.jesuisfather.ui.theme.ParentCardSurface
import com.example.jesuisfather.ui.theme.PrimaryBlue
import com.example.jesuisfather.ui.theme.SecondaryButtonBackground
import com.example.jesuisfather.ui.theme.SubtleTextGrey
import com.example.jesuisfather.ui.theme.SuccessGreen

@Composable
fun OnboardingScreen(
    installedApps: List<AllowedApp>,
    isDeviceOwner: Boolean,
    onSavePin: (pin: String) -> Boolean,
    onCompleteOnboarding: (selectedApps: List<String>) -> Unit,
    onOpenHomeSettings: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val selectedPackages = remember { mutableStateOf(mutableSetOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParentBackgroundSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Je Suis Father 🛡️",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Step $step of 3",
            fontSize = 14.sp,
            color = AccentBlue,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        when (step) {
            1 -> {
                // Step 1: Create 4-digit PIN
                Text(
                    text = "Create Parental PIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "This 4-digit PIN is required to edit settings or exit the child sandbox.",
                    fontSize = 13.sp,
                    color = SubtleTextGrey,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ParentCardSurface,
                        unfocusedContainerColor = ParentCardSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ParentCardSurface,
                        unfocusedContainerColor = ParentCardSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = pinError!!, color = AuthErrorRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (pin.length != 4) {
                            pinError = "PIN must be exactly 4 digits."
                        } else if (pin != confirmPin) {
                            pinError = "PINs do not match."
                        } else {
                            if (onSavePin(pin)) {
                                pinError = null
                                step = 2
                            } else {
                                pinError = "Error saving PIN."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Next: Select Child Apps", fontWeight = FontWeight.Bold)
                }
            }

            2 -> {
                // Step 2: Select Allowed Apps
                Text(
                    text = "Whitelist Initial Apps",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Select at least 1 app your child is permitted to access.",
                    fontSize = 13.sp,
                    color = SubtleTextGrey,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val isChecked = selectedPackages.value.contains(app.packageName)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val set = selectedPackages.value.toMutableSet()
                                        if (checked) set.add(app.packageName) else set.remove(app.packageName)
                                        selectedPackages.value = set
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryBlue,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { step = 3 },
                    enabled = selectedPackages.value.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Next: Launcher Setup", fontWeight = FontWeight.Bold)
                }
            }

            3 -> {
                // Step 3: Default Home & Device Owner info
                Text(
                    text = "System Protection & Kiosk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Set Default Home App",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Set Je Suis Father as your default launcher so pressing Home never exits the sandbox.",
                            color = SubtleTextGrey,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = onOpenHomeSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryButtonBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Home Settings", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentCardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isDeviceOwner) "ADB Device Owner: Enabled ✅" else "ADB Device Owner: Optional",
                            fontWeight = FontWeight.Bold,
                            color = if (isDeviceOwner) DeviceOwnerActiveBadge else DeviceOwnerInactiveBadge,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "To disable notification drag-down completely, run this command once on PC via USB:\nadb shell dpm set-device-owner com.jesuisfather.app/.core.admin.JeSuisFatherAdminReceiver",
                            color = SubtleTextGrey,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onCompleteOnboarding(selectedPackages.value.toList())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Finish & Start Sandbox 🚀", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
