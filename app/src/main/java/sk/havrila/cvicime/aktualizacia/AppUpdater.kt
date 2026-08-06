package sk.havrila.cvicime.aktualizacia

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sk.havrila.cvicime.BuildConfig
import sk.havrila.cvicime.data.Prefs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aktualizácia priamo z GitHub Releases.
 *
 * Verzia sa NEČÍTA z názvu tagu, ale z názvu APK súboru (`cvicime-<versionCode>.apk`).
 * Dôvod: versionCode je jediné, čo Android pri inštalácii naozaj porovnáva,
 * a názov tagu sa dá ľahko preklepnúť.
 *
 * Inštaluje sa klasickým ACTION_VIEW intentom. PackageInstaller session cesta je
 * zámerne nepoužitá - v predošlom projekte ju HyperOS obmedzoval, prvá inštalácia
 * vždy padla a nič to nešetrilo, lebo Mi kontrola pred inštaláciou sa ukáže tak či tak.
 */
object AppUpdater {

    private const val TAG = "AppUpdater"
    private const val API = "https://api.github.com/repos/mrlukas161/Cvicime/releases/latest"
    private val NAZOV_APK = Regex("""cvicime-(\d+)\.apk""")
    private const val DEN_MS = 24 * 60 * 60 * 1000L

    data class NovaVerzia(
        val nazovVerzie: String,
        val versionCode: Int,
        val urlApk: String,
        val poznamky: String,
    )

    /**
     * Vráti novú verziu, ak existuje. `nasilu = false` kontroluje najviac raz denne.
     * Chyba siete nie je dôvod na hlásenie - appka funguje aj bez aktualizácií.
     */
    suspend fun zisti(context: Context, nasilu: Boolean): NovaVerzia? = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) return@withContext null // ladiaci build má iný podpis, nedá sa cezeň inštalovať

        val prefs = Prefs(context)
        val teraz = System.currentTimeMillis()
        if (!nasilu && teraz - prefs.poslednaKontrolaAktualizacie < DEN_MS) return@withContext null
        prefs.poslednaKontrolaAktualizacie = teraz

        try {
            val spojenie = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Cvicime")
            }
            val kod = spojenie.responseCode
            if (kod != 200) {
                // 404 na súkromnom repozitári je normálny stav, nie chyba appky.
                Log.i(TAG, "GitHub vrátil $kod — aktualizácie sa preskakujú")
                return@withContext null
            }
            val telo = spojenie.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(telo)
            val prilohy = json.optJSONArray("assets") ?: return@withContext null

            for (i in 0 until prilohy.length()) {
                val p = prilohy.getJSONObject(i)
                val zhoda = NAZOV_APK.find(p.optString("name")) ?: continue
                val kodVerzie = zhoda.groupValues[1].toIntOrNull() ?: continue
                if (kodVerzie <= BuildConfig.VERSION_CODE) return@withContext null
                return@withContext NovaVerzia(
                    nazovVerzie = json.optString("tag_name").removePrefix("v").substringBefore("-build"),
                    versionCode = kodVerzie,
                    urlApk = p.optString("browser_download_url"),
                    poznamky = json.optString("body").take(400),
                )
            }
            null
        } catch (t: Throwable) {
            Log.i(TAG, "Kontrola aktualizácie zlyhala: ${t.message}")
            null
        }
    }

    /**
     * Android pustí inštaláciu z appky len s povolením „inštalovať neznáme aplikácie".
     * Kontroluje sa PRED sťahovaním - inak by sa zbytočne stiahlo 12 MB a používateľ
     * by narazil na stenu až na konci.
     */
    fun smieInstalovat(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Otvorí systémovú obrazovku, kde sa to povolenie zapína. */
    fun otvorPovolenie(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            android.net.Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Stiahne APK do cache a otvorí systémový inštalátor. Všetko mimo hlavného vlákna. */
    suspend fun stiahniANainstaluj(
        context: Context,
        verzia: NovaVerzia,
        priebeh: (Int) -> Unit = {},
    ): String? = withContext(Dispatchers.IO) {
        try {
            val priecinok = File(context.cacheDir, "aktualizacie").apply { mkdirs() }
            val subor = File(priecinok, "cvicime-${verzia.versionCode}.apk")

            val spojenie = (URL(verzia.urlApk).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Cvicime")
            }
            if (spojenie.responseCode !in 200..299) return@withContext "Sťahovanie zlyhalo (${spojenie.responseCode})."

            val celkom = spojenie.contentLength.toLong()
            var stiahnute = 0L
            spojenie.inputStream.use { vstup ->
                subor.outputStream().use { vystup ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = vstup.read(buffer)
                        if (n <= 0) break
                        vystup.write(buffer, 0, n)
                        stiahnute += n
                        if (celkom > 0) priebeh(((stiahnute * 100) / celkom).toInt())
                    }
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", subor)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) { context.startActivity(intent) }
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Inštalácia zlyhala: ${t.message}", t)
            "Nepodarilo sa: ${t.message}"
        }
    }
}
