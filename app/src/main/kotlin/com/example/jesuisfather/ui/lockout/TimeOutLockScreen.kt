package com.example.jesuisfather.ui.lockout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jesuisfather.ui.theme.PrimaryUnlockCTA
import com.example.jesuisfather.ui.theme.TimeOutBackground
import com.example.jesuisfather.ui.theme.TimeOutBodyGrey
import com.example.jesuisfather.ui.theme.TimeOutHeadingDark

@Composable
fun TimeOutLockScreen(
    onParentUnlockClick: () -> Unit
) {
    // Intercept back button so child cannot exit lockout
    BackHandler(enabled = true) {
        // Consumed deliberately
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TimeOutBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "😴⏰",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Play Time is Over!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TimeOutHeadingDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Time to rest or ask a parent for more screen time.",
                fontSize = 15.sp,
                color = TimeOutBodyGrey,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onParentUnlockClick,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryUnlockCTA),
                modifier = Modifier
                    .height(52.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Parent Unlock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
