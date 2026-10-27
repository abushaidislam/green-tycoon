package com.greentycoon.game.ui.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.greentycoon.game.model.GameState
import com.greentycoon.game.model.PotSlot
import com.greentycoon.game.model.RoomSide
import com.greentycoon.game.ui.theme.*

@Composable
fun RoomCanvas(
    gameState: GameState,
    onPotClicked: (Int) -> Unit,
    onDoorClicked: () -> Unit,
    onWaterBottleClicked: () -> Unit,
    onCashRegisterClicked: () -> Unit,
    onSwitchRoomSide: (RoomSide) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RoomAnim")
    val animationTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PlantWindSway"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gameState.currentRoomSide) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val groundY = height * 0.72f

                    // Check navigation arrow clicks
                    if (offset.y > height * 0.85f) {
                        if (gameState.currentRoomSide == RoomSide.PLANTS && offset.x > width * 0.75f) {
                            onSwitchRoomSide(RoomSide.DOOR)
                            return@detectTapGestures
                        } else if (gameState.currentRoomSide == RoomSide.DOOR && offset.x < width * 0.25f) {
                            onSwitchRoomSide(RoomSide.PLANTS)
                            return@detectTapGestures
                        }
                    }

                    // Check cash register click (bottom center)
                    if (offset.x in (width * 0.40f)..(width * 0.60f) && offset.y in (groundY - 10f)..(height * 0.95f)) {
                        onCashRegisterClicked()
                        return@detectTapGestures
                    }

                    if (gameState.currentRoomSide == RoomSide.PLANTS) {
                        // Check water bottle click on left
                        if (offset.x < width * 0.22f && offset.y in (groundY - 160f)..(groundY + 40f)) {
                            onWaterBottleClicked()
                            return@detectTapGestures
                        }

                        // Check pot clicks
                        val pot1X = width * 0.32f
                        val pot2X = width * 0.68f
                        if (offset.x in (pot1X - 70f)..(pot1X + 70f)) {
                            onPotClicked(0)
                        } else if (offset.x in (pot2X - 70f)..(pot2X + 70f)) {
                            onPotClicked(1)
                        }
                    } else {
                        // In DOOR view: click door to interact with customer
                        if (offset.x in (width * 0.55f)..(width * 0.95f) && offset.y in (height * 0.15f)..(groundY)) {
                            onDoorClicked()
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val groundY = height * 0.72f
        val wallSplitY = height * 0.38f

        // 1. Draw Two-tone Wall (Terracotta Top, Beige Bottom)
        drawRect(
            color = WallTerracotta,
            topLeft = Offset(0f, 0f),
            size = Size(width, wallSplitY)
        )
        drawRect(
            color = WallBeige,
            topLeft = Offset(0f, wallSplitY),
            size = Size(width, groundY - wallSplitY)
        )
        // Wall dividing line
        drawLine(
            color = Color(0x33000000),
            start = Offset(0f, wallSplitY),
            end = Offset(width, wallSplitY),
            strokeWidth = 3f
        )

        // 2. Draw Concrete Floor
        drawRect(
            color = FloorConcrete,
            topLeft = Offset(0f, groundY),
            size = Size(width, height - groundY)
        )
        drawLine(
            color = Color(0xFF757575),
            start = Offset(0f, groundY),
            end = Offset(width, groundY),
            strokeWidth = 4f
        )

        // 3. Render content depending on current room view
        if (gameState.currentRoomSide == RoomSide.PLANTS) {
            renderPlantRoom(this, gameState, width, height, groundY, animationTime)
        } else {
            renderDoorRoom(this, gameState, width, height, groundY, animationTime)
        }

        // 4. Draw Cash Register on floor (common to both views)
        drawCashRegister(this, width * 0.50f, groundY + 25f)
    }
}

private fun renderPlantRoom(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    state: GameState,
    width: Float,
    height: Float,
    groundY: Float,
    animationTime: Float
) {
    // Grow light on right tripod
    val lightX = width * 0.88f
    val lightTopY = height * 0.22f
    drawGrowLightStand(scope, lightX, lightTopY, groundY, state.roomUpgrades.lightLevel)

    // Left water bottle stack
    val bottleX = width * 0.12f
    drawWaterBottleStack(scope, bottleX, groundY, state.inventory.waterBottles)

    // Draw Pots 0 and 1
    if (state.pots.isNotEmpty()) {
        val pot1X = width * 0.32f
        PlantCanvas.drawPotAndPlant(scope, state.pots[0], pot1X, groundY, animationTime)
        drawPotStatusOverlay(scope, state.pots[0], pot1X - 70f, groundY - 140f)
    }
    if (state.pots.size > 1) {
        val pot2X = width * 0.65f
        PlantCanvas.drawPotAndPlant(scope, state.pots[1], pot2X, groundY, animationTime)
        drawPotStatusOverlay(scope, state.pots[1], pot2X - 70f, groundY - 140f)
    }

    // Navigation arrow on bottom right pointing to door
    drawNavigationButton(scope, width * 0.90f, height * 0.92f, label = "Door", isLeft = false)
}

private fun renderDoorRoom(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    state: GameState,
    width: Float,
    height: Float,
    groundY: Float,
    animationTime: Float
) {
    // Draw Door Frame and Wooden Door
    val doorLeft = width * 0.55f
    val doorTop = height * 0.10f
    val doorWidth = width * 0.40f
    val doorHeight = groundY - doorTop

    // Door Frame
    scope.drawRect(
        color = DoorTrim,
        topLeft = Offset(doorLeft - 8f, doorTop - 8f),
        size = Size(doorWidth + 16f, doorHeight + 8f)
    )
    // Wooden Door
    scope.drawRect(
        color = DoorWood,
        topLeft = Offset(doorLeft, doorTop),
        size = Size(doorWidth, doorHeight)
    )
    // Door panels
    for (i in 1..4) {
        val panelY = doorTop + (doorHeight / 5) * i
        scope.drawLine(
            color = Color(0x33000000),
            start = Offset(doorLeft + 10f, panelY),
            end = Offset(doorLeft + doorWidth - 10f, panelY),
            strokeWidth = 3f
        )
    }
    // Brass Knob
    scope.drawCircle(
        color = Color(0xFFFFD54F),
        radius = 8f,
        center = Offset(doorLeft + 18f, doorTop + doorHeight * 0.52f)
    )

    // Poster on left wall
    scope.drawRect(
        color = Color(0xFFFFEE58),
        topLeft = Offset(width * 0.10f, height * 0.18f),
        size = Size(width * 0.30f, height * 0.22f)
    )
    scope.drawRect(
        color = Color(0x33000000),
        topLeft = Offset(width * 0.12f, height * 0.20f),
        size = Size(width * 0.26f, height * 0.12f)
    )

    // Customer at door if visitor present
    val visitor = state.currentVisitor
    if (visitor != null) {
        drawVisitorAtDoor(scope, doorLeft + doorWidth * 0.45f, doorTop + doorHeight * 0.35f, visitor)
    }

    // Navigation arrow on bottom left pointing back to plants
    drawNavigationButton(scope, width * 0.10f, height * 0.92f, label = "Plants", isLeft = true)
}

private fun drawGrowLightStand(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    lightX: Float,
    lightTopY: Float,
    groundY: Float,
    lightLevel: Int
) {
    // Tripod legs
    scope.drawLine(color = Color(0xFF212121), start = Offset(lightX, groundY - 60f), end = Offset(lightX - 45f, groundY), strokeWidth = 5f)
    scope.drawLine(color = Color(0xFF212121), start = Offset(lightX, groundY - 60f), end = Offset(lightX + 45f, groundY), strokeWidth = 5f)
    // Vertical stand pole
    scope.drawLine(color = Color(0xFF212121), start = Offset(lightX, lightTopY), end = Offset(lightX, groundY), strokeWidth = 6f)
    // Angled lamp neck
    scope.drawLine(color = Color(0xFF212121), start = Offset(lightX, lightTopY), end = Offset(lightX - 25f, lightTopY - 20f), strokeWidth = 6f)

    // Glowing fluorescent tube lamp
    scope.drawLine(
        color = Color(0xFFFFF9C4),
        start = Offset(lightX - 55f, lightTopY - 45f),
        end = Offset(lightX - 5f, lightTopY + 5f),
        strokeWidth = 14f
    )
    // Radial light cone beam
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x44FFF59D), Color.Transparent),
            center = Offset(lightX - 30f, lightTopY - 20f),
            radius = 160f + lightLevel * 30f
        ),
        radius = 160f + lightLevel * 30f,
        center = Offset(lightX - 30f, lightTopY - 20f)
    )
}

