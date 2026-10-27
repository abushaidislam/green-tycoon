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

    @Test
    fun testOfflineGrowthZeroOrNegativeDelta() {
        val startPlant = PlantInstance(strain = sourDiesel, growthProgress = 10f, moisture = 50f)
        val potSlot = PotSlot(id = 0, potType = plasticPot, isUnlocked = true, plant = startPlant)
        val pots = listOf(potSlot)

        val resultZero = GrowthEngine.calculateOfflineGrowth(pots, defaultRoom, offlineSeconds = 0f)
        assertSame("Zero delta should return same list", pots, resultZero)

        val resultNegative = GrowthEngine.calculateOfflineGrowth(pots, defaultRoom, offlineSeconds = -10f)
        assertSame("Negative delta should return same list", pots, resultNegative)
    }

    @Test
    fun testOfflineGrowthWithLockedOrEmptyPots() {
        val startPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, moisture = 50f)
        val pot0 = PotSlot(id = 0, potType = plasticPot, isUnlocked = true, plant = startPlant)
        val pot1 = PotSlot(id = 1, potType = plasticPot, isUnlocked = false, plant = null)
        val pot2 = PotSlot(id = 2, potType = plasticPot, isUnlocked = true, plant = null)
        val pots = listOf(pot0, pot1, pot2)

        val result = GrowthEngine.calculateOfflineGrowth(pots, defaultRoom, offlineSeconds = 20f)
        assertEquals(3, result.size)
        assertNotNull(result[0].plant)
        assertEquals(40f, result[0].plant!!.moisture, 0.01f) // 50 - (20 * 0.5) = 40
        assertNull(result[1].plant)
        assertFalse(result[1].isUnlocked)
        assertNull(result[2].plant)
        assertTrue(result[2].isUnlocked)
    }

    @Test
    fun testOfflineGrowthDryingOutMidway() {
        // Plant has 10f moisture. Rate is 0.5f/s, so water lasts exactly 20 seconds.
        val startPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, moisture = 10f, health = 100f)
        val potSlot = PotSlot(id = 0, potType = plasticPot, isUnlocked = true, plant = startPlant)

        // Offline for 40 seconds: 20 seconds moist + 20 seconds dry
        val result = GrowthEngine.calculateOfflineGrowth(listOf(potSlot), defaultRoom, offlineSeconds = 40f)
        val plant = result[0].plant!!

        assertEquals(0f, plant.moisture, 0.001f)
        // 20s of growth out of 60s base = 33.33%
        assertEquals(33.33f, plant.growthProgress, 0.1f)
        // 20s of drought * 0.5% decay per second = 10% health lost -> 90%
        assertEquals(90f, plant.health, 0.01f)
    }

    @Test
    fun testDryHealthDecayRateAndClampAtZero() {
        val dryPlant = PlantInstance(strain = sourDiesel, growthProgress = 10f, moisture = 0f, health = 100f)

        // 30 seconds dry -> 30 * 0.5 = 15% decay -> health = 85f
        val decayed = GrowthEngine.tickPlant(dryPlant, plasticPot, defaultRoom, deltaSeconds = 30f)
        assertEquals(85f, decayed.health, 0.01f)
        assertEquals("Growth should not advance when dry", 10f, decayed.growthProgress, 0.001f)

        // Extreme drought: 300 seconds -> health should clamp to 0f, not go negative
        val extremeDry = GrowthEngine.tickPlant(dryPlant, plasticPot, defaultRoom, deltaSeconds = 300f)
        assertEquals(0f, extremeDry.health, 0.001f)
    }

    @Test
    fun testMoistPlantRetainsFullHealth() {
        val healthyPlant = PlantInstance(strain = sourDiesel, growthProgress = 0f, moisture = 100f, health = 100f)

        // 10 seconds with plenty of water
        val updated = GrowthEngine.tickPlant(healthyPlant, plasticPot, defaultRoom, deltaSeconds = 10f)
        assertEquals(100f, updated.health, 0.001f)
        assertTrue(updated.growthProgress > 0f)
    }

    @Test
    fun testReadyPlantDoesNotTickFurther() {
        val readyPlant = PlantInstance(
            strain = sourDiesel,
            growthProgress = 100f,
            moisture = 50f,
            health = 90f,
            stage = GrowthStage.READY
        )

        val updated = GrowthEngine.tickPlant(readyPlant, plasticPot, defaultRoom, deltaSeconds = 100f)
        assertSame("Ready plant should return immediately without changes", readyPlant, updated)
    }

    @Test
    fun testCalculateYieldNotReadyPlant() {
        val immaturePlant = PlantInstance(strain = sourDiesel, stage = GrowthStage.VEGETATIVE, health = 100f)
        val yield = GrowthEngine.calculateYield(immaturePlant, PotType.PLASTIC)
        assertEquals(0, yield)

        val buddingPlant = PlantInstance(strain = sourDiesel, stage = GrowthStage.BUDDING, health = 100f)
        assertEquals(0, GrowthEngine.calculateYield(buddingPlant, PotType.HYDRO_POT))
    }

    @Test
    fun testCalculateYieldDifferentPotTypes() {
        // SOUR_DIESEL baseYieldGrams = 25
        val readyPlant = PlantInstance(strain = sourDiesel, stage = GrowthStage.READY, health = 100f)

        // Plastic: 1.0x -> 25g
        assertEquals(25, GrowthEngine.calculateYield(readyPlant, PotType.PLASTIC))

        // Ceramic: 1.25x -> (25 * 1.25).toInt() = 31g
        assertEquals(31, GrowthEngine.calculateYield(readyPlant, PotType.CERAMIC))

        // Terracotta Clay: 1.5x -> (25 * 1.5).toInt() = 37g
        assertEquals(37, GrowthEngine.calculateYield(readyPlant, PotType.CLAY))

        // Hydro Pot: 2.0x -> (25 * 2.0).toInt() = 50g
        assertEquals(50, GrowthEngine.calculateYield(readyPlant, PotType.HYDRO_POT))
    }

    @Test
    fun testCalculateYieldPlantHealthFractions() {
        // SOUR_DIESEL baseYield = 25g, Pot = Plastic (1.0x)
        val plant80Health = PlantInstance(strain = sourDiesel, stage = GrowthStage.READY, health = 80f)
        // 25 * 1.0 * 0.8 = 20g
        assertEquals(20, GrowthEngine.calculateYield(plant80Health, PotType.PLASTIC))

        val plant50Health = PlantInstance(strain = sourDiesel, stage = GrowthStage.READY, health = 50f)
        // 25 * 1.0 * 0.5 = 12g
        assertEquals(12, GrowthEngine.calculateYield(plant50Health, PotType.PLASTIC))

        // Plant health 5% should clamp to 0.1f (10% minimum fraction)
        val plant5Health = PlantInstance(strain = sourDiesel, stage = GrowthStage.READY, health = 5f)
        // 25 * 1.0 * 0.1 = 2g
        assertEquals(2, GrowthEngine.calculateYield(plant5Health, PotType.PLASTIC))

        // Combined: Hydro pot (2.0x) with 50% health -> 25 * 2.0 * 0.5 = 25g
        val hydroHalfHealth = PlantInstance(strain = sourDiesel, stage = GrowthStage.READY, health = 50f)
        assertEquals(25, GrowthEngine.calculateYield(hydroHalfHealth, PotType.HYDRO_POT))
    }
}
