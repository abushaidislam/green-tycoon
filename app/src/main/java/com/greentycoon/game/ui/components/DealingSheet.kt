package com.greentycoon.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greentycoon.game.model.CustomerType
import com.greentycoon.game.model.NpcCustomer
import com.greentycoon.game.ui.theme.*

@Composable
fun DealingSheet(
    visitor: NpcCustomer,
    onSellClicked: (Double) -> Unit,
    onKickClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var proposedPrice by remember(visitor.id) {
        mutableStateOf(visitor.currentOfferPricePerGram)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE6A5D6A7),
                        Color(0xE62E7D32),
                        Color(0xEE1B5E20)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // 1. Kick Out Boot Icon on Top Left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0x44000000))
                .clickable { onKickClicked() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                // Brown heavy work boot silhouette
                val bootPath = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.1f)
                    lineTo(size.width * 0.5f, size.height * 0.1f)
                    lineTo(size.width * 0.5f, size.height * 0.5f)
                    lineTo(size.width * 0.9f, size.height * 0.6f)
                    lineTo(size.width * 0.9f, size.height * 0.85f)
                    lineTo(size.width * 0.1f, size.height * 0.85f)
                    close()
                }
                drawPath(bootPath, color = Color(0xFF6D4C41))
                // Red impact sparks
                drawCircle(color = KickRed, radius = 4f, center = Offset(size.width * 0.1f, size.height * 0.1f))
            }
        }

        // 2. Center Content: Big Portrait, Speech Bubble, Details, and Sell Button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Customer Face Portrait Canvas
            Canvas(
                modifier = Modifier
                    .size(190.dp, 200.dp)
            ) {
                val cx = size.width / 2
                val cy = size.height / 2 + 10f

                // Neck
                drawRect(
                    color = Color(0xFFD7CCC8),
                    topLeft = Offset(cx - 25f, cy + 30f),
                    size = Size(50f, 40f)
                )
                // Head oval
                drawOval(
                    color = Color(0xFFE8CBB8),
                    topLeft = Offset(cx - 50f, cy - 65f),
                    size = Size(100f, 120f)
                )

                if (visitor.avatarId == "mike") {
                    // Cap
                    drawRoundRect(
                        color = Color(0xFFF8BBD0),
                        topLeft = Offset(cx - 52f, cy - 75f),
                        size = Size(104f, 40f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    // Cap visor
                    drawRoundRect(
                        color = Color(0xFF5D4037),
                        topLeft = Offset(cx - 45f, cy - 40f),
                        size = Size(90f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Tired eyes
                    drawCircle(color = Color(0x88D32F2F), radius = 9f, center = Offset(cx - 20f, cy - 10f))
                    drawCircle(color = Color(0xFF3E2723), radius = 4f, center = Offset(cx - 20f, cy - 10f))
                    drawCircle(color = Color(0x88D32F2F), radius = 9f, center = Offset(cx + 20f, cy - 10f))
                    drawCircle(color = Color(0xFF3E2723), radius = 4f, center = Offset(cx + 20f, cy - 10f))
                    // Big bushy mustache
                    drawOval(
                        color = Color(0xFF4E342E),
                        topLeft = Offset(cx - 30f, cy + 18f),
                        size = Size(60f, 16f)
                    )
                } else if (visitor.type == CustomerType.UNDERCOVER_COP) {
                    // Police badge / cap
                    drawRect(color = Color(0xFF1565C0), topLeft = Offset(cx - 45f, cy - 75f), size = Size(90f, 30f))
                    drawCircle(color = Color(0xFFFFD54F), radius = 8f, center = Offset(cx, cy - 60f))
                } else {
                    // Sunglasses dude
                    drawRoundRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(cx - 36f, cy - 18f),
                        size = Size(72f, 22f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }

            // Speech Bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "\"${visitor.dialogueText}\"",
                    color = Color(0xFF263238),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Customer Name & Offer Summary (e.g. Mike takes 1 weed for 50 cash)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = visitor.name,
                    color = HudYellow,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                val totalCash = (visitor.requestedGrams * proposedPrice).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "takes ", color = HudYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${visitor.requestedGrams} weed ", color = HudCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(text = "for ", color = HudYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$totalCash cash", color = HudWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // Bargaining Price Slider
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("Bargain: ", color = HudWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = proposedPrice.toFloat(),
                        onValueChange = { proposedPrice = it.toDouble() },
                        valueRange = (visitor.baseOfferedPricePerGram.toFloat() * 0.8f)..(visitor.baseOfferedPricePerGram.toFloat() * 1.6f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${proposedPrice.toInt()}/g",
                        color = HudYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // 3. Bottom SELL Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSellClicked(proposedPrice) }
                    .padding(bottom = 16.dp)
            ) {
                // Handshake transaction graphic
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Leaf
                    Canvas(modifier = Modifier.size(36.dp)) {
                        drawCircle(color = PlantGreen, radius = 14f, center = Offset(18f, 18f))
                    }
                    // Bidirectional Arrow
                    Text(text = " ⇄ ", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    // Cash
                    Canvas(modifier = Modifier.size(40.dp, 26.dp)) {
                        drawRoundRect(color = Color(0xFFC8E6C9), size = Size(40f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                    }
                }

                Text(
                    text = "SELL",
                    color = HudYellow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
