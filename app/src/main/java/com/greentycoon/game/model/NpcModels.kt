package com.greentycoon.game.model

enum class CustomerType {
    REGULAR_BUYER,
    COLLEGE_STUDENT,
    BUSINESS_VIP,
    UNDERCOVER_COP,
    BURGLAR
}

data class NpcCustomer(
    val id: String,
    val name: String,
    val type: CustomerType = CustomerType.REGULAR_BUYER,
    val requestedStrainId: String,
    val requestedGrams: Int,
    val baseOfferedPricePerGram: Double,
    val currentOfferPricePerGram: Double,
    val patienceSeconds: Int = 30,
    val arrivalTimestamp: Long = System.currentTimeMillis(),
    val dialogueText: String,
    val avatarId: String = "customer_1"
)

sealed class DealOutcome {
    data class Sold(val cashEarned: Double, val gramsSold: Int, val xpEarned: Long) : DealOutcome()
    data class Refused(val message: String) : DealOutcome()
    data class CustomerLeft(val reason: String) : DealOutcome()
    data class CopRaidBribed(val bribePaid: Double) : DealOutcome()
    data class CopRaidConfiscated(val gramsLost: Int) : DealOutcome()
    data class Robbed(val cashStolen: Double) : DealOutcome()
    data class RobberDefended(val rewardXp: Long) : DealOutcome()
}
