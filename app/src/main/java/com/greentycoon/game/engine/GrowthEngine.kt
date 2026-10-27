package com.greentycoon.game.engine

import com.greentycoon.game.model.*
import kotlin.math.max
import kotlin.math.min

object GrowthEngine {

    const val DRY_HEALTH_DECAY_RATE_PER_SECOND = 0.5f
    const val FERTILIZER_GROWTH_MULTIPLIER = 1.5f

    /**
     * Advances a single plant by deltaSeconds.
     */
    fun tickPlant(
        plant: PlantInstance,
        potType: PotType,
        room: RoomUpgrades,
        deltaSeconds: Float
    ): PlantInstance {
        if (plant.stage == GrowthStage.READY || deltaSeconds <= 0f) {
            return plant
        }

        // 1. Calculate water consumption
        val effectiveWaterRate = (plant.strain.waterConsumptionRatePerSecond / potType.moistureRetentionMultiplier)
        val moistureConsumed = effectiveWaterRate * deltaSeconds
        val newMoisture = max(0f, plant.moisture - moistureConsumed)

        // 2. Growth occurs while moisture > 0
        val moistTimeFraction = if (plant.moisture <= 0f) {
            0f
        } else if (plant.moisture >= moistureConsumed) {
            1f
        } else {
            // ran out of water midway
            (plant.moisture / moistureConsumed).coerceIn(0f, 1f)
        }
        val effectiveGrowthSeconds = deltaSeconds * moistTimeFraction
        val drySeconds = deltaSeconds - effectiveGrowthSeconds

        // 3. Compute growth rate
        val fertilizerMultiplier = if (plant.fertilizerActive) FERTILIZER_GROWTH_MULTIPLIER else 1.0f
        val lightMultiplier = room.lightGrowthMultiplier
        val totalSpeed = fertilizerMultiplier * lightMultiplier

        val progressPerSecond = (100.0f / plant.strain.baseGrowDurationSeconds) * totalSpeed
        val progressDelta = progressPerSecond * effectiveGrowthSeconds
        val newProgress = min(100.0f, plant.growthProgress + progressDelta)

        // 4. Compute health decay if dry
        val healthDecay = drySeconds * DRY_HEALTH_DECAY_RATE_PER_SECOND
        val newHealth = max(0f, plant.health - healthDecay)

        val newStage = resolveStage(newProgress)

        return plant.copy(
            growthProgress = newProgress,
            moisture = newMoisture,
            health = newHealth,
            stage = newStage
        )
    }

    /**
     * Resolves the visual growth stage from progress percentage.
     */
    fun resolveStage(progress: Float): GrowthStage {
        return when {
            progress >= 100.0f -> GrowthStage.READY
            progress >= 75.0f -> GrowthStage.BUDDING
            progress >= 40.0f -> GrowthStage.VEGETATIVE
            progress >= 15.0f -> GrowthStage.SPROUT
            else -> GrowthStage.SEED
        }
    }

    /**
     * Deterministically calculates plant growth across an offline duration (seconds).
     */
    fun calculateOfflineGrowth(
        pots: List<PotSlot>,
        room: RoomUpgrades,
        offlineSeconds: Float
    ): List<PotSlot> {
        if (offlineSeconds <= 0f) return pots

        return pots.map { pot ->
            val plant = pot.plant
            if (pot.isUnlocked && plant != null) {
                pot.copy(plant = tickPlant(plant, pot.potType, room, offlineSeconds))
            } else {
                pot
            }
        }
    }

    /**
     * Waters a plant back to 100% moisture.
     */
    fun waterPlant(plant: PlantInstance): PlantInstance {
        return plant.copy(moisture = 100f)
    }

    /**
     * Applies fertilizer to accelerate growth.
     */
    fun fertilizePlant(plant: PlantInstance): PlantInstance {
        return plant.copy(fertilizerActive = true)
    }

    /**
     * Calculates the yield in grams when harvesting a mature plant.
     */
    fun calculateYield(plant: PlantInstance, potType: PotType): Int {
        if (plant.stage != GrowthStage.READY) return 0
        val base = plant.strain.baseYieldGrams.toFloat()
        val capacityBonus = potType.capacityMultiplier
        val healthFraction = (plant.health / 100f).coerceIn(0.1f, 1.0f)
        return (base * capacityBonus * healthFraction).toInt()
    }
}
