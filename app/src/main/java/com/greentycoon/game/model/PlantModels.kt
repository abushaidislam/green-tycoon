package com.greentycoon.game.model

enum class PotType(
    val displayName: String,
    val capacityMultiplier: Float,
    val moistureRetentionMultiplier: Float,
    val upgradePrice: Double
) {
    PLASTIC("Plastic Pot", 1.0f, 1.0f, 0.0),
    CERAMIC("Ceramic Pot", 1.25f, 1.3f, 500.0),
    CLAY("Terracotta Clay Pot", 1.5f, 1.6f, 1500.0),
    HYDRO_POT("Hydroponic Pot", 2.0f, 2.5f, 5000.0)
}

enum class GrowthStage {
    EMPTY,
    SEED,          // 0% - 15%
    SPROUT,        // 15% - 40%
    VEGETATIVE,    // 40% - 75%
    BUDDING,       // 75% - 99.9%
    READY          // 100%
}

data class PlantStrain(
    val id: String,
    val name: String,
    val baseGrowDurationSeconds: Long,
    val baseYieldGrams: Int,
    val baseSellPricePerGram: Double,
    val waterConsumptionRatePerSecond: Float,
    val requiredLevel: Int,
    val seedPrice: Double
) {
    companion object {
        val SOUR_DIESEL = PlantStrain(
            id = "sour_diesel",
            name = "Sour Diesel",
            baseGrowDurationSeconds = 60L,
            baseYieldGrams = 25,
            baseSellPricePerGram = 10.0,
            waterConsumptionRatePerSecond = 0.5f,
            requiredLevel = 1,
            seedPrice = 50.0
        )

        val OG_KUSH = PlantStrain(
            id = "og_kush",
            name = "OG Kush",
            baseGrowDurationSeconds = 120L,
            baseYieldGrams = 50,
            baseSellPricePerGram = 15.0,
            waterConsumptionRatePerSecond = 0.4f,
            requiredLevel = 2,
            seedPrice = 150.0
        )

        val NORTHERN_LIGHTS = PlantStrain(
            id = "northern_lights",
            name = "Northern Lights",
            baseGrowDurationSeconds = 240L,
            baseYieldGrams = 100,
            baseSellPricePerGram = 25.0,
            waterConsumptionRatePerSecond = 0.3f,
            requiredLevel = 4,
            seedPrice = 500.0
        )

        val PURPLE_HAZE = PlantStrain(
            id = "purple_haze",
            name = "Purple Haze",
            baseGrowDurationSeconds = 360L,
            baseYieldGrams = 150,
            baseSellPricePerGram = 40.0,
            waterConsumptionRatePerSecond = 0.25f,
            requiredLevel = 6,
            seedPrice = 1200.0
        )

        val ALL_STRAINS = listOf(SOUR_DIESEL, OG_KUSH, NORTHERN_LIGHTS, PURPLE_HAZE)
        
        fun findById(id: String): PlantStrain =
            ALL_STRAINS.find { it.id == id } ?: SOUR_DIESEL
    }
}

data class PlantInstance(
    val strain: PlantStrain,
    val growthProgress: Float = 0.0f,      // 0.0f to 100.0f
    val moisture: Float = 100.0f,          // 0.0f to 100.0f
    val health: Float = 100.0f,            // 0.0f to 100.0f
    val stage: GrowthStage = GrowthStage.SEED,
    val fertilizerActive: Boolean = false,
    val plantedTimestamp: Long = System.currentTimeMillis()
)

data class PotSlot(
    val id: Int,
    val potType: PotType = PotType.PLASTIC,
    val isUnlocked: Boolean = false,
    val plant: PlantInstance? = null
)
