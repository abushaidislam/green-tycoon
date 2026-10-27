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
    fun testBribeCalculationForCop() {
        val lowCashPlayer = PlayerProfile(cash = 200.0)
        val highCashPlayer = PlayerProfile(cash = 5000.0)

        val lowBribe = DealingEngine.calculateCopBribe(lowCashPlayer)
        val highBribe = DealingEngine.calculateCopBribe(highCashPlayer)

        assertEquals(100.0, lowBribe, 0.01) // Minimum bribe floor
        assertEquals(1000.0, highBribe, 0.01) // 20% of 5000
    }
}
