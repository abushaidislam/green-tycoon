package com.greentycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greentycoon.game.model.GameState
import com.greentycoon.game.model.GrowthStage
import com.greentycoon.game.ui.theme.*

@Composable
fun PotActionRow(
    gameState: GameState,
    onWaterAllClicked: () -> Unit,
    onFertilizeClicked: () -> Unit,
    onHarvestClicked: () -> Unit,
    onShopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canHarvest = gameState.pots.any { it.plant?.stage == GrowthStage.READY }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            label = "Water",
            badge = "${gameState.inventory.waterBottles}",
            backgroundColor = WaterBlue,
            onClick = onWaterAllClicked
        )

        ActionButton(
            label = "Fertilize",
            badge = "${gameState.inventory.fertilizers}",
            backgroundColor = Color(0xFF7CB342),
            onClick = onFertilizeClicked
        )

        ActionButton(
            label = "Harvest",
            badge = if (canHarvest) "READY" else null,
            backgroundColor = if (canHarvest) PlantGoldGlow else Color(0xFF757575),
            labelColor = if (canHarvest) Color(0xFF212121) else Color.White,
            onClick = onHarvestClicked
        )

        ActionButton(
            label = "Shop",
            badge = "$",
            backgroundColor = Color(0xFF5D4037),
            onClick = onShopClicked
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    badge: String? = null,
    backgroundColor: Color,
    labelColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = labelColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            if (badge != null) {
                Text(
                    text = badge,
                    color = labelColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
