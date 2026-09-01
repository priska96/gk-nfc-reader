package expo.modules.ehcnfc

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import expo.modules.ehcnfc.nfcreader.NfcReader
import expo.modules.ehcnfc.nfcreader.toOptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import expo.modules.ehcnfc.nfcreader.SessionMessages
import expo.modules.ehcnfc.readingresult.PersonalData
import expo.modules.ehcnfc.readingresult.ReaderState
import expo.modules.ehcnfc.readingresult.ReadingResult
import expo.modules.ehcnfc.views.NfcBottomSheetActivity
import java.util.Collections


class ExpoEhcNfcModule : Module() {

    companion object {

        var readerInstance: NfcReader? = null
        var state: ReaderState = ReaderState.IDLE
        var results: MutableList<ReadingResult> = Collections.synchronizedList(mutableListOf()) // that makes read/write thread-safe
        var pendingPromise: Promise? = null
        var emitter: ExpoEhcNfcModule? = null
        var appContextRef: Context? = null


        /** Resets all module-level NFC state */
        fun resetModuleState() {
            readerInstance = null
            state = ReaderState.IDLE
            pendingPromise = null
            results.clear()
            Log.d("ExpoEhcNfcModule", "Module state reset")
        }

        /** Sends the nfcReaderState to JS */
        fun updateAndSendStateChange(newState: ReaderState, value: PersonalData?, error: Exception?) {
            state = newState
            val event: MutableMap<String, Any?> = mutableMapOf(
                "state" to when (state) {
                    ReaderState.IDLE -> "idle"
                    ReaderState.LOADING -> "loading"
                    ReaderState.SUCCESS -> "success"
                    ReaderState.FAILURE -> "failure"
                },
                "value" to null,
                "error" to null
            )
            when (state) {
                ReaderState.SUCCESS -> {
                    event["value"] = value?.toJson()
                }
                ReaderState.FAILURE -> {
                    event["error"] =  appContextRef?.getString(R.string.nfc_txt_unknown_error)
                    if(error != null){
                        event["error"] = appContextRef?.getString(R.string.nfc_txt_error) + ": "+ error.message
                    }

                }
                else -> { /* idle and loading already handled */ }
            }
            emitter?.sendEvent("onStatusChange", event)
        }
    }

    override fun definition() = ModuleDefinition {
        // Sets the name of the module that JavaScript code will use to refer to the module.
        // The module will be accessible from `requireNativeModule('ExpoEhcNfc')` in JavaScript.
        Name("ExpoEhcNfc")

        Events("onStatusChange")

        OnCreate {
            emitter = this@ExpoEhcNfcModule
            appContextRef = appContext.reactContext ?: throw IllegalStateException("No context")
        }
        // AsyncFunction to be exposed to react-native
        AsyncFunction("readPersonalData") { options: ReadableMap, promise: Promise ->
            updateAndSendStateChange(ReaderState.IDLE, null, null)

            if (appContextRef == null) {
                updateAndSendStateChange(ReaderState.FAILURE, null, null)
                resetModuleState()
                return@AsyncFunction promise.reject(
                    "NO_CONTEXT",
                    "Current context is null",
                    null
                )
            }

            val messages = SessionMessages(
                discoveryMessage = appContextRef?.getString(R.string.nfc_txt_discoveryMessage) ?: "",
                connectMessage = appContextRef?.getString(R.string.nfc_txt_connectMessage) ?: "",
                secureChannelMessage = appContextRef?.getString(R.string.nfc_txt_secureChannelMessage) ?: "",
                noCardMessage = appContextRef?.getString(R.string.nfc_txt_noCardMessage) ?: "",
                multipleCardsMessage = appContextRef?.getString(R.string.nfc_txt_multipleCardsMessage) ?: "",
                unsupportedCardMessage = appContextRef?.getString(R.string.nfc_txt_unsupportedCardMessage) ?: "",
                connectionErrorMessage = appContextRef?.getString(R.string.nfc_txt_connectionErrorMessage) ?: "",
                wrongCanMessage = appContextRef?.getString(R.string.nfc_txt_wrongCanMessage) ?: "",
                unexpectedAnswerFromCardMessage = appContextRef?.getString(R.string.nfc_txt_unexpectedAnswerFromCardMessage) ?: "",
                readingErrorMessage = appContextRef?.getString(R.string.nfc_txt_readingErrorMessage) ?: "",
                cancelMessage = appContextRef?.getString(R.string.nfc_txt_cancelMessage) ?: "",
                readingMessage = appContextRef?.getString(R.string.nfc_txt_readingMessage) ?: "",
                successMessage = appContextRef?.getString(R.string.nfc_txt_successMessage) ?: ""
            )

            val parsedOptions = options.toOptions()
            if(parsedOptions.can.length != 6){
                updateAndSendStateChange(ReaderState.FAILURE, null, Exception(appContextRef?.getString(R.string.nfc_can_error)))
                resetModuleState()
                return@AsyncFunction promise.reject(
                    "INVALID_CAN",
                    appContextRef?.getString(R.string.nfc_can_error),
                    null
                )
            }
            if (readerInstance == null) {
                readerInstance = NfcReader(parsedOptions, messages)
            }
            Log.d("ExpoEhcNfcModule", "Starting readPersonalData with options: $options")

            val activity = appContext.currentActivity
            if (activity == null) {
                updateAndSendStateChange(ReaderState.FAILURE, null, null)
                resetModuleState()
                return@AsyncFunction promise.reject(
                    "NO_ACTIVITY",
                    appContextRef?.getString(R.string.no_activity_error),
                    null
                )
            }
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

            if (nfcAdapter == null) {
                updateAndSendStateChange(ReaderState.FAILURE, null, Exception(appContextRef?.getString(R.string.no_activity_error)))
                resetModuleState()
                return@AsyncFunction promise.reject(
                    "NO_NFC_ADAPTER",
                    appContextRef?.getString(R.string.no_activity_error),
                    null
                )
            }
            NfcBottomSheetActivity.appContextRef = appContextRef as Context // pass the Context here
            NfcBottomSheetActivity.nfcAdapter = nfcAdapter // pass the NfcAdapter here
            NfcBottomSheetActivity.messages = messages // pass the Messages here
            val intent = Intent(activity, NfcBottomSheetActivity::class.java)
            updateAndSendStateChange(ReaderState.LOADING, null, null)
            activity.startActivity(intent)
        }
    }
}
