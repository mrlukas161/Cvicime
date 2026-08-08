package sk.havrila.cvicime.trening

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Hlas a vibrácie prehrávača.
 *
 * Slovenský hlas nemusí na zariadení existovať (T1 z návrhu) - preto sa dostupnosť
 * zisťuje pri inicializácii a keď jazyk nie je, hovorenie sa ticho preskočí
 * a ostanú pípnutia + vibrácie. Appka na hlase nesmie stáť.
 */
class Hlas(context: Context) {

    private val app = context.applicationContext
    private var tts: TextToSpeech? = null
    private var jazykOk = false
    private var ton: ToneGenerator? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        tts = TextToSpeech(app) { stav ->
            if (stav == TextToSpeech.SUCCESS) {
                val vysledok = tts?.setLanguage(Locale("sk", "SK"))
                jazykOk = vysledok != TextToSpeech.LANG_MISSING_DATA &&
                    vysledok != TextToSpeech.LANG_NOT_SUPPORTED
                if (jazykOk) {
                    tts?.setSpeechRate(1.05f)
                } else {
                    Log.i("Hlas", "Slovenský TTS nie je dostupný — ostávajú pípnutia a vibrácie")
                }
            }
        }
        ton = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }.getOrNull()
    }

    /** Povie text, ak je slovenský hlas dostupný a hlas je zapnutý. Inak nič. */
    fun povedz(text: String, zapnuty: Boolean) {
        if (!zapnuty || !jazykOk) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cvicime")
    }

    /** Krátke pípnutie odpočtu (posledné 3 sekundy). */
    fun pipni(zapnuty: Boolean) {
        if (!zapnuty) return
        ton?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    /** Dlhší tón na konci série/pauzy. */
    fun pipniKoniec(zapnuty: Boolean) {
        if (!zapnuty) return
        ton?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
    }

    fun zavibruj(zapnute: Boolean, dlho: Boolean = false) {
        if (!zapnute) return
        val v = vibrator ?: return
        val ms = if (dlho) 400L else 150L
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun zavri() {
        tts?.shutdown()
        tts = null
        ton?.release()
        ton = null
    }
}
