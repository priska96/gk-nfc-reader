package expo.modules.ehcnfc.cardobjects

interface CardItemType

/**
 * EF: Elementary File
*/
data class ElementaryFile(
val fid: FileIdentifier,
val sfid: ShortFileIdentifier? = null
) : CardItemType

/**
 * DF: Dedicated File
*/
data class DedicatedFile(
val aid: ApplicationIdentifier,
val fid: FileIdentifier? = null
) : CardItemType

/**
 * Card File system layout for EGK smart-cards
 */
object EgkFileSystem {

    /**
     * Elementary File identifier
     */
    object EF {
        val atr = ElementaryFile(
            fid = FileIdentifier.fromHex("2F01"),
            sfid = ShortFileIdentifier.fromHex("1D")
        )
        val cardAccess = ElementaryFile(
            fid = FileIdentifier.fromHex("011C"),
            sfid = ShortFileIdentifier.fromHex("1C")
        )
        val cCaEgkCsE256 = ElementaryFile(
            fid = FileIdentifier.fromHex("2F07"),
            sfid = ShortFileIdentifier.fromHex("07")
        )
        val cEgkAutCVCE256 = ElementaryFile(
            fid = FileIdentifier.fromHex("2F06"),
            sfid = ShortFileIdentifier.fromHex("06")
        )
        val dir = ElementaryFile(
            fid = FileIdentifier.fromHex("2F00"),
            sfid = ShortFileIdentifier.fromHex("1E")
        )
        val gdo = ElementaryFile(
            fid = FileIdentifier.fromHex("2F02"),
            sfid = ShortFileIdentifier.fromHex("02")
        )
        val version = ElementaryFile(
            fid = FileIdentifier.fromHex("2F10"),
            sfid = ShortFileIdentifier.fromHex("10")
        )
        val version2 = ElementaryFile(
            fid = FileIdentifier.fromHex("2F11"),
            sfid = ShortFileIdentifier.fromHex("11")
        )
        val nfd = ElementaryFile(
            fid = FileIdentifier.fromHex("D010"),
            sfid = ShortFileIdentifier.fromHex("10")
        )
        val hcaEinwilligung = ElementaryFile(
            fid = FileIdentifier.fromHex("D005"),
            sfid = ShortFileIdentifier.fromHex("05")
        )
        val hcaGVD = ElementaryFile(
            fid = FileIdentifier.fromHex("D003"),
            sfid = ShortFileIdentifier.fromHex("03")
        )
        val hcaLogging = ElementaryFile(
            fid = FileIdentifier.fromHex("D006"),
            sfid = ShortFileIdentifier.fromHex("06")
        )
        val hcaPD = ElementaryFile(
            fid = FileIdentifier.fromHex("D001"),
            sfid = ShortFileIdentifier.fromHex("01")
        )
        val hcaPruefungsnachweis = ElementaryFile(
            fid = FileIdentifier.fromHex("D01C"),
            sfid = ShortFileIdentifier.fromHex("1C")
        )
        val hcaStandalone = ElementaryFile(
            fid = FileIdentifier.fromHex("DA0A"),
            sfid = ShortFileIdentifier.fromHex("0A")
        )
        val hcaStatusVD = ElementaryFile(
            fid = FileIdentifier.fromHex("D00C"),
            sfid = ShortFileIdentifier.fromHex("0C")
        )
        val hcaTTN = ElementaryFile(
            fid = FileIdentifier.fromHex("D00F"),
            sfid = ShortFileIdentifier.fromHex("0F")
        )
        val hcaVD = ElementaryFile(
            fid = FileIdentifier.fromHex("D002"),
            sfid = ShortFileIdentifier.fromHex("02")
        )
        val hcaVerweis = ElementaryFile(
            fid = FileIdentifier.fromHex("D009"),
            sfid = ShortFileIdentifier.fromHex("09")
        )
        val esignCChAutR2048 = ElementaryFile(
            fid = FileIdentifier.fromHex("C500"),
            sfid = ShortFileIdentifier.fromHex("01")
        )
        val esignCChAutE256 = ElementaryFile(
            fid = FileIdentifier.fromHex("C504"),
            sfid = ShortFileIdentifier.fromHex("04")
        )
        val esignCChAutnR2048 = ElementaryFile(
            fid = FileIdentifier.fromHex("C509"),
            sfid = ShortFileIdentifier.fromHex("09")
        )
        val esignCChEncR2048 = ElementaryFile(
            fid = FileIdentifier.fromHex("C200"),
            sfid = ShortFileIdentifier.fromHex("02")
        )
        val esignCChEncvR2048 = ElementaryFile(
            fid = FileIdentifier.fromHex("C50A"),
            sfid = ShortFileIdentifier.fromHex("0A")
        )
    }

    /**
     * Dedicated File Identifier
     */
    object DF {
        val MF = DedicatedFile(aid = ApplicationIdentifier.fromHex("D2760001448000"), fid = FileIdentifier.fromHex("3F00"))
        val HCA = DedicatedFile(aid = ApplicationIdentifier.fromHex("D27600000102"))
        val ESIGN = DedicatedFile(aid = ApplicationIdentifier.fromHex("A000000167455349474E"))
        val QES = DedicatedFile(aid = ApplicationIdentifier.fromHex("D27600006601"))
        val NFD = DedicatedFile(aid = ApplicationIdentifier.fromHex("D27600014407"))
        val DPE = DedicatedFile(aid = ApplicationIdentifier.fromHex("D27600014408"))
        val GDD = DedicatedFile(aid = ApplicationIdentifier.fromHex("D2760001440A"))
        val OSE = DedicatedFile(aid = ApplicationIdentifier.fromHex("D2760001440B"))
        val AMTS = DedicatedFile(aid = ApplicationIdentifier.fromHex("D2760001440C"))
    }

    /**
     * Pin identifiers
     */
    enum class Pin(val rawValue: String) {
        PinCH("01"),
        MrpinHome("02"),
        MrpinNFD("03"),
        MrpinNFDREAD("07"),
        MrpinDPE("04"),
        MrpinDPEREAD("08"),
        MprinGDD("05"),
        MrpinOSE("09"),
        MrpinAMTS("0C"),
        PinAMTSREP("0D"),
        PinQES("01")
    }
}
