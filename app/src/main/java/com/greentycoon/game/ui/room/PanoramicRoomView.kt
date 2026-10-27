package com.greentycoon.game.ui.room

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.greentycoon.game.R
import com.greentycoon.game.model.*
import com.greentycoon.game.ui.canvas.PlantSpriteResolver
import com.greentycoon.game.ui.theme.*

@Composable
fun PanoramicRoomView(
    gameState: GameState,
    onPotClicked: (Int) -> Unit,
    onDoorClicked: () -> Unit,
    onDeskClicked: () -> Unit,
    onUnlockPotClicked: (Int) -> Unit,
    onUpgradePotClicked: (Int, PotType) -> Unit,
    onWaterPotClicked: (Int) -> Unit,
    onFertilizePotClicked: (Int) -> Unit,
    onHarvestPotClicked: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeWateringPotId: Int? = null,
    onWateringAnimationEnd: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var localWateringPotId by remember { mutableStateOf<Int?>(null) }
    var selectedPotIdForActions by remember { mutableStateOf<Int?>(null) }

    val currentWateringId = activeWateringPotId ?: localWateringPotId
    val selectedPot = gameState.pots.find { it.id == selectedPotIdForActions }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val roomHeight = maxHeight
        // Allocate generous panoramic canvas for widescreen room scrolling
        val totalRoomWidth = (maxHeight * 2.4f).coerceAtLeast(maxWidth * 1.7f)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalRoomWidth)
                .horizontalScroll(scrollState)
        ) {
            // 1. Panoramic Room Background (room_0_a on left, room_0_b on right)
            Row(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.room_0_a),
                    contentDescription = "Room Panorama Left",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Image(
                    painter = painterResource(id = R.drawable.room_0_b),
                    contentDescription = "Room Panorama Right",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // 2. Interactive Stations Overlay
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Station: Desk & Stash (28% width)
                DeskStation(
                    gameState = gameState,
                    onDeskClicked = onDeskClicked,
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight()
                )

                // Center Station: Grow Room & 6 Pots (44% width)
                GrowRoomStation(
                    gameState = gameState,
                    currentWateringPotId = currentWateringId,
                    onPotSelected = { pot ->
                        if (pot.isUnlocked) {
                            if (pot.plant == null) {
                                onPotClicked(pot.id)
                            } else if (pot.plant.stage == GrowthStage.READY) {
                                onHarvestPotClicked(pot.id)
                            } else {
                                selectedPotIdForActions = pot.id
                                onPotClicked(pot.id)
                            }
                        } else {
                            onUnlockPotClicked(pot.id)
                        }
                    },
                    onHarvestClicked = onHarvestPotClicked,
                    onUnlockClicked = onUnlockPotClicked,
                    onWateringEnded = { potId ->
                        if (localWateringPotId == potId) {
                            localWateringPotId = null
                        }
                        onWateringAnimationEnd()
                    },
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                )

                // Right Station: Door & Safe (28% width)
                DoorAndSafeStation(
                    gameState = gameState,
                    onDoorClicked = onDoorClicked,
                    onSafeClicked = onDeskClicked,
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight()
                )
            }
        }
    }

    // Modal dialog for growing pot actions (Water, Fertilize, Upgrade) - updates reactively
    selectedPot?.let { pot ->
        PotActionDialog(
            pot = pot,
            gameState = gameState,
            onWater = {
                localWateringPotId = pot.id
                onWaterPotClicked(pot.id)
                selectedPotIdForActions = null
            },
            onFertilize = {
                onFertilizePotClicked(pot.id)
                selectedPotIdForActions = null
            },
            onUpgradePot = { targetType ->
                onUpgradePotClicked(pot.id, targetType)
                selectedPotIdForActions = null
            },
            onDismiss = { selectedPotIdForActions = null }
        )
    }
}

