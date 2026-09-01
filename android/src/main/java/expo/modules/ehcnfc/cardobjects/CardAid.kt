package expo.modules.ehcnfc.cardobjects

/**
 * Small helper to create an ApplicationIdentifier from hex. If the hex string is
 * invalid the whole application should not start, so we convert the
 * checked Error into an unchecked IllegalStateException.
 */
private fun createAid(hex: String): ApplicationIdentifier =
    try {
        ApplicationIdentifier.fromHex(hex)
    } catch (e: ApplicationIdentifier.Error) {
        throw IllegalStateException("Invalid AID constant in CardAid enum", e)
    }


enum class CardAid(val aid: ApplicationIdentifier) {
    EGK (createAid("D2760001448000")),
    HBA (createAid("D27600014601")),
    SMCB(createAid("D27600014606"));
    /**
     * 5.3.4 MF / EF.Version2 (SMC-B)
     * 5.3.8 MF / EF.Version2 (eGK)
     * 5.3.5 MF / EF.Version2 (HBA)
     *
     * All three card types use the same Short-File-Identifier.
     */
    val efVersion2Sfi: ShortFileIdentifier
        get() = EgkFileSystem.EF.version2.sfid
            ?: error("EF.Version2.sfid must not be null according to spec")

    /**
     * 5.3.2 MF / EF.CardAccess (eGK)
     * 5.3.2 MF / EF.CardAccess (HBA)
     *
     * Note: for both the shortFileIdentifier is the same
     */
    val efCardAccess: ShortFileIdentifier
        get() = EgkFileSystem.EF.cardAccess.sfid
            ?: error("EF.cardAccess.sfid must not be null according to spec")

    /**
     * Mirrors Swift’s `enumCase.rawValue` by delegating to the wrapped
     * ApplicationIdentifier.
     */
    val rawValue: ByteArray
        get() = aid.rawValue

    /* ---------------- companion helpers ---------------- */

    companion object {

        /** Look up the enum case by its bytes, return null if unknown. */
        fun fromBytesOrNull(bytes: ByteArray): CardAid? =
            entries.firstOrNull { it.rawValue.contentEquals(bytes) }

    }
}