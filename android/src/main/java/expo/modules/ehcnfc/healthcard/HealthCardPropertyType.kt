package expo.modules.ehcnfc.healthcard

import expo.modules.ehcnfc.cardobjects.CardAid

sealed class HealthCardPropertyType(open val generation: CardGeneration) {
    data class Egk(override val generation: CardGeneration) : HealthCardPropertyType(generation)
    data class Hba(override val generation: CardGeneration) : HealthCardPropertyType(generation)
    data class Smcb(override val generation: CardGeneration) : HealthCardPropertyType(generation)

    override fun toString(): String = when (this) {
        is Egk -> "eGK $generation"
        is Hba -> "HBA $generation"
        is Smcb -> "SMC-B $generation"
    }

    companion object{

        @Throws(IllegalArgumentException::class)
        fun fromCardAid(cardAid: CardAid, cardVersion2: CardVersion2?): HealthCardPropertyType {
            if (cardVersion2 == null) {
                throw IllegalArgumentException("Illegal generation: $cardVersion2")
            }
            val generation = cardVersion2.generation()
                ?: throw IllegalArgumentException("Illegal generation: $cardVersion2")

            return when (cardAid) {
                CardAid.EGK -> Egk(generation)
                CardAid.HBA -> Hba(generation)
                CardAid.SMCB -> Smcb(generation)
            }
        }
    }
}