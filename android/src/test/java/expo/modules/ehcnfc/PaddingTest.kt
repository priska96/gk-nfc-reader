package expo.modules.ehcnfc

import expo.modules.ehcnfc.helpers.Bytes.padData
import org.junit.Assert.*
import org.junit.Test

class PaddingTest {

    @Test
    fun testPadding_exactBlock() {
        val input = ByteArray(16) { 0x01 }
        val padded = padData(input,16)

        // should add a full extra block (16 bytes)
        assertEquals(32, padded.size)

        // first padding byte after data should be 0x80
        assertEquals(0x80.toByte(), padded[16])
    }
}
