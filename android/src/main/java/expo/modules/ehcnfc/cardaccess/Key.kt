package expo.modules.ehcnfc.cardaccess

class Key(private val keyId: UByte) {

    companion object {
        private const val minKeyId: UByte = 2u
        private const val maxKeyId: UByte = 28u
        private const val dfSpecificPwdMarker: UByte = 0x80u  // Example marker, set according to your Swift code
    }

    init {
        require(keyId in minKeyId..maxKeyId) {
            "Password ID: [$keyId] out of range [$minKeyId, $maxKeyId]"
        }
    }

    private fun calculateKeyReference(dfSpecific: Boolean): UByte {
        return if (dfSpecific) {
            (keyId + dfSpecificPwdMarker).toUByte()
        } else {
            keyId
        }
    }

    // To get the byte array directly
    fun keyReferenceBytes(dfSpecific: Boolean): ByteArray {
        return byteArrayOf(calculateKeyReference(dfSpecific).toByte())
    }
}
