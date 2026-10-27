package com.greentycoon.game.ui

import com.greentycoon.game.R
import com.greentycoon.game.model.*
import com.greentycoon.game.ui.canvas.PlantSpriteResolver
import org.junit.Assert.*
import org.junit.Test

class PanoramicRoomLogicTest {

    @Test
    fun testPotUnlockCosts() {
        val state = GameState()
        assertEquals(0.0, state.calculateUnlockCost(0), 0.001)
        assertEquals(150.0, state.calculateUnlockCost(1), 0.001)
        assertEquals(450.0, state.calculateUnlockCost(2), 0.001)
        assertEquals(1000.0, state.calculateUnlockCost(3), 0.001)
        assertEquals(2500.0, state.calculateUnlockCost(4), 0.001)
        assertEquals(5000.0, state.calculateUnlockCost(5), 0.001)
    }

    @Test
    fun testPotsGridStructure() {
        val state = GameState()
        val chunked = state.pots.chunked(3)
        assertTrue("Pots should be chunked into rows of at most 3", chunked.all { it.size <= 3 })
        assertEquals(6, state.pots.size)
        assertEquals(2, chunked.size)
        assertEquals(3, chunked[0].size)
        assertEquals(3, chunked[1].size)
    }

    @Test
    fun testEmptyPotGraphics() {
        val emptyPot = PotSlot(id = 0, potType = PotType.PLASTIC, isUnlocked = true, plant = null)
        val potBase = PlantSpriteResolver.getPotDrawable(emptyPot.potType)
        val soil = R.drawable.weed_ground

        assertEquals(R.drawable.pot_0, potBase)
        assertEquals(R.drawable.weed_ground, soil)
    }

    @Test
    fun testGrowingPotGraphics() {
        val growingPlant = PlantInstance(
            strain = PlantStrain.SOUR_DIESEL,
            stage = GrowthStage.VEGETATIVE,
            growthProgress = 50f,
            moisture = 75f
        )
        val pot = PotSlot(id = 1, potType = PotType.CERAMIC, isUnlocked = true, plant = growingPlant)

        val potBase = PlantSpriteResolver.getPotDrawable(pot.potType)
        val plantSprite = PlantSpriteResolver.getPlantSprite(growingPlant)

        assertEquals(R.drawable.pot_1, potBase)
        assertEquals(R.drawable.weed_0_5, plantSprite)
    }

    @Test
    fun testReadyPotHarvestGraphics() {
        val readyPlant = PlantInstance(
            strain = PlantStrain.PURPLE_HAZE,
            stage = GrowthStage.READY,
            growthProgress = 100f
        )
        val pot = PotSlot(id = 2, potType = PotType.HYDRO_POT, isUnlocked = true, plant = readyPlant)

        val potBase = PlantSpriteResolver.getPotDrawable(pot.potType)
        val plantSprite = PlantSpriteResolver.getPlantSprite(readyPlant)

        assertEquals(R.drawable.pot_2, potBase)
        assertEquals(R.drawable.weed_3_7, plantSprite)
    }

    @Test
    fun testGrowLightLevels() {
        val light1 = RoomUpgrades(lightLevel = 1)
        val light2 = RoomUpgrades(lightLevel = 2)
        val light3 = RoomUpgrades(lightLevel = 3)
        val light4 = RoomUpgrades(lightLevel = 4)

        assertEquals(1.0f, light1.lightGrowthMultiplier, 0.001f)
        assertEquals(1.25f, light2.lightGrowthMultiplier, 0.001f)
        assertEquals(1.5f, light3.lightGrowthMultiplier, 0.001f)
        assertEquals(2.0f, light4.lightGrowthMultiplier, 0.001f)
    }

    @Test
    fun testDoorKnockState() {
        val idleState = GameState(currentVisitor = null)
        assertNull(idleState.currentVisitor)

        val visitor = NpcCustomer(
            id = "mike_1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 12.0,
            currentOfferPricePerGram = 12.0,
            patienceSeconds = 30,
            dialogueText = "Looking for good smoke!",
            avatarId = "mike"
        )
        val visitingState = GameState(currentVisitor = visitor)
        assertNotNull(visitingState.currentVisitor)
        assertEquals("Mike", visitingState.currentVisitor?.name)
    }
}