@Composable
private fun DeskStation(
    gameState: GameState,
    onDeskClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Wooden desk interaction surface
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC3E2723)),
            border = BorderStroke(2.dp, Color(0xFF8D6E63)),
            modifier = Modifier
                .padding(bottom = 75.dp)
                .fillMaxWidth(0.92f)
                .clickable { onDeskClicked() }
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💼 STASH & CASH DESK",
                    color = HudYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stash Jar Graphic
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.desk_weed),
                            contentDescription = "Weed Stash Jar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${gameState.inventory.totalWeedGrams}g Weed",
                            color = PlantLightGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Cash Stack Graphic
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.desk_cash),
                            contentDescription = "Cash Stacks",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${gameState.player.cash.toInt()}",
                            color = HudCashGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to open register stats",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GrowRoomStation(
    gameState: GameState,
    currentWateringPotId: Int?,
    onPotSelected: (PotSlot) -> Unit,
    onHarvestClicked: (Int) -> Unit,
    onUnlockClicked: (Int) -> Unit,
    onWateringEnded: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Overhead Grow Light Fixture & Cone
        GrowLightFixture(lightLevel = gameState.roomUpgrades.lightLevel)

        Spacer(modifier = Modifier.weight(1f))

        // 2. Pots Layout (Authentic rows of pots)
        val potRows = gameState.pots.chunked(3)
        Column(
            modifier = Modifier
                .padding(bottom = 75.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in potRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (pot in row) {
                        PotSlotView(
                            pot = pot,
                            gameState = gameState,
                            isWatering = (currentWateringPotId == pot.id),
                            onPotClicked = { onPotSelected(pot) },
                            onHarvestClicked = { onHarvestClicked(pot.id) },
                            onUnlockClicked = { onUnlockClicked(pot.id) },
                            onWateringEnd = { onWateringEnded(pot.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowLightFixture(lightLevel: Int) {
    val lightConfig = when (lightLevel) {
        1 -> Triple("Incandescent (1.0x)", Color(0xFFFFD54F), Color(0x33FFE082))
        2 -> Triple("Fluorescent (1.25x)", Color(0xFF80DEEA), Color(0x3380DEEA))
        3 -> Triple("LED Full Spec (1.5x)", Color(0xFFE040FB), Color(0x33EA80FC))
        4 -> Triple("HPS Ultra (2.0x)", Color(0xFFFFAB00), Color(0x44FFD700))
        else -> Triple("Incandescent (1.0x)", Color(0xFFFFD54F), Color(0x33FFE082))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
    ) {
        // Metallic lamp hood
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE263238)),
            border = BorderStroke(1.5.dp, lightConfig.second),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(lightConfig.second)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "💡 GROW LIGHT: ${lightConfig.first}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Downward light radiance cone
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(lightConfig.third, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun PotSlotView(
    pot: PotSlot,
    gameState: GameState,
    isWatering: Boolean,
    onPotClicked: () -> Unit,
    onHarvestClicked: () -> Unit,
    onUnlockClicked: () -> Unit,
    onWateringEnd: () -> Unit
) {
    val plant = pot.plant

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .size(width = 110.dp, height = 180.dp)
    ) {
        // 1. Fertilizer Aura Glow
        if (plant?.fertilizerActive == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x66FFD54F), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        // 2. Soil/Ground Sprite (weed_ground)
        Image(
            painter = painterResource(id = R.drawable.weed_ground),
            contentDescription = "Soil",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(bottom = 54.dp)
                .size(width = 72.dp, height = 24.dp)
        )

        // 3. Plant Sprite (authentic Weed Firm growth stage)
        if (plant != null && plant.stage != GrowthStage.EMPTY) {
            val spriteRes = PlantSpriteResolver.getPlantSprite(plant)
            Image(
                painter = painterResource(id = spriteRes),
                contentDescription = plant.strain.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(bottom = 44.dp)
                    .size(width = 90.dp, height = 110.dp)
                    .clickable { onPotClicked() }
            )
        }

        // 4. Pot Base Graphic
        val potBaseRes = PlantSpriteResolver.getPotDrawable(pot.potType)
        Image(
            painter = painterResource(id = potBaseRes),
            contentDescription = pot.potType.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 80.dp, height = 75.dp)
                .then(if (!pot.isUnlocked) Modifier.alpha(0.35f) else Modifier)
                .clickable { onPotClicked() }
        )

        // 5. Moisture Meter for growing plants
        if (pot.isUnlocked && plant != null && plant.stage != GrowthStage.READY) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, bottom = 20.dp)
                    .width(8.dp)
                    .height(48.dp)
                    .background(Color(0x77000000), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((plant.moisture / 100f).coerceIn(0f, 1f))
                        .align(Alignment.BottomCenter)
                        .background(WaterBlue, RoundedCornerShape(4.dp))
                )
            }
        }

        // 6. Pulsating Harvest Shears Badge when READY
        if (pot.isUnlocked && plant?.stage == GrowthStage.READY) {
            val infiniteTransition = rememberInfiniteTransition(label = "HarvestPulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "HarvestScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE2E7D32))
                    .clickable { onHarvestClicked() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.cut),
                        contentDescription = "Harvest Shears",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HARVEST",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 7. Unlocked & Empty Prompt
        if (pot.isUnlocked && plant == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x99000000))
                    .clickable { onPotClicked() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+ Plant Seed",
                    color = HudYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // 8. Locked Pot Overlay
        if (!pot.isUnlocked) {
            val unlockCost = gameState.calculateUnlockCost(pot.id)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xAA000000))
                    .clickable { onUnlockClicked() }
                    .padding(6.dp)
            ) {
                Text("🔒", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pot #${pot.id + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "$${unlockCost.toInt()}",
                    color = HudCashGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        // 9. Authentic Watering Can Animation Overlay
        WateringAnimationOverlay(
            isWatering = isWatering,
            onAnimationEnd = onWateringEnd,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        )
    }
}

@Composable
private fun DoorAndSafeStation(
    gameState: GameState,
    onDoorClicked: () -> Unit,
    onSafeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Wall Safe
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSafeClicked() }
                    .padding(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.safe_close),
                    contentDescription = "Wall Safe",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(90.dp)
                )
                Text(
                    text = "Safe Vault",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Entrance Door & Animated Chime Badge
            Box(contentAlignment = Alignment.TopCenter) {
                // Entrance Door Graphic
                Image(
                    painter = painterResource(id = R.drawable.btn_door),
                    contentDescription = "Entrance Door",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(130.dp)
                        .clickable { onDoorClicked() }
                )

                // Customer Knocking Chime / Alert Badge
                if (gameState.currentVisitor != null) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ChimePulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(450, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ChimeScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = (-36).dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE65100))
                            .clickable { onDoorClicked() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.bell),
                                contentDescription = "Door Bell Chime",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CUSTOMER AT DOOR!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (gameState.currentVisitor != null) "Tap door to deal!" else "Front Entrance",
                color = if (gameState.currentVisitor != null) HudYellow else Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PotActionDialog(
    pot: PotSlot,
    gameState: GameState,
    onWater: () -> Unit,
    onFertilize: () -> Unit,
    onUpgradePot: (PotType) -> Unit,
    onDismiss: () -> Unit
) {
    val plant = pot.plant ?: return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF263238),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "POT SLOT #${pot.id + 1} - ${plant.strain.name}",
                    color = HudYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Growth progress & stage
                Text(
                    text = "Stage: ${plant.stage.name} (${plant.growthProgress.toInt()}%)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (plant.growthProgress / 100f).coerceIn(0f, 1f) },
                    color = PlantLightGreen,
                    trackColor = Color(0xFF37474F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Moisture level
                Text(
                    text = "Moisture: ${plant.moisture.toInt()}%",
                    color = WaterBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fertilizer status
                Text(
                    text = if (plant.fertilizerActive) "⚡ Fertilizer Active (2x Growth)" else "Fertilizer: None",
                    color = if (plant.fertilizerActive) Color(0xFFFFD54F) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action 1: Water
                Button(
                    onClick = onWater,
                    enabled = gameState.inventory.waterBottles > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💧 WATER PLANT (${gameState.inventory.waterBottles} Left)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Fertilize
                Button(
                    onClick = onFertilize,
                    enabled = !plant.fertilizerActive && gameState.inventory.fertilizers > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (plant.fertilizerActive) "ALREADY FERTILIZED"
                        else "⚡ FERTILIZE (${gameState.inventory.fertilizers} Left)"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 3: Upgrade Pot Type (if available)
                val nextPotType = when (pot.potType) {
                    PotType.PLASTIC -> PotType.CERAMIC
                    PotType.CERAMIC -> PotType.CLAY
                    PotType.CLAY -> PotType.HYDRO_POT
                    PotType.HYDRO_POT -> null
                }

                if (nextPotType != null) {
                    Button(
                        onClick = { onUpgradePot(nextPotType) },
                        enabled = gameState.player.cash >= nextPotType.upgradePrice,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🏺 UPGRADE TO ${nextPotType.displayName} ($${nextPotType.upgradePrice.toInt()})")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dismiss
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE", color = Color.LightGray)
                }
            }
        }
    }
}
