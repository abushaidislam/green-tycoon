package com.greentycoon.game.engine

import com.greentycoon.game.model.*
import org.junit.Assert.*
import org.junit.Test

class GrowthEngineTest {

    private val sourDiesel = PlantStrain.SOUR_DIESEL // 60s base, 0.5f water/s
    private val plasticPot = PotType.PLASTIC
    private val defaultRoom = RoomUpgrades()

    @Test
    fun testMoistureDecaysOverTime() {
        val initialPlant = PlantInstance(strain = sourDiesel, moisture = 100f)
        
        // 10 seconds of time elapsed
        val updated = GrowthEngine.tickPlant(initialPlant, plasticPot, defaultRoom, deltaSeconds = 10f)

        // 10s * 0.5f water/s = 5.0f consumed
        assertEquals(95f, updated.moisture, 0.01f)
    }

    @Test
    fun testGrowthAdvancesWhenMoist() {
        val initialPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, moisture = 100f)

        // 30 seconds out of 60s base = 50% progress
        val updated = GrowthEngine.tickPlant(initialPlant, plasticPot, defaultRoom, deltaSeconds = 30f)

        assertEquals(50f, updated.growthProgress, 0.1f)
        assertEquals(GrowthStage.VEGETATIVE, updated.stage)
    }

    @Test
    fun testGrowthFreezesWhenDry() {
        // Plant has 0 moisture
        val dryPlant = PlantInstance(strain = sourDiesel, growthProgress = 20f, moisture = 0f)

        val updated = GrowthEngine.tickPlant(dryPlant, plasticPot, defaultRoom, deltaSeconds = 10f)

        // Progress must stay unchanged at 20f
        assertEquals(20f, updated.growthProgress, 0.001f)
        // Health should decay when dry (0.5% per second * 10s = 5% decay)
        assertEquals(95f, updated.health, 0.01f)
    }

    @Test
    fun testFertilizerBoostsGrowthSpeed() {
        val normalPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, fertilizerActive = false)
        val fertPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, fertilizerActive = true)

        val updatedNormal = GrowthEngine.tickPlant(normalPlant, plasticPot, defaultRoom, deltaSeconds = 10f)
        val updatedFert = GrowthEngine.tickPlant(fertPlant, plasticPot, defaultRoom, deltaSeconds = 10f)

        // Fertilizer gives 1.5x growth boost
        assertEquals(updatedNormal.growthProgress * 1.5f, updatedFert.growthProgress, 0.01f)
    }

    @Test
    fun testStageTransitions() {
        assertEquals(GrowthStage.SEED, GrowthEngine.resolveStage(10f))
        assertEquals(GrowthStage.SPROUT, GrowthEngine.resolveStage(25f))
        assertEquals(GrowthStage.VEGETATIVE, GrowthEngine.resolveStage(60f))
        assertEquals(GrowthStage.BUDDING, GrowthEngine.resolveStage(85f))
        assertEquals(GrowthStage.READY, GrowthEngine.resolveStage(100f))
    }

    @Test
    fun testOfflineDeltaProgressionMatchesSimulatedSteps() {
        val startPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, moisture = 50f)
        val potSlot = PotSlot(id = 0, potType = plasticPot, isUnlocked = true, plant = startPlant)
        val pots = listOf(potSlot)

        // Simulate 40 seconds of offline time
        val offlinePots = GrowthEngine.calculateOfflineGrowth(pots, defaultRoom, offlineSeconds = 40f)
        val plantResult = offlinePots[0].plant!!

        // In 40s, moisture consumption: 40 * 0.5 = 20 consumed -> remaining 30f
        assertEquals(30f, plantResult.moisture, 0.01f)
        // Progress in 40s (60s total): 40/60 * 100 = 66.67%
        assertEquals(66.67f, plantResult.growthProgress, 0.1f)
    }
}
