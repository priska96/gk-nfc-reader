package expo.modules.ehcnfc.views

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import expo.modules.ehcnfc.ExpoEhcNfcModule
import expo.modules.ehcnfc.ExpoEhcNfcModule.Companion
import expo.modules.ehcnfc.ExpoEhcNfcModule.Companion.results
import expo.modules.ehcnfc.ExpoEhcNfcModule.Companion.state
import expo.modules.ehcnfc.R
import expo.modules.ehcnfc.nfcreader.SessionMessages
import expo.modules.ehcnfc.readingresult.PersonalData
import expo.modules.ehcnfc.readingresult.ReaderState
import expo.modules.ehcnfc.readingresult.ReadingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NfcBottomSheetActivity: AppCompatActivity() , NfcAdapter.ReaderCallback{

    companion object {
        lateinit var appContextRef: Context
        lateinit var nfcAdapter: NfcAdapter
        lateinit var messages: SessionMessages
        var currentTag: Tag? = null
    }
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root container with bottom sheet "look"
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.WHITE)

        }

        // Status text
        statusTextView = TextView(this).apply {
            text = messages.discoveryMessage
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        rootLayout.addView(statusTextView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 32
        })

        // Close button
        val closeButton = Button(this).apply {
            text = messages.cancelMessage
            setOnClickListener {
                currentTag = null
                ExpoEhcNfcModule.resetModuleState()
                finish()
            }
        }
        rootLayout.addView(closeButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Set layout as content view
        setContentView(rootLayout)

        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.BOTTOM)
    }

    // called every time the activity comes to the foreground
    override fun onResume() {
        super.onResume()
        // Start tasks that need to run when activity is visible
        nfcAdapter.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null
        )
    }
    private fun updateBottomSheetStatusText(message: String) {
        runOnUiThread {
            statusTextView.text = message
        }
    }
    private fun playSuccessTick() {
        val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) // 100 = volume %
        toneG.startTone(ToneGenerator.TONE_PROP_ACK, 200) // 150ms beep
        // Automatically stops after duration
    }
    override fun onTagDiscovered(tag: Tag) {

        Log.d("BottomSheetActivity", "Tag discovered: ${tag.id.joinToString { "%02x".format(it) }}")
        if(currentTag != null){
            Log.e("NfcBottomSheetActivity", "error ${messages.multipleCardsMessage}")
            updateBottomSheetStatusText(messages.multipleCardsMessage)
            val failedResult = Result.failure<PersonalData>(Exception(messages.multipleCardsMessage))
            ExpoEhcNfcModule.results.add(ReadingResult(result = failedResult, commands = emptyList()))

            ExpoEhcNfcModule.updateAndSendStateChange(ReaderState.FAILURE, null, Exception(messages.multipleCardsMessage))
            ExpoEhcNfcModule.pendingPromise?.reject("WRONG_CARD",failedResult.exceptionOrNull()?.message, null)

            return
        }

        currentTag = tag
        updateBottomSheetStatusText(messages.connectMessage)

        val reader = ExpoEhcNfcModule.readerInstance ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val personalData = reader.readIsoDepTag(tag) { status ->
                    updateBottomSheetStatusText(status) // updates UI directly
                }
                val succeededResult = Result.success(personalData)

                ExpoEhcNfcModule.results.add(ReadingResult(result = succeededResult, commands = emptyList()))
                ExpoEhcNfcModule.updateAndSendStateChange(ReaderState.SUCCESS, personalData, null)
                ExpoEhcNfcModule.pendingPromise?.resolve(succeededResult)
                playSuccessTick()
                finish() // close the bottomSheet only on success
            } catch (e: Exception) {
                Log.e("NfcBottomSheetActivity", "error ${e.message}")
                updateBottomSheetStatusText("${ExpoEhcNfcModule.appContextRef?.getString(R.string.nfc_txt_error)}: ${e.message}")
                val failedResult = Result.failure<PersonalData>(Exception("Error: ${e.message}"))

                ExpoEhcNfcModule.results.add(ReadingResult(result = failedResult, commands = emptyList()))
                ExpoEhcNfcModule.updateAndSendStateChange(ReaderState.FAILURE, null, Exception(e.message))
                ExpoEhcNfcModule.pendingPromise?.reject("WRONG_CARD",failedResult.exceptionOrNull()?.message, null)
            }finally {
                currentTag = null // free up for next tag
                Log.d("NfcBottomSheetActivity", "state $state")

                if (results.isNotEmpty()) {
                    Log.d("NfcBottomSheetActivity", "results ${results[0].formattedDescription()}")
                } else {
                    Log.d("NfcBottomSheetActivity", "results list is empty")
                }
                ExpoEhcNfcModule.resetModuleState()
            }
        }
    }
}