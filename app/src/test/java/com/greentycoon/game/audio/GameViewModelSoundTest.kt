package com.greentycoon.game.audio

import com.greentycoon.game.model.*
import com.greentycoon.game.ui.GameViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelSoundTest {

    @Test
    fun testWaterPotEmitsWaterSoundEvent() = runTest {
        val dryPlant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, moisture = 10f)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = dryPlant)),
            inventory = StashInventory(waterBottles = 2)
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            emitted = vm.soundEvents.first()
        }

        vm.handleAction(GameAction.WaterPot(potId = 0))
        job.join()

        assertEquals(GameSoundEvent.Water, emitted)
    }

    @Test
    fun testWaterPotWithZeroBottlesDoesNotEmitSound() = runTest {
        val dryPlant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, moisture = 10f)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = dryPlant)),
            inventory = StashInventory(waterBottles = 0)
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            vm.soundEvents.collect { emitted = it }
        }

        vm.handleAction(GameAction.WaterPot(potId = 0))
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertNull(emitted)
    }

    @Test
    fun testHarvestPotEmitsHarvestSoundEvent() = runTest {
        val readyPlant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, growthProgress = 100f, stage = GrowthStage.READY)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = readyPlant))
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            emitted = vm.soundEvents.first()
        }

        vm.handleAction(GameAction.HarvestPot(potId = 0))
        job.join()

        assertEquals(GameSoundEvent.Harvest, emitted)
    }

    @Test
    fun testHarvestNonReadyPlantDoesNotEmitSound() = runTest {
        val growingPlant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, growthProgress = 50f, stage = GrowthStage.VEGETATIVE)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = growingPlant))
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            vm.soundEvents.collect { emitted = it }
        }

        vm.handleAction(GameAction.HarvestPot(potId = 0))
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertNull(emitted)
    }

    @Test
    fun testFertilizePotEmitsFertilizeSoundEvent() = runTest {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, fertilizerActive = false)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = plant)),
            inventory = StashInventory(fertilizers = 2)
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            emitted = vm.soundEvents.first()
        }

        vm.handleAction(GameAction.FertilizePot(potId = 0))
        job.join()

        assertEquals(GameSoundEvent.Fertilize, emitted)
    }

    @Test
    fun testFertilizeAlreadyActivePlantDoesNotEmitSound() = runTest {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, fertilizerActive = true)
        val state = GameState(
            pots = listOf(PotSlot(id = 0, isUnlocked = true, plant = plant)),
            inventory = StashInventory(fertilizers = 2)
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            vm.soundEvents.collect { emitted = it }
        }

        vm.handleAction(GameAction.FertilizePot(potId = 0))
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertNull(emitted)
    }

    @Test
    fun testAcceptDealEmitsCashEarnedSoundEvent() = runTest {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 5,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )
        val state = GameState(
            player = PlayerProfile(cash = 100.0),
            inventory = StashInventory(harvestedStock = mapOf("sour_diesel" to 10)),
            currentVisitor = customer
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            emitted = vm.soundEvents.first()
        }

        vm.handleAction(GameAction.AcceptDeal("c1"))
        job.join()

        assertEquals(GameSoundEvent.CashEarned, emitted)
    }

    @Test
    fun testAcceptDealWithoutEnoughGramsDoesNotEmitSound() = runTest {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 5,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )
        val state = GameState(
            player = PlayerProfile(cash = 100.0),
            inventory = StashInventory(harvestedStock = mapOf("sour_diesel" to 0)),
            currentVisitor = customer
        )
        val vm = GameViewModel(initialState = state, enableLoop = false)

        var emitted: GameSoundEvent? = null
        val job = launch {
            vm.soundEvents.collect { emitted = it }
        }

        vm.handleAction(GameAction.AcceptDeal("c1"))
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertNull(emitted)
    }

    @Test
    fun testSoundManagerMuteToggle() {
        SoundManager.setMuted(false)
        assertFalse(SoundManager.isMuted())
        assertTrue(SoundManager.toggleMute())
        assertTrue(SoundManager.isMuted())
        assertFalse(SoundManager.toggleMute())
        assertFalse(SoundManager.isMuted())
    }

    @Test
    fun testSoundManagerPlaySafeWhenUninitialized() {
        SoundManager.play(GameSoundEvent.DoorKnock)
        SoundManager.play(GameSoundEvent.DoorOpen)
        SoundManager.play(GameSoundEvent.DoorClose)
    }
}
