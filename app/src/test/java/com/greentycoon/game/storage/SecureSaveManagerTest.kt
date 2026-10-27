package com.greentycoon.game.storage

import com.greentycoon.game.model.*
import org.junit.Assert.*
import org.junit.Test

class SecureSaveManagerTest {

    @Test
    fun testJsonSerializationRoundtrip() {
        val originalPlant = PlantInstance(
            strain = PlantStrain.SOUR_DIESEL,
            growthProgress = 45.5f,
            moisture = 80.0f,
            stage = GrowthStage.VEGETATIVE
        )
        val originalState = GameState(
            player = PlayerProfile(cash = 1250.0, level = 3, currentXp = 150),
            pots = listOf(
                PotSlot(id = 0, potType = PotType.CERAMIC, isUnlocked = true, plant = originalPlant),
                PotSlot(id = 1, potType = PotType.PLASTIC, isUnlocked = true, plant = null)
            ),
            inventory = StashInventory(
                seeds = mapOf("sour_diesel" to 5),
                harvestedStock = mapOf("og_kush" to 25)
            )
        )

        val json = SecureSaveManager.serializeToJson(originalState)
        assertNotNull(json)
        assertTrue(json.contains("1250"))
        assertTrue(json.contains("sour_diesel"))

        val restoredState = SecureSaveManager.deserializeFromJson(json)
        assertNotNull(restoredState)
        assertEquals(1250.0, restoredState!!.player.cash, 0.001)
        assertEquals(3, restoredState.player.level)
        assertEquals(2, restoredState.pots.size)
        assertEquals(PotType.CERAMIC, restoredState.pots[0].potType)
        assertEquals(45.5f, restoredState.pots[0].plant!!.growthProgress, 0.01f)
        assertEquals(25, restoredState.inventory.harvestedStock["og_kush"])
    }

    @Test
    fun testCorruptedJsonSafelyReturnsNull() {
        val malformedJson = "{ \"player\": { \"cash\": INVALID_NUMBER } }"
        val result = SecureSaveManager.deserializeFromJson(malformedJson)
        assertNull("Malformed JSON must safely return null instead of throwing unhandled exception", result)
    }

    @Test
    fun testHmacIntegrityCalculation() {
        val payload = "{\"cash\":100}"
        val signature1 = SecureSaveManager.computeHmac(payload)
        val signature2 = SecureSaveManager.computeHmac(payload)
        val alteredSignature = SecureSaveManager.computeHmac("{\"cash\":999999}")

        assertEquals(signature1, signature2)
        assertNotEquals(signature1, alteredSignature)
    }
}
