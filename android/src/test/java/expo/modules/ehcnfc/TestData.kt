package expo.modules.ehcnfc

data class TestData(
    val paceKey: PaceKey,
    val apdus: Map<String, Apdu>,
    val macs: Map<String, String>,
    val personalData: PersonalData,
    val fcp: FCP
) {
    data class PaceKey(val enc: String, val mac: String)
    data class Apdu(
        val expectedCmd: String,
        val expectedEncryptedCmd: String,
        val expectedRes: String,
        val expectedEncryptedRes: String
    )
    data class PersonalData(
        val compressed: String,
        val decompressed: String,
    )
    data class FCP(
        val numberOfOctets: Int,
        val fileDescriptor: String,
        val fileIdentifier: String,
        val applicationIdentifier: String?,
        val shortFileIdentifier: String?,
        val lifeCycleStatus: String,
        val readSize: Int
    )
}
