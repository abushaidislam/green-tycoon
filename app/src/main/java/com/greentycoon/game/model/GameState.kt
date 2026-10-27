package com.greentycoon.game.model

data class PlayerProfile(
    val cash: Double = 250.0,
    val bank: Double = 0.0,
    val level: Int = 1,
    val currentXp: Long = 0L,
    val respectScore: Int = 10,
    val totalHarvestsGrams: Long = 0L
) {
    val xpForNextLevel: Long
        get() = level * 100L + (level * level * 20L)
        
    val xpProgressFraction: Float
        get() = (currentXp.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

data class StashInventory(
    val seeds: Map<String, Int> = mapOf("sour_diesel" to 3),
    val harvestedStock: Map<String, Int> = emptyMap(),
    val waterBottles: Int = 5,
    val fertilizers: Int = 2
) {
    val totalWeedGrams: Int
        get() = harvestedStock.values.sum()
}

data class RoomUpgrades(
    val lightLevel: Int = 1,       // 1: Incandescent (1.0x), 2: Fluorescent (1.25x), 3: LED (1.5x), 4: HPS Ultra (2.0x)
    val ventilationLevel: Int = 1, // Reduces smell / lowers cop raid probability
    val vinylPlayerActive: Boolean = false
) {
    val lightGrowthMultiplier: Float
        get() = when (lightLevel) {
            1 -> 1.0f
            2 -> 1.25f
            3 -> 1.5f
            4 -> 2.0f
            else -> 1.0f
        }
}

enum class RoomSide {
    PLANTS,
    DOOR
}

data class GameState(
    val player: PlayerProfile = PlayerProfile(),
    val pots: List<PotSlot> = listOf(
        PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = null),
        PotSlot(id = 1, potType = PotType.PLASTIC, isUnlocked = false, plant = null),
        PotSlot(id = 2, potType = PotType.PLASTIC, isUnlocked = false, plant = null),
        PotSlot(id = 3, potType = PotType.PLASTIC, isUnlocked = false, plant = null)
    ),
    val inventory: StashInventory = StashInventory(),
    val roomUpgrades: RoomUpgrades = RoomUpgrades(),
    val currentVisitor: NpcCustomer? = null,
    val currentRoomSide: RoomSide = RoomSide.PLANTS,
    val lastVisitorDepartureTimestamp: Long = 0L,
    val lastSavedTimestamp: Long = System.currentTimeMillis(),
    val bannerNotification: String? = null
)

sealed class GameAction {
    data class WaterPot(val potId: Int) : GameAction()
    data class FertilizePot(val potId: Int) : GameAction()
    data class HarvestPot(val potId: Int) : GameAction()
    data class PlantSeedInPot(val potId: Int, val strainId: String) : GameAction()
    data class UnlockPot(val potId: Int) : GameAction()
    data class UpgradePot(val potId: Int, val targetPotType: PotType) : GameAction()
    data class BuySeed(val strainId: String, val count: Int = 1) : GameAction()
    data class BuyFertilizer(val count: Int = 1) : GameAction()
    data class BuyWaterBottle(val count: Int = 1) : GameAction()
    data class UpgradeLight(val newLevel: Int) : GameAction()
    data class SwitchRoomSide(val side: RoomSide) : GameAction()
    
    // NPC dealing actions
    data class AcceptDeal(val visitorId: String) : GameAction()
    data class BargainDeal(val visitorId: String, val proposedPrice: Double) : GameAction()
    data class KickCustomer(val visitorId: String) : GameAction()
    object DismissNotification : GameAction()
}
