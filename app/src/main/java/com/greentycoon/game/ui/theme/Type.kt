package com.greentycoon.game.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.greentycoon.game.R

val TweedFontFamily = FontFamily(
    Font(R.font.tweed, FontWeight.Normal),
    Font(R.font.tweed_bold, FontWeight.Bold)
)

val ChalkdusterFontFamily = FontFamily(
    Font(R.font.chalkduster, FontWeight.Normal)
)

fun createTweedFontFamily(assetManager: AssetManager): FontFamily = FontFamily(
    Font("fonts/tweed.ttf", assetManager, FontWeight.Normal),
    Font("fonts/tweed_bold.ttf", assetManager, FontWeight.Bold)
)

fun createChalkdusterFontFamily(assetManager: AssetManager): FontFamily = FontFamily(
    Font("fonts/chalkduster.ttf", assetManager, FontWeight.Normal)
)

val TweedHeader = TextStyle(
    fontFamily = TweedFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = 0.5.sp
)

val ChalkBody = TextStyle(
    fontFamily = ChalkdusterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

val CashDisplay = TextStyle(
    fontFamily = TweedFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    letterSpacing = 0.5.sp
)

val Typography = Typography(
    headlineLarge = TweedHeader,
    bodyLarge = ChalkBody,
    labelLarge = CashDisplay
)
