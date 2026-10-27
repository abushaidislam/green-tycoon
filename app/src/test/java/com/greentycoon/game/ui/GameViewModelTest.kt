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

    @Test
    fun testAcceptDealUndercoverCopBribe() {
        val cop = NpcCustomer(
            id = "cop_1",
            name = "Officer Bob - undercover cop",
            type = CustomerType.UNDERCOVER_COP,
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 0.0,
            currentOfferPricePerGram = 0.0,
            dialogueText = "Police! Settle this right here."
        )
        val initialState = GameState(
            player = PlayerProfile(cash = 1000.0, respectScore = 10),
            currentVisitor = cop
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.AcceptDeal("cop_1"))

        val updated = vm.gameState.value
        assertNull("Cop leaves after bribe is accepted", updated.currentVisitor)
        // Bribe calculation: 20% of $1,000 = $200 -> remaining cash is $800
        assertEquals(800.0, updated.player.cash, 0.01)
        assertEquals(12, updated.player.respectScore) // Respect increases by +2
        assertEquals("Bribed Officer Bob ($200). Raid avoided!", updated.bannerNotification)
        assertTrue(updated.lastVisitorDepartureTimestamp > 0L)
    }

    @Test
    fun testFertilizePotSuccess() {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, fertilizerActive = false)
        val initialState = GameState(
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = plant)),
            inventory = StashInventory(fertilizers = 2)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.FertilizePot(potId = 0))

        val updated = vm.gameState.value
        assertEquals(1, updated.inventory.fertilizers)
        assertTrue("Plant should have fertilizer active", updated.pots[0].plant!!.fertilizerActive)
        assertEquals("Fertilizer applied! 1.5x Growth speed.", updated.bannerNotification)
    }

    @Test
    fun testFertilizePotAlreadyFertilized() {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, fertilizerActive = true)
        val initialState = GameState(
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = plant)),
            inventory = StashInventory(fertilizers = 2)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.FertilizePot(potId = 0))

        val updated = vm.gameState.value
        assertEquals("Fertilizer should not be consumed if already active", 2, updated.inventory.fertilizers)
        assertTrue(updated.pots[0].plant!!.fertilizerActive)
    }

    @Test
    fun testFertilizePotInsufficientFertilizer() {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, fertilizerActive = false)
        val initialState = GameState(
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = plant)),
            inventory = StashInventory(fertilizers = 0)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.FertilizePot(potId = 0))

        val updated = vm.gameState.value
        assertEquals(0, updated.inventory.fertilizers)
        assertFalse("Plant cannot be fertilized without stock", updated.pots[0].plant!!.fertilizerActive)
    }

    @Test
    fun testUnlockPotSufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 500.0),
            pots = listOf(
                PotSlot(id = 0, isUnlocked = true),
                PotSlot(id = 1, isUnlocked = true),
                PotSlot(id = 2, isUnlocked = false) // cost is 450.0
            )
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UnlockPot(potId = 2))

        val updated = vm.gameState.value
        assertTrue("Pot slot #2 should be unlocked", updated.pots[2].isUnlocked)
        assertEquals(50.0, updated.player.cash, 0.01) // 500 - 450
        assertEquals("Unlocked Pot Slot #3!", updated.bannerNotification)
    }

    @Test
    fun testUnlockPotInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 100.0),
            pots = listOf(
                PotSlot(id = 0, isUnlocked = true),
                PotSlot(id = 1, isUnlocked = true),
                PotSlot(id = 2, isUnlocked = false) // cost is 450.0
            )
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UnlockPot(potId = 2))

        val updated = vm.gameState.value
        assertFalse("Pot slot #2 should remain locked", updated.pots[2].isUnlocked)
        assertEquals(100.0, updated.player.cash, 0.01)
        assertEquals("Not enough cash!", updated.bannerNotification)
    }

    @Test
    fun testUnlockPotAlreadyUnlocked() {
        val initialState = GameState(
            player = PlayerProfile(cash = 500.0),
            pots = listOf(PotSlot(id = 0, isUnlocked = true))
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UnlockPot(potId = 0))

        val updated = vm.gameState.value
        assertEquals("Cash should remain unchanged", 500.0, updated.player.cash, 0.01)
        assertTrue(updated.pots[0].isUnlocked)
    }

    @Test
    fun testUpgradePotSufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 600.0),
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true))
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UpgradePot(potId = 0, targetPotType = PotType.CERAMIC)) // $500 cost

        val updated = vm.gameState.value
        assertEquals(PotType.CERAMIC, updated.pots[0].potType)
        assertEquals(100.0, updated.player.cash, 0.01) // 600 - 500
    }

    @Test
    fun testUpgradePotInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 200.0),
            pots = listOf(PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true))
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UpgradePot(potId = 0, targetPotType = PotType.CERAMIC)) // $500 cost

        val updated = vm.gameState.value
        assertEquals("Pot should remain plastic", PotType.PLASTIC, updated.pots[0].potType)
        assertEquals(200.0, updated.player.cash, 0.01)
    }

    @Test
    fun testBuySeedInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 20.0),
            inventory = StashInventory(seeds = emptyMap())
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuySeed("sour_diesel", count = 1)) // $50 seed

        val updated = vm.gameState.value
        assertEquals(20.0, updated.player.cash, 0.01)
        assertEquals(0, updated.inventory.seeds["sour_diesel"] ?: 0)
        assertEquals("Not enough cash!", updated.bannerNotification)
    }

    @Test
    fun testBuyWaterBottleSufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 100.0),
            inventory = StashInventory(waterBottles = 2)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuyWaterBottle(count = 2)) // 2 packs @ $25 = $50 -> adds 6 bottles

        val updated = vm.gameState.value
        assertEquals(50.0, updated.player.cash, 0.01)
        assertEquals(8, updated.inventory.waterBottles) // 2 + 6
        assertEquals("Purchased Water Bottles pack!", updated.bannerNotification)
    }

    @Test
    fun testBuyWaterBottleInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 15.0),
            inventory = StashInventory(waterBottles = 1)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuyWaterBottle(count = 1)) // 1 pack @ $25

        val updated = vm.gameState.value
        assertEquals(15.0, updated.player.cash, 0.01)
        assertEquals(1, updated.inventory.waterBottles)
    }

    @Test
    fun testBuyFertilizerSufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 100.0),
            inventory = StashInventory(fertilizers = 1)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuyFertilizer(count = 2)) // 2 packs @ $40 = $80 -> adds 4 fertilizers

        val updated = vm.gameState.value
        assertEquals(20.0, updated.player.cash, 0.01)
        assertEquals(5, updated.inventory.fertilizers) // 1 + 4
        assertEquals("Purchased Growth Fertilizer pack!", updated.bannerNotification)
    }

    @Test
    fun testBuyFertilizerInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 20.0),
            inventory = StashInventory(fertilizers = 0)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BuyFertilizer(count = 1)) // 1 pack @ $40

        val updated = vm.gameState.value
        assertEquals(20.0, updated.player.cash, 0.01)
        assertEquals(0, updated.inventory.fertilizers)
    }

    @Test
    fun testUpgradeLightSufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 500.0),
            roomUpgrades = RoomUpgrades(lightLevel = 1)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UpgradeLight(newLevel = 2)) // Level 2 costs $250

        val updated = vm.gameState.value
        assertEquals(250.0, updated.player.cash, 0.01)
        assertEquals(2, updated.roomUpgrades.lightLevel)
        assertEquals("Upgraded Grow Lights to Level 2!", updated.bannerNotification)
    }

    @Test
    fun testUpgradeLightInsufficientCash() {
        val initialState = GameState(
            player = PlayerProfile(cash = 100.0),
            roomUpgrades = RoomUpgrades(lightLevel = 1)
        )
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.UpgradeLight(newLevel = 2)) // Level 2 costs $250

        val updated = vm.gameState.value
        assertEquals(100.0, updated.player.cash, 0.01)
        assertEquals(1, updated.roomUpgrades.lightLevel)
    }

    @Test
    fun testBargainDealCounterOffer() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )
        val initialState = GameState(currentVisitor = customer)
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BargainDeal(visitorId = "c1", proposedPrice = 14.5))

        val updated = vm.gameState.value
        assertNotNull(updated.currentVisitor)
        assertEquals(14.5, updated.currentVisitor!!.currentOfferPricePerGram, 0.001)
    }

    @Test
    fun testBargainDealWrongVisitorId() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )
        val initialState = GameState(currentVisitor = customer)
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.BargainDeal(visitorId = "wrong_id", proposedPrice = 15.0))

        val updated = vm.gameState.value
        assertEquals(10.0, updated.currentVisitor!!.currentOfferPricePerGram, 0.001)
    }

    @Test
    fun testKickCustomer() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )
        val initialState = GameState(currentVisitor = customer)
        val vm = GameViewModel(initialState = initialState, enableLoop = false)

        vm.handleAction(GameAction.KickCustomer(visitorId = "c1"))

        val updated = vm.gameState.value
        assertNull("Customer should be kicked out", updated.currentVisitor)
        assertEquals("Kicked Mike out!", updated.bannerNotification)
        assertTrue(updated.lastVisitorDepartureTimestamp > 0L)
    }
}
