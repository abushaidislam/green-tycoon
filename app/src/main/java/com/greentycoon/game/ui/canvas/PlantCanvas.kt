package com.greentycoon.game.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.greentycoon.game.model.GrowthStage
import com.greentycoon.game.model.PlantInstance
import com.greentycoon.game.model.PotSlot
import com.greentycoon.game.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

object PlantCanvas {

    /**
     * Draws the wooden stool, pot, soil, and procedural plant.
     */
    fun drawPotAndPlant(
        scope: DrawScope,
        potSlot: PotSlot,
        centerX: Float,
        groundY: Float,
        animationTime: Float
    ) {
        val stoolWidth = 130f
        val stoolHeight = 120f
        val stoolTopY = groundY - stoolHeight
        val potWidth = 90f
        val potHeight = 85f
        val potRimY = stoolTopY - potHeight

        // 1. Draw Wooden Stool (Legs and Top Seat)
        drawStool(scope, centerX, stoolTopY, stoolWidth, groundY)

        if (!potSlot.isUnlocked) {
            // Locked pot placeholder
            scope.drawCircle(
                color = Color(0x66000000),
                radius = 30f,
                center = Offset(centerX, stoolTopY - 40f)
            )
            return
        }

        // 2. Draw Terracotta Pot and Soil
        val moisture = potSlot.plant?.moisture ?: 100f
        drawPotBody(scope, centerX, potRimY, potWidth, potHeight, moisture)

        // 3. Draw Plant if present
        val plant = potSlot.plant
        if (plant != null) {
            drawProceduralPlant(scope, plant, centerX, potRimY, animationTime)
        }
    }

    private fun drawStool(
        scope: DrawScope,
        centerX: Float,
        stoolTopY: Float,
        stoolWidth: Float,
        groundY: Float
    ) {
        val legInset = 16f
        // Stool legs
        scope.drawLine(
            color = StoolLegs,
            start = Offset(centerX - stoolWidth / 2 + legInset, stoolTopY + 10f),
            end = Offset(centerX - stoolWidth / 2 + 5f, groundY),
            strokeWidth = 10f
        )
        scope.drawLine(
            color = StoolLegs,
            start = Offset(centerX + stoolWidth / 2 - legInset, stoolTopY + 10f),
            end = Offset(centerX + stoolWidth / 2 - 5f, groundY),
            strokeWidth = 10f
        )
        // Stool horizontal brace
        scope.drawLine(
            color = StoolLegs,
            start = Offset(centerX - stoolWidth / 2 + 10f, stoolTopY + 60f),
            end = Offset(centerX + stoolWidth / 2 - 10f, stoolTopY + 60f),
            strokeWidth = 6f
        )
        // Stool wooden seat
        scope.drawRect(
            color = StoolWood,
            topLeft = Offset(centerX - stoolWidth / 2, stoolTopY),
            size = Size(stoolWidth, 16f)
        )
    }

    private fun drawPotBody(
        scope: DrawScope,
        centerX: Float,
        potRimY: Float,
        potWidth: Float,
        potHeight: Float,
        moisture: Float
    ) {
        val bottomWidth = potWidth * 0.75f
        val bottomY = potRimY + potHeight

        // Pot trapezoid path
        val potPath = Path().apply {
            moveTo(centerX - potWidth / 2, potRimY + 8f)
            lineTo(centerX + potWidth / 2, potRimY + 8f)
            lineTo(centerX + bottomWidth / 2, bottomY)
            lineTo(centerX - bottomWidth / 2, bottomY)
            close()
        }
        scope.drawPath(potPath, color = PotTerracotta, style = Fill)
        scope.drawPath(potPath, color = PotRim, style = Stroke(width = 3f))

        // Pot top rim
        scope.drawRoundRect(
            color = PotRim,
            topLeft = Offset(centerX - (potWidth / 2 + 5f), potRimY),
            size = Size(potWidth + 10f, 14f)
        )

        // Soil inside pot (interpolated by moisture)
        val soilColor = lerpColor(SoilDry, SoilWet, moisture / 100f)
        scope.drawOval(
            color = soilColor,
            topLeft = Offset(centerX - potWidth / 2 + 6f, potRimY + 2f),
            size = Size(potWidth - 12f, 10f)
        )
    }

