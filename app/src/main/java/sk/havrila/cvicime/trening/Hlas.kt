package sk.havrila.cvicime.trening

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Hlas a vibrácie prehrávača.
 *
 * Slovenský hlas nemusí na zariadení existovať (T1 z návrhu) - preto sa dostupnosť
 * zisťuje pri inicializácii a keď jazyk nie je, hovorenie sa ticho preskočí
 * a ostanú pípnutia + vibrácie. Appka na hlase nesmie stáť.
 *
 * Hudba (Spotify, YouTube Music…) hrá ďalej: kým appka hovorí, požiada systém o krátke
 * stíšenie ostatného zvuku (audio focus „may duck") a po dohovorení ho vráti. Žiadna
 * integrácia hudobných služieb netreba - funguje to s hocijakou appkou, ktorá focus rešpektuje.
 */
class Hlas(context: Context) {

    private val app = context.applicationContext
    private var tts: TextToSpeech? = null
    private var jazykOk = false
    private var ton: ToneGenerator? = null
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** Hlas navigácie: systém ho zmixuje cez hudbu a hudbu na ten čas stíši. */
    private val atributy = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val ziadostOFocus: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(atributy)
            .setOnAudioFocusChangeListener { }
            .build()
    } else {
        null
    }

    /** Vety, ktoré ešte nedohovoril - focus sa vráti až po poslednej (aj prerušenej). */
    private val rozpravane: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var cisloVety = 0

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
                    tts?.setAudioAttributes(atributy)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) = dohovorene(utteranceId)
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) = dohovorene(utteranceId)
                        override fun onError(utteranceId: String?, errorCode: Int) = dohovorene(utteranceId)
                        override fun onStop(utteranceId: String?, interrupted: Boolean) = dohovorene(utteranceId)
                    })
                } else {
                    Log.i("Hlas", "Slovenský TTS nie je dostupný — ostávajú pípnutia a vibrácie")
                }
            }
        }
        ton = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }.getOrNull()
    }

    /** Povie text (preruší, čo práve hovorí), ak je slovenský hlas dostupný a hlas je zapnutý. */
    fun povedz(text: String, zapnuty: Boolean) = hovor(text, zapnuty, TextToSpeech.QUEUE_FLUSH)

    /** Povie text po tom, čo práve hovorí - napr. detská veta za ohlásením tvojho cviku. */
    fun povedzPotom(text: String, zapnuty: Boolean) = hovor(text, zapnuty, TextToSpeech.QUEUE_ADD)

    private fun hovor(text: String, zapnuty: Boolean, rezim: Int) {
        if (!zapnuty || !jazykOk || text.isBlank()) return
        val t = tts ?: return
        val id = "cvicime-${cisloVety++}"
        if (rozpravane.isEmpty()) poziadajOFocus()
        rozpravane += id
        val vysledok = t.speak(text, rezim, Bundle(), id)
        if (vysledok != TextToSpeech.SUCCESS) dohovorene(id)
    }

    private fun dohovorene(id: String?) {
        if (id != null) rozpravane -= id
        if (rozpravane.isEmpty()) vratFocus()
    }

    private fun poziadajOFocus() {
        val a = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ziadostOFocus?.let { a.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            a.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun vratFocus() {
        val a = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ziadostOFocus?.let { a.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            a.abandonAudioFocus(null)
        }
    }

    /** Utne práve hovorenú vetu (prepnutie na pípanie/ticho). */
    fun stichni() {
        tts?.stop()
        rozpravane.clear()
        vratFocus()
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
        tts?.stop()
        tts?.shutdown()
        tts = null
        rozpravane.clear()
        vratFocus()
        ton?.release()
        ton = null
    }
}

/** Čo prehrávač robí so zvukom - prepína sa tlačidlom reproduktora v hlavičke. */
enum class RezimZvuku(val kluc: String, val popis: String) {
    HLAS("hlas", "Hlas a pípanie"),
    PIPANIE("pipanie", "Len pípanie"),
    TICHO("ticho", "Ticho");

    val hlas get() = this == HLAS
    val pipanie get() = this != TICHO

    fun dalsi(): RezimZvuku = entries[(ordinal + 1) % entries.size]

    companion object {
        fun z(kluc: String?): RezimZvuku? = entries.firstOrNull { it.kluc == kluc }
    }
}
