package com.greentycoon.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.greentycoon.game.model.GameAction
import com.greentycoon.game.ui.GameViewModel
import com.greentycoon.game.ui.MainGameScreen
import com.greentycoon.game.ui.theme.GreenTycoonTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safe Deep Link handling (fixes legacy NullPointerException bug)
        handleSafeIncomingIntent(intent)

        setContent {
            GreenTycoonTheme {
                MainGameScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSafeIncomingIntent(intent)
    }

    private fun handleSafeIncomingIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        try {
            if (uri.scheme == "greentycoon" && uri.host == "game") {
                val actionParam = uri.getQueryParameter("action")
                if (actionParam == "water_all") {
                    for (pot in viewModel.gameState.value.pots) {
                        viewModel.handleAction(GameAction.WaterPot(pot.id))
                    }
                }
            }
        } catch (e: Exception) {
            // Safely ignore unexpected URI structures without crashing
        }
    }
}
