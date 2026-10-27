package com.greentycoon.game.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greentycoon.game.engine.DealingEngine
import com.greentycoon.game.engine.GrowthEngine
import com.greentycoon.game.model.*
import com.greentycoon.game.storage.SecureSaveManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class GameViewModel(
    application: Application? = null,
    initialState: GameState? = null,
    private val enableLoop: Boolean = true,
    private val random: Random = Random()
) : AndroidViewModel(application ?: Application()) {

    private val _gameState = MutableStateFlow(initialState ?: GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var loopJob: Job? = null
    private var tickCounter = 0

    init {
        // 1. If starting fresh, attempt loading from encrypted storage
        if (initialState == null && application != null) {
            val loadedState = SecureSaveManager.loadGame(application)
            // Calculate offline elapsed time delta
            val now = System.currentTimeMillis()
            val offlineDeltaSeconds = ((now - loadedState.lastSavedTimestamp) / 1000f).coerceIn(0f, 86400f * 7) // max 7 days
            val updatedPots = GrowthEngine.calculateOfflineGrowth(
                loadedState.pots,
                loadedState.roomUpgrades,
                offlineDeltaSeconds
            )
            _gameState.value = loadedState.copy(
                pots = updatedPots,
                lastSavedTimestamp = now,
                bannerNotification = if (offlineDeltaSeconds > 60f) "Welcome back! Plants grew while you were away." else null
            )
        }

        // 2. Start foreground 1-second ticking loop
        if (enableLoop) {
            startLoop()
        }
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L) // 1 second tick
                onTick()
            }
        }
    }

    private fun onTick() {
        tickCounter++
        _gameState.update { state ->
            // 1. Tick all unlocked pots with plants
            val updatedPots = state.pots.map { pot ->
                val plant = pot.plant
                if (pot.isUnlocked && plant != null) {
                    pot.copy(plant = GrowthEngine.tickPlant(plant, pot.potType, state.roomUpgrades, deltaSeconds = 1.0f))
                } else {
                    pot
                }
            }

            // 2. Tick visitor patience or spawn new visitor
            var visitor = state.currentVisitor
            var departureTime = state.lastVisitorDepartureTimestamp
            var notification = state.bannerNotification

            if (visitor != null) {
                val remainingPatience = visitor.patienceSeconds - 1
                if (remainingPatience <= 0) {
                    visitor = null
                    departureTime = System.currentTimeMillis()
                    notification = "Customer got tired of waiting and left."
                } else {
                    visitor = visitor.copy(patienceSeconds = remainingPatience)
                }
            } else {
                // Try spawn visitor every 25 seconds if player has stash
                val timeSinceDeparture = (System.currentTimeMillis() - departureTime) / 1000
                if (timeSinceDeparture >= 25 && state.inventory.totalWeedGrams > 0) {
                    visitor = DealingEngine.tryGenerateVisitor(state, random)
                    if (visitor != null) {
                        notification = "Someone's knocking at the door! 🚪"
                    }
                }
            }

            state.copy(
                pots = updatedPots,
                currentVisitor = visitor,
                lastVisitorDepartureTimestamp = departureTime,
                bannerNotification = notification,
                lastSavedTimestamp = System.currentTimeMillis()
            )
        }

        // Auto-save every 10 seconds
        if (tickCounter % 10 == 0) {
            saveCurrentState()
        }
    }

    fun handleAction(action: GameAction) {
        _gameState.update { state ->
            val newState = when (action) {
                is GameAction.WaterPot -> handleWaterPot(state, action.potId)
                is GameAction.FertilizePot -> handleFertilizePot(state, action.potId)
                is GameAction.HarvestPot -> handleHarvestPot(state, action.potId)
                is GameAction.PlantSeedInPot -> handlePlantSeed(state, action.potId, action.strainId)
                is GameAction.UnlockPot -> handleUnlockPot(state, action.potId)
                is GameAction.UpgradePot -> handleUpgradePot(state, action.potId, action.targetPotType)
                is GameAction.BuySeed -> handleBuySeed(state, action.strainId, action.count)
                is GameAction.BuyWaterBottle -> handleBuyWater(state, action.count)
                is GameAction.BuyFertilizer -> handleBuyFertilizer(state, action.count)
                is GameAction.UpgradeLight -> handleUpgradeLight(state, action.newLevel)
                is GameAction.SwitchRoomSide -> state.copy(currentRoomSide = action.side)
                is GameAction.AcceptDeal -> handleAcceptDeal(state, action.visitorId)
                is GameAction.BargainDeal -> handleBargainDeal(state, action.visitorId, action.proposedPrice)
                is GameAction.KickCustomer -> handleKickCustomer(state, action.visitorId)
                is GameAction.DismissNotification -> state.copy(bannerNotification = null)
            }
            newState
        }
        saveCurrentState()
    }

    private fun handleWaterPot(state: GameState, potId: Int): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        val plant = pot.plant ?: return state
        if (state.inventory.waterBottles <= 0) {
            return state.copy(bannerNotification = "Out of water! Buy more in the shop.")
        }

        val updatedPlant = GrowthEngine.waterPlant(plant)
        val updatedPots = state.pots.map { if (it.id == potId) it.copy(plant = updatedPlant) else it }
        val updatedInv = state.inventory.copy(waterBottles = state.inventory.waterBottles - 1)

        return state.copy(pots = updatedPots, inventory = updatedInv)
    }

    private fun handleFertilizePot(state: GameState, potId: Int): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        val plant = pot.plant ?: return state
        if (plant.fertilizerActive || state.inventory.fertilizers <= 0) return state

        val updatedPlant = GrowthEngine.fertilizePlant(plant)
        val updatedPots = state.pots.map { if (it.id == potId) it.copy(plant = updatedPlant) else it }
        val updatedInv = state.inventory.copy(fertilizers = state.inventory.fertilizers - 1)

        return state.copy(
            pots = updatedPots,
            inventory = updatedInv,
            bannerNotification = "Fertilizer applied! 1.5x Growth speed."
        )
    }

    private fun handleHarvestPot(state: GameState, potId: Int): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        val plant = pot.plant ?: return state
        if (plant.stage != GrowthStage.READY) return state

        val yieldGrams = GrowthEngine.calculateYield(plant, pot.potType)
        val strainId = plant.strain.id

        val currentStock = state.inventory.harvestedStock[strainId] ?: 0
        val updatedStock = state.inventory.harvestedStock.toMutableMap().apply {
            put(strainId, currentStock + yieldGrams)
        }

        val xpGained = (yieldGrams * 2L)
        val updatedPlayer = awardXp(state.player, xpGained).copy(
            totalHarvestsGrams = state.player.totalHarvestsGrams + yieldGrams
        )

        val updatedPots = state.pots.map { if (it.id == potId) it.copy(plant = null) else it }

        return state.copy(
            pots = updatedPots,
            inventory = state.inventory.copy(harvestedStock = updatedStock),
            player = updatedPlayer,
            bannerNotification = "Harvested $yieldGrams grams of ${plant.strain.name}! (+$xpGained XP)"
        )
    }

    private fun handlePlantSeed(state: GameState, potId: Int, strainId: String): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        if (!pot.isUnlocked || pot.plant != null) return state

        val seedCount = state.inventory.seeds[strainId] ?: 0
        if (seedCount <= 0) return state.copy(bannerNotification = "No seeds of this strain left!")

        val strain = PlantStrain.findById(strainId)
        val newPlant = PlantInstance(strain = strain)

        val updatedSeeds = state.inventory.seeds.toMutableMap().apply {
            put(strainId, seedCount - 1)
        }
        val updatedPots = state.pots.map { if (it.id == potId) it.copy(plant = newPlant) else it }

        return state.copy(
            pots = updatedPots,
            inventory = state.inventory.copy(seeds = updatedSeeds),
            bannerNotification = "Planted ${strain.name}!"
        )
    }

    private fun handleUnlockPot(state: GameState, potId: Int): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        if (pot.isUnlocked) return state

        val unlockCost = when (potId) {
            1 -> 150.0
            2 -> 450.0
            else -> 1000.0
        }
        if (state.player.cash < unlockCost) return state.copy(bannerNotification = "Not enough cash!")

        val updatedPots = state.pots.map { if (it.id == potId) it.copy(isUnlocked = true) else it }
        val updatedPlayer = state.player.copy(cash = state.player.cash - unlockCost)

        return state.copy(
            pots = updatedPots,
            player = updatedPlayer,
            bannerNotification = "Unlocked Pot Slot #${potId + 1}!"
        )
    }

    private fun handleUpgradePot(state: GameState, potId: Int, targetType: PotType): GameState {
        val pot = state.pots.find { it.id == potId } ?: return state
        if (state.player.cash < targetType.upgradePrice) return state

        val updatedPots = state.pots.map { if (it.id == potId) it.copy(potType = targetType) else it }
        val updatedPlayer = state.player.copy(cash = state.player.cash - targetType.upgradePrice)

        return state.copy(pots = updatedPots, player = updatedPlayer)
    }

    private fun handleBuySeed(state: GameState, strainId: String, count: Int): GameState {
        val strain = PlantStrain.findById(strainId)
        val totalCost = strain.seedPrice * count
        if (state.player.cash < totalCost) return state.copy(bannerNotification = "Not enough cash!")

        val currentSeeds = state.inventory.seeds[strainId] ?: 0
        val updatedSeeds = state.inventory.seeds.toMutableMap().apply {
            put(strainId, currentSeeds + count)
        }
        val updatedPlayer = state.player.copy(cash = state.player.cash - totalCost)

        return state.copy(
            player = updatedPlayer,
            inventory = state.inventory.copy(seeds = updatedSeeds),
            bannerNotification = "Purchased $count ${strain.name} seeds!"
        )
    }

    private fun handleBuyWater(state: GameState, count: Int): GameState {
        val cost = 25.0 * count
        if (state.player.cash < cost) return state

        return state.copy(
            player = state.player.copy(cash = state.player.cash - cost),
            inventory = state.inventory.copy(waterBottles = state.inventory.waterBottles + (3 * count)),
            bannerNotification = "Purchased Water Bottles pack!"
        )
    }

    private fun handleBuyFertilizer(state: GameState, count: Int): GameState {
        val cost = 40.0 * count
        if (state.player.cash < cost) return state

        return state.copy(
            player = state.player.copy(cash = state.player.cash - cost),
            inventory = state.inventory.copy(fertilizers = state.inventory.fertilizers + (2 * count)),
            bannerNotification = "Purchased Growth Fertilizer pack!"
        )
    }

    private fun handleUpgradeLight(state: GameState, newLevel: Int): GameState {
        val cost = when (newLevel) {
            2 -> 250.0
            3 -> 750.0
            else -> 2000.0
        }
        if (state.player.cash < cost) return state

        return state.copy(
            player = state.player.copy(cash = state.player.cash - cost),
            roomUpgrades = state.roomUpgrades.copy(lightLevel = newLevel),
            bannerNotification = "Upgraded Grow Lights to Level $newLevel!"
        )
    }

    private fun handleBargainDeal(state: GameState, visitorId: String, proposedPrice: Double): GameState {
        val visitor = state.currentVisitor ?: return state
        if (visitor.id != visitorId) return state
        return state.copy(currentVisitor = visitor.copy(currentOfferPricePerGram = proposedPrice))
    }

    private fun handleAcceptDeal(state: GameState, visitorId: String): GameState {
        val visitor = state.currentVisitor ?: return state
        if (visitor.id != visitorId) return state

        // Special handling for Undercover Cop
        if (visitor.type == CustomerType.UNDERCOVER_COP) {
            val bribe = DealingEngine.calculateCopBribe(state.player)
            val updatedCash = (state.player.cash - bribe).coerceAtLeast(0.0)
            return state.copy(
                player = state.player.copy(cash = updatedCash, respectScore = state.player.respectScore + 2),
                currentVisitor = null,
                lastVisitorDepartureTimestamp = System.currentTimeMillis(),
                bannerNotification = "Bribed Officer Bob ($${bribe.toInt()}). Raid avoided!"
            )
        }

        // Standard Deal
        val strainId = visitor.requestedStrainId
        val currentGrams = state.inventory.harvestedStock[strainId] ?: 0
        val gramsToSell = minOf(visitor.requestedGrams, currentGrams)
        if (gramsToSell <= 0) return state.copy(bannerNotification = "You don't have enough grams to sell!")

        val totalEarnings = gramsToSell * visitor.currentOfferPricePerGram
        val updatedStock = state.inventory.harvestedStock.toMutableMap().apply {
            put(strainId, currentGrams - gramsToSell)
        }
        val xpGained = (totalEarnings * 0.5).toLong()
        val updatedPlayer = awardXp(state.player, xpGained).copy(
            cash = state.player.cash + totalEarnings,
            respectScore = state.player.respectScore + 1
        )

        return state.copy(
            player = updatedPlayer,
            inventory = state.inventory.copy(harvestedStock = updatedStock),
            currentVisitor = null,
            lastVisitorDepartureTimestamp = System.currentTimeMillis(),
            bannerNotification = "Sold $gramsToSell grams to ${visitor.name} for $${totalEarnings.toInt()}! (+$xpGained XP)"
        )
    }

    private fun handleKickCustomer(state: GameState, visitorId: String): GameState {
        val visitor = state.currentVisitor ?: return state
        if (visitor.id != visitorId) return state

        return state.copy(
            currentVisitor = null,
            lastVisitorDepartureTimestamp = System.currentTimeMillis(),
            bannerNotification = "Kicked ${visitor.name} out!"
        )
    }

    private fun awardXp(player: PlayerProfile, xp: Long): PlayerProfile {
        var currentXp = player.currentXp + xp
        var level = player.level
        var needed = player.xpForNextLevel

        while (currentXp >= needed) {
            currentXp -= needed
            level++
            needed = level * 100L + (level * level * 20L)
        }

        return player.copy(level = level, currentXp = currentXp)
    }

    private fun saveCurrentState() {
        try {
            val app = getApplication<Application?>()
            if (app != null) {
                SecureSaveManager.saveGame(app, _gameState.value)
            }
        } catch (e: Exception) {
            // Ignore in unit tests
        }
    }
}
