package com.greentycoon.game.ui

import com.greentycoon.game.R
import com.greentycoon.game.model.GrowthStage
import com.greentycoon.game.model.PlantInstance
import com.greentycoon.game.model.PlantStrain
import com.greentycoon.game.model.PotType
import com.greentycoon.game.ui.canvas.PlantSpriteResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlantSpriteResolverTest {

    @Test
    fun testPlantStageSpritesReturnValidDrawables() {
        for (stage in GrowthStage.values()) {
            val resId = PlantSpriteResolver.getPlantSprite(strainId = "sour_diesel", stage = stage)
            assertNotEquals("Stage $stage must map to a valid drawable", 0, resId)
        }
    }

    @Test
    fun testPotDrawablesReturnValidDrawables() {
        for (potType in PotType.values()) {
            val resId = PlantSpriteResolver.getPotDrawable(potType)
            assertNotEquals("PotType $potType must map to a valid drawable", 0, resId)
        }
    }

    @Test
    fun testPotDrawablesExactMapping() {
        assertEquals(R.drawable.pot_0, PlantSpriteResolver.getPotDrawable(PotType.PLASTIC))
        assertEquals(R.drawable.pot_1, PlantSpriteResolver.getPotDrawable(PotType.CLAY))
        assertEquals(R.drawable.pot_1, PlantSpriteResolver.getPotDrawable(PotType.CERAMIC))
        assertEquals(R.drawable.pot_2, PlantSpriteResolver.getPotDrawable(PotType.HYDRO_POT))
    }

    @Test
    fun testStrainSpriteSequencesAndFallback() {
        // Primary strain
        assertEquals(R.drawable.weed_0_7, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.READY))
        // Secondary strains mapping
        assertEquals(R.drawable.weed_1_7, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.READY))
        assertEquals(R.drawable.weed_2_7, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.READY))
        assertEquals(R.drawable.weed_3_7, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.READY))
        assertEquals(R.drawable.weed_4_7, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.READY))
        // Fallback for unknown strain
        assertEquals(R.drawable.weed_0_7, PlantSpriteResolver.getPlantSprite("unknown_strain", GrowthStage.READY))
    }

    @Test
    fun testAllGrowthStagesForPrimaryStrain() {
        assertEquals(R.drawable.weed_ground, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.EMPTY))
        assertEquals(R.drawable.weed_0_1, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.SEED))
        assertEquals(R.drawable.weed_0_3, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.SPROUT))
        assertEquals(R.drawable.weed_0_5, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.VEGETATIVE))
        assertEquals(R.drawable.weed_0_6, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.BUDDING))
        assertEquals(R.drawable.weed_0_7, PlantSpriteResolver.getPlantSprite("sour_diesel", GrowthStage.READY))
    }

    @Test
    fun testAllGrowthStagesForSecondaryStrains() {
        // og_kush
        assertEquals(R.drawable.weed_1_1, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.SEED))
        assertEquals(R.drawable.weed_1_3, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.SPROUT))
        assertEquals(R.drawable.weed_1_5, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.VEGETATIVE))
        assertEquals(R.drawable.weed_1_6, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.BUDDING))
        assertEquals(R.drawable.weed_1_7, PlantSpriteResolver.getPlantSprite("og_kush", GrowthStage.READY))

        // northern_lights
        assertEquals(R.drawable.weed_2_1, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.SEED))
        assertEquals(R.drawable.weed_2_3, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.SPROUT))
        assertEquals(R.drawable.weed_2_5, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.VEGETATIVE))
        assertEquals(R.drawable.weed_2_6, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.BUDDING))
        assertEquals(R.drawable.weed_2_7, PlantSpriteResolver.getPlantSprite("northern_lights", GrowthStage.READY))

        // purple_haze
        assertEquals(R.drawable.weed_3_1, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.SEED))
        assertEquals(R.drawable.weed_3_3, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.SPROUT))
        assertEquals(R.drawable.weed_3_5, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.VEGETATIVE))
        assertEquals(R.drawable.weed_3_6, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.BUDDING))
        assertEquals(R.drawable.weed_3_7, PlantSpriteResolver.getPlantSprite("purple_haze", GrowthStage.READY))

        // white_widow
        assertEquals(R.drawable.weed_4_1, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.SEED))
        assertEquals(R.drawable.weed_4_3, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.SPROUT))
        assertEquals(R.drawable.weed_4_5, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.VEGETATIVE))
        assertEquals(R.drawable.weed_4_6, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.BUDDING))
        assertEquals(R.drawable.weed_4_7, PlantSpriteResolver.getPlantSprite("white_widow", GrowthStage.READY))
    }

    @Test
    fun testPlantInstanceConvenienceOverload() {
        val plant = PlantInstance(strain = PlantStrain.SOUR_DIESEL, stage = GrowthStage.BUDDING)
        assertEquals(R.drawable.weed_0_6, PlantSpriteResolver.getPlantSprite(plant))
    }

    @Test
    fun testCaseInsensitiveStrainResolution() {
        assertEquals(R.drawable.weed_0_7, PlantSpriteResolver.getPlantSprite("SOUR_DIESEL", GrowthStage.READY))
        assertEquals(R.drawable.weed_1_7, PlantSpriteResolver.getPlantSprite("OG_Kush", GrowthStage.READY))
        assertEquals(R.drawable.weed_2_7, PlantSpriteResolver.getPlantSprite("Northern_Lights", GrowthStage.READY))
    }
}
