package com.greentycoon.game.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.greentycoon.game.audio.SoundManager
import com.greentycoon.game.model.*
import com.greentycoon.game.ui.components.*
import com.greentycoon.game.ui.room.PanoramicRoomView
import com.greentycoon.game.ui.theme.*

@Composable
fun MainGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()

    var showShopDialog by remember { mutableStateOf(false) }
    var showDealingSheet by remember { mutableStateOf(false) }
    var selectedEmptyPotId by remember { mutableStateOf<Int?>(null) }
    var showCashRegisterSummary by remember { mutableStateOf(false) }
    var activeWateringPotId by remember { mutableStateOf<Int?>(null) }

    // Collect sound events from GameViewModel and play through low-latency SoundManager
    LaunchedEffect(Unit) {
        viewModel.soundEvents.collect { event ->
            SoundManager.play(event)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Panoramic Weed Firm Room (Desk, Grow Room Pots, Door & Safe)
        PanoramicRoomView(
            gameState = gameState,
            onPotClicked = { potId ->
                val pot = gameState.pots.getOrNull(potId)
                if (pot != null && pot.isUnlocked) {
                    if (pot.plant == null) {
                        selectedEmptyPotId = potId
                    }
                }
            },
            onDoorClicked = {
                if (gameState.currentVisitor != null) {
                    showDealingSheet = true
                }
            },
            onDeskClicked = {
                showCashRegisterSummary = true
            },
            onUnlockPotClicked = { potId ->
                viewModel.handleAction(GameAction.UnlockPot(potId))
            },
            onUpgradePotClicked = { potId, targetPotType ->
                viewModel.handleAction(GameAction.UpgradePot(potId, targetPotType))
            },
            onWaterPotClicked = { potId ->
                activeWateringPotId = potId
                viewModel.handleAction(GameAction.WaterPot(potId))
            },
            onFertilizePotClicked = { potId ->
                viewModel.handleAction(GameAction.FertilizePot(potId))
            },
            onHarvestPotClicked = { potId ->
                viewModel.handleAction(GameAction.HarvestPot(potId))
            },
            activeWateringPotId = activeWateringPotId,
            onWateringAnimationEnd = { activeWateringPotId = null },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top HUD Overlay
        TopHud(
            gameState = gameState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Notification Banner (if any)
        gameState.bannerNotification?.let { banner ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD000000))
                    .clickable { viewModel.handleAction(GameAction.DismissNotification) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = banner,
                    color = HudYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // 4. Customer Knocking Alert Notification Bar
        if (gameState.currentVisitor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEEFF7043))
                    .clickable {
                        showDealingSheet = true
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Knock at door! 🚪➔",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }

        // 5. Bottom Action Controls
        PotActionRow(
            gameState = gameState,
            onWaterAllClicked = {
                activeWateringPotId = gameState.pots.firstOrNull { it.plant != null }?.id
                for (p in gameState.pots) {
                    if (p.plant != null) {
                        viewModel.handleAction(GameAction.WaterPot(p.id))
                    }
                }
            },
            onFertilizeClicked = {
                for (p in gameState.pots) {
                    if (p.plant != null && !p.plant.fertilizerActive) {
                        viewModel.handleAction(GameAction.FertilizePot(p.id))
                    }
                }
            },
            onHarvestClicked = {
                for (p in gameState.pots) {
                    if (p.plant?.stage == GrowthStage.READY) {
                        viewModel.handleAction(GameAction.HarvestPot(p.id))
                    }
                }
            },
            onShopClicked = {
                showShopDialog = true
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 6. Modal Overlays
        // Shop Dialog
        if (showShopDialog) {
            ShopDialog(
                gameState = gameState,
                onDismiss = { showShopDialog = false },
                onBuySeed = { strainId -> viewModel.handleAction(GameAction.BuySeed(strainId)) },
                onBuyWater = { viewModel.handleAction(GameAction.BuyWaterBottle()) },
                onBuyFertilizer = { viewModel.handleAction(GameAction.BuyFertilizer()) },
                onUnlockPot = { potId -> viewModel.handleAction(GameAction.UnlockPot(potId)) },
                onUpgradeLight = { level -> viewModel.handleAction(GameAction.UpgradeLight(level)) }
            )
        }

        // Dealing Bottom Sheet
        if (showDealingSheet && gameState.currentVisitor != null) {
            DealingSheet(
                visitor = gameState.currentVisitor!!,
                onSellClicked = { price ->
                    viewModel.handleAction(GameAction.BargainDeal(gameState.currentVisitor!!.id, price))
                    viewModel.handleAction(GameAction.AcceptDeal(gameState.currentVisitor!!.id))
                    showDealingSheet = false
                },
                onKickClicked = {
                    viewModel.handleAction(GameAction.KickCustomer(gameState.currentVisitor!!.id))
                    showDealingSheet = false
                }
            )
        }

        // Plant Seed Selection Dialog
        selectedEmptyPotId?.let { potId ->
            PlantSeedDialog(
                seeds = gameState.inventory.seeds,
                onSeedSelected = { strainId ->
                    viewModel.handleAction(GameAction.PlantSeedInPot(potId, strainId))
                    selectedEmptyPotId = null
                },
                onDismiss = { selectedEmptyPotId = null }
            )
        }

        // Cash Register Stats Dialog
        if (showCashRegisterSummary) {
            CashRegisterDialog(
                gameState = gameState,
                onDismiss = { showCashRegisterSummary = false }
            )
        }
    }
}

@Composable
private fun PlantSeedDialog(
    seeds: Map<String, Int>,
    onSeedSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val availableSeeds = seeds.filter { it.value > 0 }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF263238),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Seed to Plant", color = HudYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (availableSeeds.isEmpty()) {
                    Text("You have no seeds in your stash! Buy seeds at the Shop.", color = Color.LightGray)
                } else {
                    availableSeeds.forEach { (strainId, count) ->
                        val strain = PlantStrain.findById(strainId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF37474F))
                                .clickable { onSeedSelected(strainId) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strain.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$count left", color = PlantLightGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CashRegisterDialog(
    gameState: GameState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF263238),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CASH REGISTER", color = HudYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(14.dp))

                Text("Total Cash: $${gameState.player.cash.toInt()}", color = HudWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Total Harvested: ${gameState.player.totalHarvestsGrams}g", color = HudCyan, fontSize = 16.sp)
                Text("Respect Score: ${gameState.player.respectScore}", color = PlantLightGreen, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCashGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
