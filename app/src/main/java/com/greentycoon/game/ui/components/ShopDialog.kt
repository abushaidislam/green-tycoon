package com.greentycoon.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.greentycoon.game.model.GameState
import com.greentycoon.game.model.PlantStrain
import com.greentycoon.game.ui.theme.*

@Composable
fun ShopDialog(
    gameState: GameState,
    onDismiss: () -> Unit,
    onBuySeed: (String) -> Unit,
    onBuyWater: () -> Unit,
    onBuyFertilizer: () -> Unit,
    onUnlockPot: (Int) -> Unit,
    onUpgradeLight: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Seeds, 1: Supplies, 2: Upgrades

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF263238),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GROW SHOP",
                        color = HudYellow,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$${gameState.player.cash.toInt()}",
                        color = HudCashGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Seeds, Supplies, Upgrades
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF37474F),
                    contentColor = HudYellow
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Seeds") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Supplies") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Equipment") })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> SeedsTab(gameState, onBuySeed)
                        1 -> SuppliesTab(gameState, onBuyWater, onBuyFertilizer)
                        2 -> EquipmentTab(gameState, onUnlockPot, onUpgradeLight)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SeedsTab(
    gameState: GameState,
    onBuySeed: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PlantStrain.ALL_STRAINS) { strain ->
            val canAfford = gameState.player.cash >= strain.seedPrice
            val isLevelLocked = gameState.player.level < strain.requiredLevel

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF37474F))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strain.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Yield: ~${strain.baseYieldGrams}g | Growth: ${strain.baseGrowDurationSeconds}s",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    if (isLevelLocked) {
                        Text("Requires Level ${strain.requiredLevel}", color = HudYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { onBuySeed(strain.id) },
                    enabled = canAfford && !isLevelLocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HudCashGreen,
                        disabledContainerColor = Color(0xFF78909C)
                    )
                ) {
                    Text("$${strain.seedPrice.toInt()}")
                }
            }
        }
    }
}

@Composable
private fun SuppliesTab(
    gameState: GameState,
    onBuyWater: () -> Unit,
    onBuyFertilizer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Water bottles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF37474F))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Water Bottle Pack (x3)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Current: ${gameState.inventory.waterBottles}", color = Color.LightGray, fontSize = 12.sp)
            }
            Button(
                onClick = onBuyWater,
                enabled = gameState.player.cash >= 25.0,
                colors = ButtonDefaults.buttonColors(containerColor = WaterBlue)
            ) {
                Text("$25")
            }
        }

        // Fertilizer pack
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF37474F))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Growth Fertilizer (x2)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Current: ${gameState.inventory.fertilizers} (1.5x Speed)", color = Color.LightGray, fontSize = 12.sp)
            }
            Button(
                onClick = onBuyFertilizer,
                enabled = gameState.player.cash >= 40.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7CB342))
            ) {
                Text("$40")
            }
        }
    }
}

@Composable
private fun EquipmentTab(
    gameState: GameState,
    onUnlockPot: (Int) -> Unit,
    onUpgradeLight: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Pot 2 Unlock
        val lockedPots = gameState.pots.filter { !it.isUnlocked }
        if (lockedPots.isNotEmpty()) {
            val nextPot = lockedPots.first()
            val unlockCost = when (nextPot.id) {
                1 -> 150.0
                2 -> 450.0
                else -> 1000.0
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF37474F))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Unlock Pot Slot #${nextPot.id + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Grow more plants simultaneously", color = Color.LightGray, fontSize = 12.sp)
                }
                Button(
                    onClick = { onUnlockPot(nextPot.id) },
                    enabled = gameState.player.cash >= unlockCost,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCashGreen)
                ) {
                    Text("$${unlockCost.toInt()}")
                }
            }
        }

        // Light Upgrade
        val currentLight = gameState.roomUpgrades.lightLevel
        if (currentLight < 4) {
            val nextLevel = currentLight + 1
            val lightCost = when (nextLevel) {
                2 -> 250.0
                3 -> 750.0
                else -> 2000.0
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF37474F))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Upgrade Grow Light (Lv $nextLevel)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Accelerates all plant growth rates", color = Color.LightGray, fontSize = 12.sp)
                }
                Button(
                    onClick = { onUpgradeLight(nextLevel) },
                    enabled = gameState.player.cash >= lightCost,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCashGreen)
                ) {
                    Text("$${lightCost.toInt()}")
                }
            }
        }
    }
}
