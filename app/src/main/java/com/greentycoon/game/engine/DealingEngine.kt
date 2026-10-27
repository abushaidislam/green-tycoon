package com.greentycoon.game.engine

import com.greentycoon.game.model.*
import java.util.Random
import java.util.UUID
import kotlin.math.max

object DealingEngine {

    private data class NpcTemplate(
        val name: String,
        val type: CustomerType,
        val dialogues: List<String>,
        val avatarId: String
    )

    private val CUSTOMER_TEMPLATES = listOf(
        NpcTemplate(
            name = "Mike - the plumber",
            type = CustomerType.REGULAR_BUYER,
            dialogues = listOf(
                "Oh those were the days I'm telling you, those were the days!",
                "Got a clogged pipe today... really need some good weed to unwind.",
                "Hey buddy, give me the fresh green stuff."
            ),
            avatarId = "mike"
        ),
        NpcTemplate(
            name = "Jane - college student",
            type = CustomerType.COLLEGE_STUDENT,
            dialogues = listOf(
                "Peace! Finals week is killing me...",
                "Hey, got any Sour Diesel? Need some creative energy.",
                "Hope no one saw me walk up to your door!"
            ),
            avatarId = "jane"
        ),
        NpcTemplate(
            name = "Mr. Vance - business VIP",
            type = CustomerType.BUSINESS_VIP,
            dialogues = listOf(
                "Keep it discreet. I've got money if the quality matches.",
                "Long board meeting. Pack me your highest grade.",
                "Time is money, my friend. Let's make a quick deal."
            ),
            avatarId = "vance"
        ),
        NpcTemplate(
            name = "Officer Bob - undercover cop",
            type = CustomerType.UNDERCOVER_COP,
            dialogues = listOf(
                "Police! Step back! We received complaints about a strong odor.",
                "Looks like you're running an unlicensed operation here...",
                "Either we take a trip downtown, or we can settle this right here."
            ),
            avatarId = "cop"
        ),
        NpcTemplate(
            name = "Sneaky Pete - burglar",
            type = CustomerType.BURGLAR,
            dialogues = listOf(
                "Hand over the cash register and nobody gets hurt!",
                "Nice harvest you got here. Shame if someone took it.",
                "Empty your pockets!"
            ),
            avatarId = "burglar"
        )
    )

    /**
     * Checks inventory and probabilistically generates a visitor at the door.
     */
    fun tryGenerateVisitor(state: GameState, random: Random = Random()): NpcCustomer? {
        val availableStrains = state.inventory.harvestedStock.filter { it.value > 0 }
        if (availableStrains.isEmpty()) {
            return null // No customer if player has no inventory
        }

        // Random roll for customer type (90% regular customers, 5% cop, 5% burglar)
        val roll = random.nextFloat()
        val template = when {
            roll < 0.05f && state.player.level >= 3 -> CUSTOMER_TEMPLATES.first { it.type == CustomerType.UNDERCOVER_COP }
            roll < 0.10f && state.player.level >= 2 -> CUSTOMER_TEMPLATES.first { it.type == CustomerType.BURGLAR }
            else -> CUSTOMER_TEMPLATES.filter { it.type != CustomerType.UNDERCOVER_COP && it.type != CustomerType.BURGLAR }.random(random)
        }

        // Select a strain that the player actually has in stock
        val chosenStrainId = availableStrains.keys.toList().random(random)
        val availableGrams = availableStrains[chosenStrainId] ?: 1
        val strain = PlantStrain.findById(chosenStrainId)

        // Request between 1 and min(25, availableGrams)
        val requestedGrams = (1..max(1, minOf(25, availableGrams))).random(random)

        // Base price calculation with slight random fluctuation (+/- 10%)
        val priceMultiplier = 0.9 + (random.nextDouble() * 0.2)
        val baseOfferPrice = (strain.baseSellPricePerGram * priceMultiplier).let {
            Math.round(it * 10.0) / 10.0
        }

        return NpcCustomer(
            id = UUID.randomUUID().toString(),
            name = template.name,
            type = template.type,
            requestedStrainId = chosenStrainId,
            requestedGrams = requestedGrams,
            baseOfferedPricePerGram = baseOfferPrice,
            currentOfferPricePerGram = baseOfferPrice,
            patienceSeconds = 45,
            dialogueText = template.dialogues.random(random),
            avatarId = template.avatarId
        )
    }

    /**
     * Calculates the probability (0.0f - 1.0f) that the customer accepts the proposed price.
     */
    fun calculateAcceptanceProbability(
        customer: NpcCustomer,
        proposedPrice: Double,
        playerRespect: Int
    ): Float {
        val basePrice = customer.baseOfferedPricePerGram
        if (proposedPrice <= basePrice) {
            return 1.0f // Always accepts at or below base offer
        }

        val markupFraction = (proposedPrice - basePrice) / basePrice
        // Respect bonus: each 10 respect points adds 1% acceptance headroom
        val respectBonus = playerRespect * 0.001f

        // Probability formula: steep decay above 25% markup
        val baseChance = 1.0f - (markupFraction.toFloat() * 1.3f)
        return (baseChance + respectBonus).coerceIn(0.02f, 0.98f)
    }

    /**
     * Computes the required bribe cost for a police raid.
     */
    fun calculateCopBribe(player: PlayerProfile): Double {
        // Floor of $100 or 20% of current cash
        return max(100.0, player.cash * 0.20)
    }
}