private fun drawWaterBottleStack(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    groundY: Float,
    count: Int
) {
    // Water bottle silhouette
    val bottleY = groundY - 60f
    scope.drawRoundRect(
        color = WaterBlue,
        topLeft = Offset(x - 12f, bottleY),
        size = Size(24f, 50f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    scope.drawRect(
        color = Color(0xFF0288D1),
        topLeft = Offset(x - 6f, bottleY - 8f),
        size = Size(12f, 8f)
    )
}

private fun drawPotStatusOverlay(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    pot: PotSlot,
    x: Float,
    y: Float
) {
    val plant = pot.plant ?: return

    // Moisture gauge bar (Vertical)
    val barHeight = 40f
    val barWidth = 8f
    val fillHeight = barHeight * (plant.moisture / 100f)

    scope.drawRect(
        color = Color(0x66000000),
        topLeft = Offset(x, y),
        size = Size(barWidth, barHeight)
    )
    scope.drawRect(
        color = WaterBlue,
        topLeft = Offset(x, y + barHeight - fillHeight),
        size = Size(barWidth, fillHeight)
    )

    // Timer clock icon
    scope.drawCircle(
        color = Color(0xFFEEEEEE),
        radius = 9f,
        center = Offset(x + 30f, y + 20f)
    )
    scope.drawCircle(
        color = Color(0xFF424242),
        radius = 9f,
        center = Offset(x + 30f, y + 20f),
        style = Stroke(2f)
    )
    // Clock hands
    scope.drawLine(
        color = Color(0xFF212121),
        start = Offset(x + 30f, y + 20f),
        end = Offset(x + 30f, y + 14f),
        strokeWidth = 2f
    )
    scope.drawLine(
        color = Color(0xFF212121),
        start = Offset(x + 30f, y + 20f),
        end = Offset(x + 35f, y + 20f),
        strokeWidth = 2f
    )
}

private fun drawVisitorAtDoor(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    faceX: Float,
    faceY: Float,
    visitor: com.greentycoon.game.model.NpcCustomer
) {
    // Visitor Face
    scope.drawCircle(
        color = Color(0xFFE0BB95),
        radius = 24f,
        center = Offset(faceX, faceY)
    )
    // Sunglasses or Cap depending on template
    if (visitor.avatarId == "mike") {
        // Pink Cap
        scope.drawOval(
            color = Color(0xFFE1BEE7),
            topLeft = Offset(faceX - 22f, faceY - 28f),
            size = Size(44f, 16f)
        )
    } else {
        // Red Sunglasses
        scope.drawRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(faceX - 16f, faceY - 6f),
            size = Size(32f, 10f)
        )
    }

    // Speech bubble path
    val bubbleX = faceX - 110f
    val bubbleY = faceY - 35f
    scope.drawRoundRect(
        color = Color.White,
        topLeft = Offset(bubbleX, bubbleY),
        size = Size(100f, 32f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    // Bubble pointer
    val pointer = Path().apply {
        moveTo(bubbleX + 98f, bubbleY + 12f)
        lineTo(faceX - 18f, faceY - 4f)
        lineTo(bubbleX + 90f, bubbleY + 22f)
        close()
    }
    scope.drawPath(pointer, color = Color.White)
}

private fun drawCashRegister(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    y: Float
) {
    // Register base
    scope.drawRoundRect(
        color = Color(0xFF90A4AE),
        topLeft = Offset(x - 28f, y),
        size = Size(56f, 30f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
    // Display screen
    scope.drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(x - 16f, y - 14f),
        size = Size(32f, 14f)
    )
    // Keypad area
    scope.drawRect(
        color = Color(0xFFCFD8DC),
        topLeft = Offset(x - 22f, y + 4f),
        size = Size(44f, 14f)
    )
}

private fun drawNavigationButton(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    y: Float,
    label: String,
    isLeft: Boolean
) {
    // Board arrow button
    scope.drawRoundRect(
        color = StoolWood,
        topLeft = Offset(x - 30f, y - 15f),
        size = Size(60f, 30f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    // Arrow
    val arrowX = if (isLeft) x - 8f else x + 8f
    scope.drawLine(
        color = Color(0xFF424242),
        start = Offset(x + (if (isLeft) 10f else -10f), y),
        end = Offset(arrowX, y),
        strokeWidth = 4f
    )
}
