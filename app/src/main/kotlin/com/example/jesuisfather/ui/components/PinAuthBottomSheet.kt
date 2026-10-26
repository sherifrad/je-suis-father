package com.example.jesuisfather.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jesuisfather.ui.theme.AuthErrorRed
import com.example.jesuisfather.ui.theme.KeypadBorderGrey
import com.example.jesuisfather.ui.theme.KeypadButtonBackground
import com.example.jesuisfather.ui.theme.PrimaryBlue
import com.example.jesuisfather.ui.theme.ParentCardSurface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinAuthBottomSheet(
    remainingLockoutSeconds: Int = 0,
    errorMessage: String? = null,
    onPinEntered: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var enteredDigits by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
        }
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            enteredDigits = ""
            triggerShake()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ParentCardSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Parental Access",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (remainingLockoutSeconds > 0) {
                Text(
                    text = "Keypad locked: try again in ${remainingLockoutSeconds / 60}:${String.format("%02d", remainingLockoutSeconds % 60)}",
                    color = AuthErrorRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = AuthErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4-Dot Progress Indicator with Shake Animation
            Row(
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredDigits.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) PrimaryBlue else Color.Transparent)
                            .border(2.dp, if (isFilled) PrimaryBlue else KeypadBorderGrey, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3x4 Circular Numeric Keypad
            val isLocked = remainingLockoutSeconds > 0
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            KeypadButton(
                                text = key,
                                enabled = !isLocked,
                                onClick = {
                                    when (key) {
                                        "BIO" -> onBiometricClick()
                                        "DEL" -> {
                                            if (enteredDigits.isNotEmpty()) {
                                                enteredDigits = enteredDigits.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (enteredDigits.length < 4) {
                                                val nextDigits = enteredDigits + key
                                                enteredDigits = nextDigits
                                                if (nextDigits.length == 4) {
                                                    onPinEntered(nextDigits)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(KeypadButtonBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val label = when (text) {
            "BIO" -> "👆"
            "DEL" -> "⌫"
            else -> text
        }
        Text(
            text = label,
            fontSize = if (text.length == 1) 24.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else KeypadBorderGrey
        )
    }
}
