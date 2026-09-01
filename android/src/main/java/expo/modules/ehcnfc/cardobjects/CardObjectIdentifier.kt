package expo.modules.ehcnfc.cardobjects

interface CardObjectIdentifier {
    /** Raw, immutable  byte representation of this identifier (Swift: `var rawValue: Data`). */
    val rawValue: ByteArray

    /**
     * Swift-compatible API name: `static func isValid(_ value: Data) -> Result<Data, Error>`
     *
     * Kotlin interfaces cannot have static methods, so we declare an *instance* method
     * that every concrete type will forward to its own `companion object` helper of
     * the same name.  The semantic contract is identical.
     */
    fun isValid(value: ByteArray): Result<ByteArray>
}