package com.greentycoon.game.model

import org.junit.Assert.*
import org.junit.Test

class GameStateModelTest {

    @Test
    fun testInitialGameStateValues() {
        val state = GameState()

        assertEquals(250.0, state.player.cash, 0.001)
        assertEquals(1, state.player.level)
        assertEquals(4, state.pots.size)
        assertTrue("Pot 0 should be unlocked by default", state.pots[0].isUnlocked)
        assertFalse("Pot 1 should be locked initially", state.pots[1].isUnlocked)
        assertNull(state.pots[0].plant)
        assertEquals(3, state.inventory.seeds["sour_diesel"])
        assertEquals(0, state.inventory.totalWeedGrams)
        assertEquals(RoomSide.PLANTS, state.currentRoomSide)
    }

    @Test
    fun testPlayerProfileXpCalculations() {
        val profile = PlayerProfile(level = 1, currentXp = 60)
        // Level 1: 1 * 100 + 1 * 20 = 120 XP needed
        assertEquals(120L, profile.xpForNextLevel)
        assertEquals(0.5f, profile.xpProgressFraction, 0.01f)
    }

    @Test
    fun testStrainCatalogs() {
        val sour = PlantStrain.findById("sour_diesel")
        assertEquals("Sour Diesel", sour.name)
        assertEquals(25, sour.baseYieldGrams)
        assertEquals(60L, sour.baseGrowDurationSeconds)

        val og = PlantStrain.findById("og_kush")
        assertEquals(120L, og.baseGrowDurationSeconds)
    }

    @Test
    fun testRoomUpgradesMultiplier() {
        val room1 = RoomUpgrades(lightLevel = 1)
        assertEquals(1.0f, room1.lightGrowthMultiplier, 0.001f)

        val room4 = RoomUpgrades(lightLevel = 4)
        assertEquals(2.0f, room4.lightGrowthMultiplier, 0.001f)
    }
}
