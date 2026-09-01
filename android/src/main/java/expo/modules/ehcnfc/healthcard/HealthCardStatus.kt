package expo.modules.ehcnfc.healthcard

sealed class HealthCardStatus {

    // when card type has not been determined (yet) [e.g. probing]
    data object Unknown : HealthCardStatus()

    // when card type has been identified by this library
    data class Valid(val cardType: HealthCardPropertyType?) : HealthCardStatus()

    // when card type could not be determined
    data object Invalid : HealthCardStatus()

    // Whether the presented Card is valid in the gematik domain
    val isValid: Boolean
        get() = this is Valid

    // The generation version of the card/COS
    val generation: CardGeneration?
        get() = (this as? Valid)?.cardType?.generation

    // The kind of gematik Healthcard (eGK, HBA, SMC-B)
    val type: HealthCardPropertyType?
        get() = (this as? Valid)?.cardType
}