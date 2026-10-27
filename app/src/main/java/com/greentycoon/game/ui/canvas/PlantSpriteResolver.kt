package com.greentycoon.game.ui.canvas

import com.greentycoon.game.R
import com.greentycoon.game.model.GrowthStage
import com.greentycoon.game.model.PlantInstance
import com.greentycoon.game.model.PotType

object PlantSpriteResolver {

    /**
     * Resolves the authentic Weed Firm drawable sprite for a given plant strain and growth stage.
     * Maps stages to the sprite sequences (weed_0_X to weed_4_X).
     * Falls back safely to weed_0_X if the strainId is unrecognized.
     */
    fun getPlantSprite(strainId: String, stage: GrowthStage): Int {
        if (stage == GrowthStage.EMPTY) {
            return R.drawable.weed_ground
        }

        val strainIndex = when (strainId.lowercase()) {
            "sour_diesel" -> 0
            "og_kush" -> 1
            "northern_lights" -> 2
            "purple_haze" -> 3
            "white_widow" -> 4
            else -> 0
        }

        return when (strainIndex) {
            0 -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_0_1
                GrowthStage.SPROUT -> R.drawable.weed_0_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_0_5
                GrowthStage.BUDDING -> R.drawable.weed_0_6
                GrowthStage.READY -> R.drawable.weed_0_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
            1 -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_1_1
                GrowthStage.SPROUT -> R.drawable.weed_1_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_1_5
                GrowthStage.BUDDING -> R.drawable.weed_1_6
                GrowthStage.READY -> R.drawable.weed_1_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
            2 -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_2_1
                GrowthStage.SPROUT -> R.drawable.weed_2_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_2_5
                GrowthStage.BUDDING -> R.drawable.weed_2_6
                GrowthStage.READY -> R.drawable.weed_2_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
            3 -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_3_1
                GrowthStage.SPROUT -> R.drawable.weed_3_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_3_5
                GrowthStage.BUDDING -> R.drawable.weed_3_6
                GrowthStage.READY -> R.drawable.weed_3_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
            4 -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_4_1
                GrowthStage.SPROUT -> R.drawable.weed_4_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_4_5
                GrowthStage.BUDDING -> R.drawable.weed_4_6
                GrowthStage.READY -> R.drawable.weed_4_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
            else -> when (stage) {
                GrowthStage.SEED -> R.drawable.weed_0_1
                GrowthStage.SPROUT -> R.drawable.weed_0_3
                GrowthStage.VEGETATIVE -> R.drawable.weed_0_5
                GrowthStage.BUDDING -> R.drawable.weed_0_6
                GrowthStage.READY -> R.drawable.weed_0_7
                GrowthStage.EMPTY -> R.drawable.weed_ground
            }
        }
    }

    /**
     * Convenience overload for resolving plant sprite directly from a PlantInstance.
     */
    fun getPlantSprite(plant: PlantInstance): Int =
        getPlantSprite(plant.strain.id, plant.stage)

    /**
     * Resolves the pot graphic for the given pot type.
     */
    fun getPotDrawable(potType: PotType): Int {
        return when (potType) {
            PotType.PLASTIC -> R.drawable.pot_0
            PotType.CLAY -> R.drawable.pot_1
            PotType.CERAMIC -> R.drawable.pot_1
            PotType.HYDRO_POT -> R.drawable.pot_2
        }
    }
}
