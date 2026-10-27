package com.greentycoon.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greentycoon.game.model.GameState
import com.greentycoon.game.ui.theme.*

@Composable
fun TopHud(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Level & XP badge on left
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized Green Fan Leaf Icon
            Canvas(modifier = Modifier.size(46.dp, 36.dp)) {
                val leafPath = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.9f)
                    quadraticTo(size.width * 0.5f, size.height * 0.5f, size.width * 0.9f, size.height * 0.1f)
                    quadraticTo(size.width * 0.6f, size.height * 0.6f, size.width * 0.1f, size.height * 0.9f)
                }
                drawPath(leafPath, color = PlantGreen)
                // Draw 5 radiating leaf blades
                for (angle in listOf(-30f, -15f, 0f, 15f, 30f)) {
                    val p = Path().apply {
                        moveTo(10f, size.height - 10f)
                        quadraticTo(size.width * 0.5f + angle, size.height * 0.4f, size.width * 0.8f + angle * 0.8f, 6f)
                        quadraticTo(size.width * 0.5f + angle * 0.5f, size.height * 0.6f, 10f, size.height - 10f)
                    }
                    drawPath(p, color = PlantLightGreen)
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = "level ${gameState.player.level}",
                    color = HudYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "${gameState.player.currentXp}/${gameState.player.xpForNextLevel}",
                    color = HudYellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }

        // 2. Cash and Stash Inventory on right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stack of Dollar Bills + Cash Count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(34.dp, 24.dp)) {
                    // 100-dollar bills stack
                    drawRoundRect(
                        color = Color(0xFFE0E0E0),
                        topLeft = Offset(4f, 6f),
                        size = Size(28f, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = Color(0xFFC8E6C9),
                        topLeft = Offset(0f, 2f),
                        size = Size(28f, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    drawCircle(color = HudCashGreen, radius = 4f, center = Offset(14f, 10f))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${gameState.player.cash.toInt()}",
                    color = HudWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }

            // Weed Bud Icon + Stash Count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(30.dp, 30.dp)) {
                    // Dense cured flower bud cluster
                    drawCircle(color = PlantBuds, radius = 12f, center = Offset(14f, 14f))
                    drawCircle(color = PlantGreen, radius = 9f, center = Offset(8f, 16f))
                    drawCircle(color = PlantLightGreen, radius = 8f, center = Offset(20f, 16f))
                    drawCircle(color = PlantGoldGlow, radius = 3f, center = Offset(14f, 10f))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${gameState.inventory.totalWeedGrams}",
                    color = HudCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        }
    }
}
