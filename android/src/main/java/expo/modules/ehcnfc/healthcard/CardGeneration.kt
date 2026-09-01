package expo.modules.ehcnfc.healthcard

/** Represent the card generation of health card
 *
 * | Version   | Version with 2 digits | INT Value for Version | Card generation
 * | < 3.0.3   |  03.00.03             | 30003                 | G1
 * | < 4.0.0   |  04.00.00             | 40000                 | G1P
 * | >= 4.0.0  |  04.00.00             | 40000                 | G2
 * | >= 4.4.0  |  04.04.00             | 40400                 | G2_1
*/
enum class CardGeneration {
    // Generation G1 (< 3.0.3)
    G1,
    // Generation G1P (3.0.3 - < 4.0.0)
    G1P,
    // Generation G2 (4.0.0 - < 4.4.0)
    G2,
    // Generation G2.1 (4.4.0+)
    G2_1;

    companion object {
        private const val VERSION_3_0_3 = 30003
        private const val VERSION_4_0_0 = 40000
        private const val VERSION_4_4_0 = 40400

        // Length in bytes [objectSystemVersion]
        const val CARD_VERSION_LENGTH = 3


        /** Return the CardGeneration for ObjectSystemVersion
         *
         * - Parameter version: value like 30003, 40000 (for details see class description)
         * - Returns: Generation Value or nil when version is unrecognized
        */
        fun parseCardGeneration(version: Int): CardGeneration? = when (version) {
            in 0 until VERSION_3_0_3 -> G1
            in VERSION_3_0_3 until VERSION_4_0_0 -> G1P
            in VERSION_4_0_0 until VERSION_4_4_0 -> G2
            in VERSION_4_4_0..Int.MAX_VALUE -> G2_1
            else -> null
        }

        /** Parse the CardGeneration from the `objectSystemVersion` from the `CardVersion2`*/
        fun parseCardGeneration(data: ByteArray): CardGeneration? {
            if (data.size != CARD_VERSION_LENGTH) return null
            // Convert byte array to integer assuming each byte represents a decimal version
            val hexString = data.joinToString("") { "%02x".format(it) }

            // Radix 10 is not entirely correct here, but we assume version nrs not to grow beyond 99
            val versionInt = hexString.toIntOrNull(radix = 10) ?: return null
            return parseCardGeneration(versionInt)
        }
    }

}