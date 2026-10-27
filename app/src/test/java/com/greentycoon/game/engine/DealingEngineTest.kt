package com.greentycoon.game.engine

import com.greentycoon.game.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class DealingEngineTest {

    @Test
    fun testNoCustomerGeneratedWhenStashIsEmpty() {
        val emptyInventory = StashInventory(harvestedStock = emptyMap())
        val state = GameState(inventory = emptyInventory)

        val visitor = DealingEngine.tryGenerateVisitor(state, random = Random(42))
        assertNull("Should not spawn customer when player has 0 grams in stock", visitor)
    }

    @Test
    fun testCustomerGeneratedWhenStockAvailable() {
        val stockInventory = StashInventory(
            harvestedStock = mapOf("sour_diesel" to 50, "og_kush" to 30)
        )
        val state = GameState(inventory = stockInventory)

        val visitor = DealingEngine.tryGenerateVisitor(state, random = Random(42))
        assertNotNull("Should spawn customer when stock is available", visitor)
        assertTrue(
            "Customer must request a strain the player actually has in stock",
            visitor!!.requestedStrainId in listOf("sour_diesel", "og_kush")
        )
        assertTrue("Requested grams must be > 0", visitor.requestedGrams > 0)
    }

    @Test
    fun testBargainingAcceptanceCurve() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need some herb"
        )

        // 1. Fair price or discount -> 100% acceptance
        val fairChance = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 10.0, playerRespect = 10)
        assertTrue("Fair price should have near 100% acceptance", fairChance >= 0.95f)

        // 2. Huge markup (+100% over base) -> Very low acceptance
        val greedyChance = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 20.0, playerRespect = 10)
        assertTrue("Double price should have < 15% acceptance", greedyChance < 0.15f)

        // 3. Higher respect score improves chances
        val highRespectChance = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 50)
        val lowRespectChance = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 5)
        assertTrue("Higher respect should yield higher chance", highRespectChance > lowRespectChance)
    }

    @Test
    fun testCalculateAcceptanceProbabilityFairAndDiscount() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need some herb"
        )

        // Exact fair price -> 1.0f
        val exactFair = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 10.0, playerRespect = 0)
        assertEquals(1.0f, exactFair, 0.001f)

        // Discounted price -> 1.0f
        val discounted = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 8.0, playerRespect = 0)
        assertEquals(1.0f, discounted, 0.001f)
    }

    @Test
    fun testCalculateAcceptanceProbabilityFiftyPercentMarkup() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Jane",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )

        // 50% markup: basePrice = 10.0, proposedPrice = 15.0
        // markupFraction = 0.5; baseChance = 1.0 - (0.5 * 1.3) = 0.35f (35%)
        val prob = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 0)
        assertEquals(0.35f, prob, 0.01f)
    }

    @Test
    fun testCalculateAcceptanceProbabilityRespectBonusScaling() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Vance",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Discreet deal"
        )

        // Base 50% markup -> 0.35f
        val probZeroRespect = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 0)
        // Respect 50 -> 50 * 0.001 = 0.05f bonus -> 0.40f
        val probFiftyRespect = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 50)
        // Respect 100 -> 100 * 0.001 = 0.10f bonus -> 0.45f
        val probHundredRespect = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 15.0, playerRespect = 100)

        assertEquals(0.35f, probZeroRespect, 0.005f)
        assertEquals(0.40f, probFiftyRespect, 0.005f)
        assertEquals(0.45f, probHundredRespect, 0.005f)
        assertEquals(0.05f, probFiftyRespect - probZeroRespect, 0.001f)
        assertEquals(0.05f, probHundredRespect - probFiftyRespect, 0.001f)
    }

    @Test
    fun testCalculateAcceptanceProbabilityClamping() {
        val customer = NpcCustomer(
            id = "c1",
            name = "Mike",
            requestedStrainId = "sour_diesel",
            requestedGrams = 10,
            baseOfferedPricePerGram = 10.0,
            currentOfferPricePerGram = 10.0,
            dialogueText = "Need herb"
        )

        // Extreme markup should clamp to minimum 0.02f
        val extremeMarkup = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 100.0, playerRespect = 0)
        assertEquals(0.02f, extremeMarkup, 0.0001f)

        // Slight markup with absurdly high respect should clamp to max 0.98f
        val maxClamp = DealingEngine.calculateAcceptanceProbability(customer, proposedPrice = 10.05, playerRespect = 1000)
        assertEquals(0.98f, maxClamp, 0.0001f)
    }

    @Test
    fun testBribeCalculationForCop() {
        val lowCashPlayer = PlayerProfile(cash = 200.0)
        val highCashPlayer = PlayerProfile(cash = 5000.0)

        val lowBribe = DealingEngine.calculateCopBribe(lowCashPlayer)
        val highBribe = DealingEngine.calculateCopBribe(highCashPlayer)

        assertEquals(100.0, lowBribe, 0.01) // Minimum bribe floor
        assertEquals(1000.0, highBribe, 0.01) // 20% of 5000
    }

    @Test
    fun testCalculateCopBribeFloorVsTwentyPercent() {
        // Zero cash -> floor $100
        assertEquals(100.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 0.0)), 0.001)

        // Below $500: 20% of $300 = $60 < $100 -> floor $100
        assertEquals(100.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 300.0)), 0.001)

        // Exact boundary at $500: 20% of $500 = $100 -> $100
        assertEquals(100.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 500.0)), 0.001)

        // Above $500: 20% of $750 = $150
        assertEquals(150.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 750.0)), 0.001)

        // 20% of $2,000 = $400
        assertEquals(400.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 2000.0)), 0.001)

        // 20% of $10,000 = $2,000
        assertEquals(2000.0, DealingEngine.calculateCopBribe(PlayerProfile(cash = 10000.0)), 0.001)
    }
}
