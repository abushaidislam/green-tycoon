package com.greentycoon.game.ui

import com.greentycoon.game.model.*
import org.junit.Assert.*
import org.junit.Test

class GameViewModelTest {

    @Test
    fun testPlantSeedAction() {
        val initialState = GameState(
            inventory = StashInventory(seeds = mapOf("sour_diesel" to 2))
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.PlantSeedInPot(potId = 0, strainId = "sour_diesel"))

        val updated = vm.gameState.value
        assertEquals(1, updated.inventory.seeds["sour_diesel"])
        assertNotNull(updated.pots[0].plant)
        assertEquals("sour_diesel", updated.pots[0].plant!!.strain.id)
        assertEquals(GrowthStage.SEED, updated.pots[0].plant!!.stage)
    }

    @Test
    fun testWaterPotAction() {
        val dryPlant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, moisture = 10f)
        val initialState = GameState(
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = dryPlant)),
            inventory = StashInventory(waterBottles = 3)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.WaterPot(potId = 0))

        val updated = vm.gameState.value
        assertEquals(100f, updated.pots[0].plant!!.moisture, 0.01f)
        assertEquals(2, updated.inventory.waterBottles)
    }

    @Test
    fun testHarvestPotAction() {
        val readyPlant = PlantInstance(
            strain = PlantStrain.SOUR_DIESEL,
            growthProgress = 100f,
            stage = GrowthStage.READY
        )
        val initialState = GameState(
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = readyPlant)),
            inventory = StashInventory(harvestedStock = emptyMap()),
            player = PlayerProfile(currentXp = 0L)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.HarvestPot(potId = 0))

        val updated = vm.gameState.value
        assertNull("Pot must be emptied after harvest", updated.pots[0].plant)
        assertTrue("Harvested stock must receive grams", (updated.inventory.harvestedStock["sour_diesel"] ?: 0) > 0)
        assertTrue("Player should gain XP", updated.player.currentXp > 0L)
    }

    @Test
    fun testBuySeedAction() {
        val initialState = GameState(
            player = PlayerProfile(cash = 500.0),
            inventory = StashInventory(seeds = emptyMap())
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuySeed("og_kush", count = 1)) // $150 seed

        val updated = vm.gameState.value
        assertEquals(350.0, updated.player.cash, 0.01)
        assertEquals(1, updated.inventory.seeds["og_kush"])
    }

    @Test
    fun testDealTransactionAction() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 12.0,
            dialogueText = "Need herb"
        )
        val initialState = GameState(
            player = PlayerProfile(cash = 100.0, currentXp = 0L),
            inventory = StashInventory(harvestedStock = mapOf("sour_diesel" to 20)),
            currentVisitor = customer
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.AcceptDeal("c1"))

        val updated = vm.gameState.value
        assertNull("Visitor leaves after deal", updated.currentVisitor)
        assertEquals(10, updated.inventory.harvestedStock["sour_diesel"])
        assertEquals(220.0, updated.player.cash, 0.01) // 100 + (10g * $12) = 220
        assertTrue(updated.player.currentXp > 0L)
    }
}
