package sk.havrila.cvicime.trening

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Krátke počúvanie hlasových povelov („desať", „preskoč", „ďalej") cez systémový
 * rozpoznávač reči - bez knižníc a bez nahrávania do súboru. Zvuk sa nikde neukladá;
 * appka dostane len rozpoznaný text. Rozpoznávanie robí systém (zvyčajne Google),
 * preto je povel voliteľný a zapína sa v Nastaveniach.
 *
 * Počúva sa vždy len krátke okno (pár sekúnd) po otázke appky - nie stále.
 */
class Ucho(context: Context) {

    private val app = context.applicationContext
    private val hlavne = Handler(Looper.getMainLooper())
    private var rozpoznavac: SpeechRecognizer? = null

    val dostupne: Boolean get() = SpeechRecognizer.isRecognitionAvailable(app)

    /**
     * Počúva najviac [ms] milisekúnd a vráti rozpoznané varianty textu (najpravdepodobnejší prvý),
     * alebo prázdny zoznam, keď nič nepočul alebo rozpoznávač nie je.
     */
    suspend fun pocuvaj(ms: Long): List<String> = withContext(Dispatchers.Main) {
        if (!dostupne) return@withContext emptyList()
        suspendCancellableCoroutine { pokracovanie ->
            val r = rozpoznavac ?: SpeechRecognizer.createSpeechRecognizer(app).also { rozpoznavac = it }
            var hotovo = false
            fun skonci(vysledok: List<String>) {
                if (hotovo) return
                hotovo = true
                hlavne.removeCallbacksAndMessages(null)
                if (pokracovanie.isActive) pokracovanie.resume(vysledok)
            }
            r.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) =
                    skonci(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
                override fun onError(error: Int) = skonci(emptyList())
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val zamer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sk-SK")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            runCatching { r.startListening(zamer) }.onFailure { skonci(emptyList()) }
            // Okno sa zavrie samo: čo povedal dovtedy, sa ešte vyhodnotí.
            hlavne.postDelayed({ runCatching { r.stopListening() } }, ms)
            hlavne.postDelayed({ runCatching { r.cancel() }; skonci(emptyList()) }, ms + 2500)
            pokracovanie.invokeOnCancellation {
                hlavne.post { runCatching { r.cancel() } }
            }
        }
    }

    fun zavri() {
        hlavne.removeCallbacksAndMessages(null)
        runCatching { rozpoznavac?.destroy() }
        rozpoznavac = null
    }
}
