package com.greentycoon.game.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greentycoon.game.R
import com.greentycoon.game.model.GameState
import com.greentycoon.game.ui.theme.*
import java.util.Locale

/**
 * Formats cash amount with dollar sign and comma thousands separators.
 */
fun formatCash(amount: Double): String = String.format(Locale.US, "$%,.2f", amount)

/**
 * Player level badge, XP progress bar, and respect meter.
 * Uses primitive parameters to enable Compose recomposition skipping during game ticks.
 */
@Composable
fun PlayerStatsBadge(
    level: Int,
    currentXp: Long,
    xpForNextLevel: Long,
    xpProgress: Float,
    respect: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Level badge with leaf icon
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC1B5E20))
                .border(1.dp, PlantLightGreen.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.weed_ground),
                    contentDescription = "Player Level",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LVL $level",
                    style = TweedHeader.copy(
                        fontSize = 13.sp,
                        color = HudYellow,
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 1
                )
            }
        }

        // XP and Respect Column
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // XP progress bar
            Column {
                val progressFraction = xpProgress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .widthIn(min = 50.dp, max = 70.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0F1416))
                        .border(0.5.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(HudCashGreen, PlantLightGreen)
                                )
                            )
                    )
                }
                Text(
                    text = "$currentXp/$xpForNextLevel XP",
                    style = ChalkBody.copy(
                        fontSize = 9.sp,
                        color = Color(0xFFCFD8DC)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Respect meter with star
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "★",
                    color = HudYellow,
                    fontSize = 11.sp
                )
                Text(
                    text = "$respect",
                    style = TweedHeader.copy(
                        fontSize = 11.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Tabular Cash counter and Stash harvested stock pill.
 * Uses primitive parameters to skip recomposition when cash and inventory do not change.
 */
@Composable
fun CashStashCounter(
    cash: Double,
    totalGrams: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Formatted cash display ($1,250.00)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.btn_cash),
                contentDescription = "Cash",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = formatCash(cash),
                style = CashDisplay.copy(
                    fontSize = 16.sp,
                    color = HudWhite,
                    fontWeight = FontWeight.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Stash pill showing current total grams in stock
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x80004D40))
                .border(0.8.dp, HudCyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "🌿",
                    fontSize = 10.sp
                )
                Text(
                    text = "${totalGrams}g",
                    style = ChalkBody.copy(
                        fontSize = 11.sp,
                        color = HudCyan,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Action buttons for Top HUD (Shop and Sound Mute toggle).
 * Provides accessible >= 48dp touch targets.
 */
@Composable
fun HudActionButtons(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Audio mute toggle button (>= 48dp interactive touch target)
        val muteIconRes = if (isMuted) R.drawable.ic_menu_muteon else R.drawable.ic_menu_muteoff
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(onClick = onToggleMute),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = muteIconRes),
                    contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Shop button (>= 48dp interactive touch target)
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(onClick = onOpenShop),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.btn_shop),
                    contentDescription = "Shop",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Weed Firm styled authentic top HUD bar featuring:
 * - Wood/slate textured surface with rounded bottom corners and elevation
 * - Left section: Level badge with leaf icon, XP progress bar, Respect stars
 * - Center section: Tabular Cash counter ($1,250.00) with cash icon and Stash stock pill
 * - Right section: Shop button and Sound Mute toggle button
 */
@Composable
fun AuthenticTopHud(
    gameState: GameState,
    onOpenShop: () -> Unit,
    onToggleMute: () -> Unit,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val hudShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .shadow(elevation = 8.dp, shape = hudShape)
            .clip(hudShape)
            .background(Color(0xFF1C140F))
            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = hudShape)
    ) {
        // Authentic Weed Firm Wood Texture Surface
        Image(
            painter = painterResource(id = R.drawable.bg_wood),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark slate overlay for text contrast and readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE1E2428),
                            Color(0xDD12171A)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. LEFT SECTION: Player Level Badge, XP progress bar, Respect score
            PlayerStatsBadge(
                level = gameState.player.level,
                currentXp = gameState.player.currentXp,
                xpForNextLevel = gameState.player.xpForNextLevel,
                xpProgress = gameState.player.xpProgressFraction,
                respect = gameState.player.respect,
                modifier = Modifier.weight(1.1f, fill = false)
            )

            // 2. CENTER SECTION: Cash Counter & Stash Weight Pill
            CashStashCounter(
                cash = gameState.player.cash,
                totalGrams = gameState.inventory.totalWeedGrams,
                modifier = Modifier.weight(1f, fill = false)
            )

            // 3. RIGHT SECTION: Audio Mute Toggle & Shop Button
            HudActionButtons(
                isMuted = isMuted,
                onToggleMute = onToggleMute,
                onOpenShop = onOpenShop
            )
        }
    }
}
