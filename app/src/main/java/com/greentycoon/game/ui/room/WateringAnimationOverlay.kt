package com.greentycoon.game.ui.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.greentycoon.game.R
import kotlinx.coroutines.delay

@Composable
fun WateringAnimationOverlay(
    isWatering: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var frameIndex by remember { mutableIntStateOf(0) }
    val frames = remember {
        listOf(
            R.drawable.watering_0_anim_0,
            R.drawable.watering_0_anim_1,
            R.drawable.watering_0_anim_2
        )
    }

    LaunchedEffect(isWatering) {
        if (isWatering) {
            repeat(2) {
                for (i in frames.indices) {
                    frameIndex = i
                    delay(130L)
                }
            }
            frameIndex = 0
            onAnimationEnd()
        }
    }

    AnimatedVisibility(
        visible = isWatering,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = frames[frameIndex]),
                contentDescription = "Watering Animation",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(90.dp)
            )
        }
    }
}
