package sk.havrila.cvicime.data.katalog

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Načíta knižnicu cvikov z assets/katalog.json do katalog.db.
 *
 * Formát súboru:
 * ```
 * {
 *   "verzia": 1,
 *   "rebriky": [
 *     {
 *       "id": "kliky", "nazov": "Kliky", "popis": "...",
 *       "skupina": "tlak", "jeZrucnost": false, "poradie": 10,
 *       "cviky": [
 *         { "id": "kliky-1", "uroven": 1, "nazov": "Kliky o stenu",
 *           "meranie": "opakovania", "cielMin": 8, "cielMax": 15,
 *           "naradie": ["stena"], "partie": {"hrudnik": 1.0, "triceps": 0.5},
 *           "cue": ["..."], "chyby": ["..."], "jeDetsky": false, "zviera": null,
 *           "pohyb": "klik-stena" }
 *       ]
 *     }
 *   ]
 * }
 * ```
 *
 * Katalóg sa prepisuje vždy, keď sa zmení `verzia` v súbore alebo keď je databáza prázdna.
 * Používateľské úrovne v rebríkoch to neovplyvní - tie sú v denik.db.
 */
object KatalogLoader {

    private const val TAG = "KatalogLoader"
    private const val SUBOR = "katalog.json"
    private const val PREFS = "katalog_prefs"
    private const val KLUC_VERZIA = "nacitana_verzia"

    /** Vráti počet načítaných cvikov, alebo -1 ak sa načítanie nevykonalo (netreba). */
    suspend fun nacitajAkTreba(context: Context): Int = try {
        nacitaj(context)
    } catch (t: Throwable) {
        // Chybný katalóg nesmie zhodiť celú appku. Appka sa spustí s prázdnou knižnicou
        // a obrazovka "Dnes" to používateľovi povie priamo, namiesto tichého pádu.
        Log.e(TAG, "Katalóg sa nepodarilo načítať: ${t.message}", t)
        -1
    }

    private suspend fun nacitaj(context: Context): Int {
        val dao = KatalogDatabase.ziskaj(context).dao()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = context.assets.open(SUBOR).bufferedReader().use { it.readText() }

        val koren = JSONObject(json)
        val verziaSuboru = koren.optInt("verzia", 1)
        val verziaVDb = prefs.getInt(KLUC_VERZIA, -1)
        val pocetVDb = dao.pocetCvikov()

        if (verziaVDb == verziaSuboru && pocetVDb > 0) return -1

        val rebriky = mutableListOf<RebrikEntity>()
        val cviky = mutableListOf<CvikEntity>()
        val poleRebrikov = koren.getJSONArray("rebriky")

        for (i in 0 until poleRebrikov.length()) {
            val r = poleRebrikov.getJSONObject(i)
            val rebrikId = r.getString("id")
            rebriky += RebrikEntity(
                id = rebrikId,
                nazov = r.getString("nazov"),
                popis = r.optString("popis", ""),
                skupina = r.getString("skupina"),
                jeZrucnost = r.optBoolean("jeZrucnost", false),
                poradie = r.optInt("poradie", 100),
            )

            val poleCvikov = r.getJSONArray("cviky")
            for (j in 0 until poleCvikov.length()) {
                val c = poleCvikov.getJSONObject(j)
                cviky += CvikEntity(
                    id = c.getString("id"),
                    rebrikId = rebrikId,
                    uroven = c.getInt("uroven"),
                    nazov = c.getString("nazov"),
                    meranie = c.optString("meranie", "opakovania"),
                    cielMin = c.optInt("cielMin", 8),
                    cielMax = c.optInt("cielMax", 12),
                    naradie = c.optJSONArray("naradie").naCsv(),
                    partie = c.optJSONObject("partie").naPartie(),
                    cue = c.optJSONArray("cue").naZoznam(),
                    chyby = c.optJSONArray("chyby").naZoznam(),
                    jeDetsky = c.optBoolean("jeDetsky", false),
                    zviera = c.optString("zviera").takeIf { it.isNotBlank() && it != "null" },
                    znacky = c.optJSONArray("znacky").naCsv(),
                    pohyb = c.optString("pohyb").takeIf { it.isNotBlank() && it != "null" },
                )
            }
        }

        dao.prepisKatalog(rebriky, cviky)
        prefs.edit().putInt(KLUC_VERZIA, verziaSuboru).apply()
        Log.i(TAG, "Katalóg načítaný: ${rebriky.size} rebríkov, ${cviky.size} cvikov (verzia $verziaSuboru)")
        return cviky.size
    }

    private fun org.json.JSONArray?.naCsv(): String {
        if (this == null) return ""
        return (0 until length()).joinToString(",") { optString(it) }
    }

    private fun org.json.JSONArray?.naZoznam(): String {
        if (this == null) return ""
        return (0 until length()).joinToString("|") { optString(it) }
    }

    private fun JSONObject?.naPartie(): String {
        if (this == null) return ""
        val kluce = keys().asSequence().toList()
        return kluce.joinToString(",") { "$it:${optDouble(it, 1.0)}" }
    }
}

/** Rozparsuje reťazec "chrbat:1.0,biceps:0.5" na dvojice. */
fun rozparsujPartie(text: String): List<Pair<String, Double>> =
    text.split(",")
        .mapNotNull { kus ->
            val casti = kus.split(":")
            if (casti.size != 2) return@mapNotNull null
            val kluc = casti[0].trim()
            val vaha = casti[1].trim().toDoubleOrNull() ?: return@mapNotNull null
            if (kluc.isBlank()) null else kluc to vaha
        }
