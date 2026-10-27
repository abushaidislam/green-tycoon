package com.greentycoon.game.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.greentycoon.game.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecureSaveManager {

    private const val PREFS_FILE_NAME = "secure_green_tycoon_save"
    private const val KEY_GAME_STATE = "enc_game_state_payload"
    private const val KEY_INTEGRITY_HMAC = "enc_game_state_hmac"
    
    // Internal secret key used for HMAC state tamper protection (independent of device hardware ID)
    private const val HMAC_INTERNAL_SALT = "GreenTycoonSecureInternalHmacSalt2026!#"

    /**
     * Serializes complete GameState to clean JSON string.
     */
    fun serializeToJson(state: GameState): String {
        val root = JSONObject()

        // 1. Player Profile
        val playerObj = JSONObject().apply {
            put("cash", state.player.cash)
            put("bank", state.player.bank)
            put("level", state.player.level)
            put("currentXp", state.player.currentXp)
            put("respectScore", state.player.respectScore)
            put("totalHarvestsGrams", state.player.totalHarvestsGrams)
        }
        root.put("player", playerObj)

        // 2. Pots
        val potsArray = JSONArray()
        for (pot in state.pots) {
            val potObj = JSONObject().apply {
                put("id", pot.id)
                put("potType", pot.potType.name)
                put("isUnlocked", pot.isUnlocked)
                if (pot.plant != null) {
                    val plantObj = JSONObject().apply {
                        put("strainId", pot.plant.strain.id)
                        put("growthProgress", pot.plant.growthProgress.toDouble())
                        put("moisture", pot.plant.moisture.toDouble())
                        put("health", pot.plant.health.toDouble())
                        put("stage", pot.plant.stage.name)
                        put("fertilizerActive", pot.plant.fertilizerActive)
                        put("plantedTimestamp", pot.plant.plantedTimestamp)
                    }
                    put("plant", plantObj)
                } else {
                    put("plant", JSONObject.NULL)
                }
            }
            potsArray.put(potObj)
        }
        root.put("pots", potsArray)

        // 3. Inventory
        val invObj = JSONObject().apply {
            val seedsObj = JSONObject()
            state.inventory.seeds.forEach { (k, v) -> seedsObj.put(k, v) }
            put("seeds", seedsObj)

            val stockObj = JSONObject()
            state.inventory.harvestedStock.forEach { (k, v) -> stockObj.put(k, v) }
            put("harvestedStock", stockObj)

            put("waterBottles", state.inventory.waterBottles)
            put("fertilizers", state.inventory.fertilizers)
        }
        root.put("inventory", invObj)

        // 4. Room Upgrades
        val roomObj = JSONObject().apply {
            put("lightLevel", state.roomUpgrades.lightLevel)
            put("ventilationLevel", state.roomUpgrades.ventilationLevel)
            put("vinylPlayerActive", state.roomUpgrades.vinylPlayerActive)
        }
        root.put("roomUpgrades", roomObj)

        root.put("lastSavedTimestamp", System.currentTimeMillis())

        return root.toString()
    }

    /**
     * Safely deserializes GameState from JSON string, returning null if malformed.
     */
    fun deserializeFromJson(jsonString: String): GameState? {
        return try {
            val root = JSONObject(jsonString)

            val pObj = root.getJSONObject("player")
            val player = PlayerProfile(
                cash = pObj.optDouble("cash", 250.0),
                bank = pObj.optDouble("bank", 0.0),
                level = pObj.optInt("level", 1),
                currentXp = pObj.optLong("currentXp", 0L),
                respectScore = pObj.optInt("respectScore", 10),
                totalHarvestsGrams = pObj.optLong("totalHarvestsGrams", 0L)
            )

            val potsArray = root.getJSONArray("pots")
            val pots = mutableListOf<PotSlot>()
            for (i in 0 until potsArray.length()) {
                val p = potsArray.getJSONObject(i)
                val id = p.getInt("id")
                val potType = try { PotType.valueOf(p.getString("potType")) } catch (_: Exception) { PotType.PLASTIC }
                val isUnlocked = p.optBoolean("isUnlocked", i == 0)
                
                val plant = if (!p.isNull("plant")) {
                    val pl = p.getJSONObject("plant")
                    val strain = PlantStrain.findById(pl.getString("strainId"))
                    PlantInstance(
                        strain = strain,
                        growthProgress = pl.getDouble("growthProgress").toFloat(),
                        moisture = pl.getDouble("moisture").toFloat(),
                        health = pl.optDouble("health", 100.0).toFloat(),
                        stage = try { GrowthStage.valueOf(pl.getString("stage")) } catch (_: Exception) { GrowthStage.SEED },
                        fertilizerActive = pl.optBoolean("fertilizerActive", false),
                        plantedTimestamp = pl.optLong("plantedTimestamp", System.currentTimeMillis())
                    )
                } else null

                pots.add(PotSlot(id = id, potType = potType, isUnlocked = isUnlocked, plant = plant))
            }

            // Ensure backward compatibility: pad legacy saves with fewer than 6 pots up to 6 slots
            while (pots.size < 6) {
                val nextId = pots.size
                pots.add(PotSlot(id = nextId, potType = PotType.PLASTIC, isUnlocked = false, plant = null))
            }

            val invObj = root.getJSONObject("inventory")
            val seedsMap = mutableMapOf<String, Int>()
            invObj.optJSONObject("seeds")?.let { sObj ->
                sObj.keys().forEach { k -> seedsMap[k] = sObj.getInt(k) }
            }
            val stockMap = mutableMapOf<String, Int>()
            invObj.optJSONObject("harvestedStock")?.let { stObj ->
                stObj.keys().forEach { k -> stockMap[k] = stObj.getInt(k) }
            }
            val inventory = StashInventory(
                seeds = seedsMap,
                harvestedStock = stockMap,
                waterBottles = invObj.optInt("waterBottles", 5),
                fertilizers = invObj.optInt("fertilizers", 2)
            )

            val rObj = root.optJSONObject("roomUpgrades")
            val upgrades = RoomUpgrades(
                lightLevel = rObj?.optInt("lightLevel", 1) ?: 1,
                ventilationLevel = rObj?.optInt("ventilationLevel", 1) ?: 1,
                vinylPlayerActive = rObj?.optBoolean("vinylPlayerActive", false) ?: false
            )

            val lastSaved = root.optLong("lastSavedTimestamp", System.currentTimeMillis())

            GameState(
                player = player,
                pots = pots,
                inventory = inventory,
                roomUpgrades = upgrades,
                lastSavedTimestamp = lastSaved
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes HMAC-SHA256 signature over data string.
     */
    fun computeHmac(data: String): String {
        return try {
            val key = SecretKeySpec(HMAC_INTERNAL_SALT.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(key)
            val bytes = mac.doFinal(data.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback digest if HMAC provider unavailable in test harness
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((data + HMAC_INTERNAL_SALT).toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Saves game state into hardware-backed EncryptedSharedPreferences.
     */
    fun saveGame(context: Context, state: GameState) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val json = serializeToJson(state)
            val hmac = computeHmac(json)

            prefs.edit()
                .putString(KEY_GAME_STATE, json)
                .putString(KEY_INTEGRITY_HMAC, hmac)
                .apply()
        } catch (e: Exception) {
            // Log warning or fallback
        }
    }

    /**
     * Loads game state, validating integrity.
     */
    fun loadGame(context: Context): GameState {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val json = prefs.getString(KEY_GAME_STATE, null) ?: return GameState()
            val savedHmac = prefs.getString(KEY_INTEGRITY_HMAC, null)
            val computedHmac = computeHmac(json)

            if (savedHmac != computedHmac) {
                // Save was tampered with; reset to fresh state
                return GameState()
            }

            return deserializeFromJson(json) ?: GameState()
        } catch (e: Exception) {
            return GameState()
        }
    }
}
