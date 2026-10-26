package com.example.jesuisfather.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jesuisfather.data.model.AllowedApp
import com.example.jesuisfather.ui.theme.KeypadBorderGrey
import com.example.jesuisfather.ui.theme.KidsBackgroundSurface
import com.example.jesuisfather.ui.theme.KidsCardAmber
import com.example.jesuisfather.ui.theme.KidsCardBlue
import com.example.jesuisfather.ui.theme.KidsCardGreen
import com.example.jesuisfather.ui.theme.KidsCardOrange
import com.example.jesuisfather.ui.theme.KidsCardPink
import com.example.jesuisfather.ui.theme.KidsCardPurple
import com.example.jesuisfather.ui.theme.ParentCardSurface
import com.example.jesuisfather.ui.theme.ParentLockOverlayBackground
import com.example.jesuisfather.ui.theme.SurfaceDarkElevated

private val CARD_PALETTE = listOf(
    KidsCardBlue,
    KidsCardAmber,
    KidsCardGreen,
    KidsCardPink,
    KidsCardPurple,
    KidsCardOrange
)

@Composable
fun KidsLauncherScreen(
    whitelistedApps: List<AllowedApp>,
    remainingMinutes: Int,
    batteryPercentage: Int = 85,
    onAppClick: (String) -> Unit,
    onParentLockLongPress: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KidsBackgroundSurface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Floating Status Pill
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ Remaining: ${remainingMinutes}m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceDarkElevated
                    )
                    Text(
                        text = "⚡ ${batteryPercentage}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceDarkElevated
                    )
                }
            }

            if (whitelistedApps.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No whitelisted apps configured.\nHold the lock icon to open Parent Dashboard.",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = KeypadBorderGrey
                    )
                }
            } else {
                // 2-Column Oversized App Cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(whitelistedApps) { index, app ->
                        val cardColor = CARD_PALETTE[index % CARD_PALETTE.size]
                        KidAppCard(
                            app = app,
                            backgroundColor = cardColor,
                            onClick = { onAppClick(app.packageName) }
                        )
                    }
                }
            }
        }

        // Parent Mode Lock Button (2-second pointer-input long-press detector)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (showTooltip) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ParentCardSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Hold for 2 seconds for Parent Mode",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ParentLockOverlayBackground)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showTooltip = !showTooltip },
                            onLongPress = {
                                showTooltip = false
                                onParentLockLongPress()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔒", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun KidAppCard(
    app: AllowedApp,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🎮", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.appName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
