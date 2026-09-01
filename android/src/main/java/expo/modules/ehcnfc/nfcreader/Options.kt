package expo.modules.ehcnfc.nfcreader

import com.facebook.react.bridge.ReadableMap

data class Options(
    val can: String,
    val pin: String?,
    val checkBrainpoolAlgorithm: Boolean?
)

fun ReadableMap.toOptions(): Options {
    return Options(
        can = getString("can") ?: error("Missing required 'can' field"),
        pin = if (hasKey("pin")) getString("pin") else null,
        checkBrainpoolAlgorithm = if (hasKey("checkBrainpoolAlgorithm")) getBoolean("checkBrainpoolAlgorithm") else null
    )
}