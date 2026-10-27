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
        assertEquals(6, restoredState.pots.size)
        assertEquals(PotType.CERAMIC, restoredState.pots[0].potType)
        assertEquals(45.5f, restoredState.pots[0].plant!!.growthProgress, 0.01f)
        assertEquals(25, restoredState.inventory.harvestedStock["og_kush"])
    }

    @Test
    fun testLegacyFourPotSaveMigrationToSixPots() {
        val legacyJson = """
            {
                "player": { "cash": 500.0, "level": 1, "currentXp": 0, "respectScore": 10, "totalHarvestsGrams": 0 },
                "pots": [
                    { "id": 0, "potType": "PLASTIC", "isUnlocked": true, "plant": null },
                    { "id": 1, "potType": "PLASTIC", "isUnlocked": true, "plant": null },
                    { "id": 2, "potType": "PLASTIC", "isUnlocked": false, "plant": null },
                    { "id": 3, "potType": "PLASTIC", "isUnlocked": false, "plant": null }
                ],
                "inventory": { "seeds": { "sour_diesel": 3 }, "harvestedStock": {}, "waterBottles": 5, "fertilizers": 2 },
                "roomUpgrades": { "lightLevel": 1, "ventilationLevel": 1, "vinylPlayerActive": false },
                "currentRoomSide": "PLANTS"
            }
        """.trimIndent()

        val state = SecureSaveManager.deserializeFromJson(legacyJson)
        assertNotNull(state)
        assertEquals(6, state!!.pots.size)
        assertEquals(0, state.pots[0].id)
        assertTrue(state.pots[0].isUnlocked)
        assertEquals(1, state.pots[1].id)
        assertTrue(state.pots[1].isUnlocked)
        assertEquals(2, state.pots[2].id)
        assertFalse(state.pots[2].isUnlocked)
        assertEquals(3, state.pots[3].id)
        assertFalse(state.pots[3].isUnlocked)
        assertEquals(4, state.pots[4].id)
        assertFalse(state.pots[4].isUnlocked)
        assertEquals(5, state.pots[5].id)
        assertFalse(state.pots[5].isUnlocked)
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