    private fun drawProceduralPlant(
        scope: DrawScope,
        plant: PlantInstance,
        centerX: Float,
        potRimY: Float,
        animationTime: Float
    ) {
        val progress = plant.growthProgress
        val sway = sin(animationTime * 2f) * 4f

        when (plant.stage) {
            GrowthStage.SEED -> {
                // Tiny seed sprout tip
                scope.drawCircle(
                    color = PlantLightGreen,
                    radius = 4f,
                    center = Offset(centerX, potRimY - 3f)
                )
            }

            GrowthStage.SPROUT -> {
                val stemHeight = 35f * (progress / 40f).coerceIn(0.4f, 1f)
                val topY = potRimY - stemHeight
                scope.drawLine(
                    color = PlantGreen,
                    start = Offset(centerX, potRimY),
                    end = Offset(centerX + sway * 0.5f, topY),
                    strokeWidth = 4f
                )
                // Two small cotyledon leaves
                scope.drawOval(
                    color = PlantLightGreen,
                    topLeft = Offset(centerX - 14f + sway * 0.5f, topY - 4f),
                    size = Size(12f, 6f)
                )
                scope.drawOval(
                    color = PlantLightGreen,
                    topLeft = Offset(centerX + 2f + sway * 0.5f, topY - 4f),
                    size = Size(12f, 6f)
                )
            }

            GrowthStage.VEGETATIVE, GrowthStage.BUDDING, GrowthStage.READY -> {
                val maxStemHeight = 240f
                val stemHeight = maxStemHeight * (progress / 100f).coerceIn(0.35f, 1f)
                val topY = potRimY - stemHeight

                // Draw central stem with wind curvature
                val stemPath = Path().apply {
                    moveTo(centerX, potRimY)
                    quadraticTo(centerX + sway * 0.4f, potRimY - stemHeight * 0.5f, centerX + sway, topY)
                }
                scope.drawPath(stemPath, color = PlantGreen, style = Stroke(width = 6f))

                // Tiered branching fan leaves
                val tiers = when (plant.stage) {
                    GrowthStage.VEGETATIVE -> 4
                    else -> 6
                }

                for (i in 1..tiers) {
                    val fraction = i.toFloat() / (tiers + 1)
                    val tierY = potRimY - (stemHeight * fraction)
                    val tierSway = sway * fraction
                    val branchSpan = (40f + (1f - fraction) * 45f)

                    // Left branch fan leaves
                    drawFanLeaves(scope, centerX + tierSway - 4f, tierY, -branchSpan, isLeft = true)
                    // Right branch fan leaves
                    drawFanLeaves(scope, centerX + tierSway + 4f, tierY, branchSpan, isLeft = false)
                }

                // If Budding or Ready, draw flowering bud clusters and trichome sparkles
                if (plant.stage == GrowthStage.BUDDING || plant.stage == GrowthStage.READY) {
                    drawFlowerBuds(scope, centerX + sway, topY, plant.stage == GrowthStage.READY, animationTime)
                }
            }
            else -> {}
        }
    }

    private fun drawFanLeaves(
        scope: DrawScope,
        originX: Float,
        originY: Float,
        span: Float,
        isLeft: Boolean
    ) {
        val branchTipX = originX + span
        val branchTipY = originY - 10f

        // Branch line
        scope.drawLine(
            color = PlantGreen,
            start = Offset(originX, originY),
            end = Offset(branchTipX, branchTipY),
            strokeWidth = 3f
        )

        // 3-5 serrated finger leaves spreading from tip
        val leafAngles = listOf(-25f, -12f, 0f, 12f, 25f)
        val leafLength = 22f
        for (angle in leafAngles) {
            val rad = Math.toRadians((if (isLeft) 180f else 0f) + angle.toDouble())
            val lx = branchTipX + (cos(rad) * leafLength).toFloat()
            val ly = branchTipY + (sin(rad) * leafLength).toFloat()

            scope.drawLine(
                color = PlantLightGreen,
                start = Offset(branchTipX, branchTipY),
                end = Offset(lx, ly),
                strokeWidth = 3.5f
            )
        }
    }

    private fun drawFlowerBuds(
        scope: DrawScope,
        headX: Float,
        headY: Float,
        isReady: Boolean,
        animationTime: Float
    ) {
        // Pulsing ready aura
        if (isReady) {
            val pulse = (sin(animationTime * 4f) * 6f) + 26f
            scope.drawCircle(
                color = PlantGoldGlow.copy(alpha = 0.35f),
                radius = pulse,
                center = Offset(headX, headY)
            )
        }

        // Main dense bud cluster
        scope.drawCircle(
            color = PlantBuds,
            radius = 16f,
            center = Offset(headX, headY - 4f)
        )
        scope.drawCircle(
            color = PlantGreen,
            radius = 12f,
            center = Offset(headX - 6f, headY + 4f)
        )
        scope.drawCircle(
            color = PlantGreen,
            radius = 12f,
            center = Offset(headX + 6f, headY + 4f)
        )

        // Golden trichomes pistils
        scope.drawCircle(
            color = PlantGoldGlow,
            radius = 4f,
            center = Offset(headX, headY - 8f)
        )
    }

    private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * f,
            green = c1.green + (c2.green - c1.green) * f,
            blue = c1.blue + (c2.blue - c1.blue) * f,
            alpha = 1f
        )
    }
}
