package sk.havrila.cvicime.ui.komponenty.pohyb

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** Načítané vzory pohybov z assets/pohyby.json. */
class KatalogPohybov(val verzia: Int, val rozmery: Rozmery, val vzory: List<Vzor>) {
    private val podlaId = vzory.associateBy { it.id }
    operator fun get(id: String): Vzor? = podlaId[id]
    fun ma(id: String?): Boolean = id != null && id in podlaId
}

/**
 * Prístup k vzorom pohybov. Súbor sa načíta raz a drží sa v pamäti (~100 kB JSON,
 * parsovanie pár milisekúnd), takže [Postavicka][sk.havrila.cvicime.ui.komponenty.Postavicka]
 * ho môže pýtať pri každom vykreslení.
 *
 * Vzory sú dáta - pózy sa opravujú v pohyby.json, nie v kóde. Kontrola bez telefónu:
 * `python3 tools/pohyby_nahlad.py`.
 */
object Pohyby {

    private const val TAG = "Pohyby"
    private const val SUBOR = "pohyby.json"

    @Volatile
    private var katalog: KatalogPohybov? = null

    fun katalog(context: Context): KatalogPohybov =
        katalog ?: synchronized(this) {
            katalog ?: nacitaj(context).also { katalog = it }
        }

    private fun nacitaj(context: Context): KatalogPohybov = try {
        val text = context.applicationContext.assets.open(SUBOR).bufferedReader().use { it.readText() }
        PohybyJson.parsuj(text)
    } catch (t: Throwable) {
        // Chybný súbor nesmie zhodiť obrazovku - postavička sa len nenakreslí.
        Log.e(TAG, "Pohyby sa nepodarilo načítať: ${t.message}", t)
        KatalogPohybov(0, Rozmery(), emptyList())
    }

    /** Existuje vzor s týmto id? */
    fun maPohyb(context: Context, pohyb: String?): Boolean = katalog(context).ma(pohyb)

    /** Vzor pre konkrétny pohyb, inak predvolený pohyb rebríka, inak null. */
    fun vzor(context: Context, pohyb: String?, rebrikId: String?): Vzor? {
        val k = katalog(context)
        return pohyb?.let { k[it] } ?: rebrikId?.let { predvolenyPre(it) }?.let { k[it] }
    }

    /**
     * Predvolený pohyb rebríka - kreslí sa tam, kde sa ukazuje rebrík ako celok
     * (karta v knižnici, test, onboarding). Konkrétny cvik má vlastný `pohyb` v katalógu.
     */
    fun predvolenyPre(rebrikId: String): String? = PREDVOLENE[rebrikId]

    val PREDVOLENE: Map<String, String> = mapOf(
        "rozohriatie" to "vysoke-kolena",
        "lsit" to "lsit",
        "stojka" to "stojka",
        "muscleup" to "muscleup",
        "panvove-dno" to "panvove-dno-lah",
        "kliky" to "klik",
        "tlak-nad-hlavu" to "tlak-stoj",
        "kliky-do-stojky" to "pike-klik",
        "dipy" to "dip",
        "pritahy" to "pritah",
        "veslovanie" to "veslovanie-predklon",
        "zadne-delty" to "supermanka",
        "vis" to "vis",
        "predlaktia" to "zapastie-sed",
        "drep" to "drep",
        "vypady" to "vypad-vzad",
        "sldl" to "mrtvy-tah-jednonoz",
        "most" to "most",
        "vypony" to "vypon-stena",
        "poskoky" to "poskok",
        "doska" to "doska",
        "hollow" to "hollow-hold",
        "nohy-k-hrazde" to "vis-kolena-hrud",
        "mobilita-ramena" to "klzanie-po-stene",
        "mobilita-kompresia" to "predklon-sed",
        "mobilita-clenky" to "hlboky-drep-vydrz",
        "mobilita-zapastia" to "opora-klak-hojdanie",
        "detske-chodze" to "medvedia-chodza",
        "detske-skoky" to "zabie-skoky",
        "detske-vydrze" to "bocian",
        "detske-hrazda" to "vis",
    )
}

/** Parser pohyby.json (org.json). Formát popisuje tools/pohyby_nahlad.py. */
object PohybyJson {

    fun parsuj(text: String): KatalogPohybov {
        val koren = JSONObject(text)
        val k = koren.optJSONObject("kostra")
        val zaklad = Rozmery()
        val rozmery = if (k == null) zaklad else Rozmery(
            trup = k.optDouble("trup", zaklad.trup),
            krk = k.optDouble("krk", zaklad.krk),
            hlava = k.optDouble("hlava", zaklad.hlava),
            stehno = k.optDouble("stehno", zaklad.stehno),
            holen = k.optDouble("holen", zaklad.holen),
            chodidlo = k.optDouble("chodidlo", zaklad.chodidlo),
            rameno = k.optDouble("rameno", zaklad.rameno),
            predlaktie = k.optDouble("predlaktie", zaklad.predlaktie),
            dlan = k.optDouble("dlan", zaklad.dlan),
            panva = k.optDouble("panva", zaklad.panva),
            plecia = k.optDouble("plecia", zaklad.plecia),
        )
        val pole = koren.getJSONArray("pohyby")
        val vzory = (0 until pole.length()).map { vzor(pole.getJSONObject(it), rozmery) }
        return KatalogPohybov(koren.optInt("verzia", 1), rozmery, vzory)
    }

    private fun vzor(o: JSONObject, rozmery: Rozmery): Vzor {
        val pohlad = when (o.optString("pohlad", "bok")) {
            "pred" -> Pohlad.PRED
            "zhora" -> Pohlad.ZHORA
            else -> Pohlad.BOK
        }
        val druh = o.optString("druh", "opakovanie")
        val rekv = o.optJSONArray("rekvizity") ?: JSONArray()
        val rekvizity = (0 until rekv.length()).map { i ->
            val r = rekv.getJSONObject(i)
            val cisla = buildMap {
                for (kluc in r.keys()) {
                    val hodnota = r.get(kluc)
                    if (hodnota is Number) put(kluc, hodnota.toDouble())
                }
            }
            Rekvizita(r.getString("typ"), cisla, r.optString("bod").takeIf { it.isNotBlank() })
        }
        val pz = o.getJSONArray("pozy")
        val pozy = (0 until pz.length()).map { poza(pz.getJSONObject(it), pohlad == Pohlad.BOK) }
        return Vzor(
            id = o.getString("id"),
            nazov = o.optString("nazov", o.getString("id")),
            pohlad = pohlad,
            druh = druh,
            posun = o.optDouble("posun", 0.0),
            linearna = o.optString("krivka", "plynula") == "linearna",
            dych = if (o.has("dych")) o.getBoolean("dych") else druh == "vydrz",
            priemetTrupu = o.optDouble("priemetTrupu", 1.0),
            oko = o.optBoolean("oko", true),
            rekvizity = rekvizity,
            pozy = pozy,
            rozmery = rozmery,
        )
    }

    private fun poza(o: JSONObject, zboku: Boolean): Poza {
        val bokPole = o.getJSONArray("bok")
        val bok = Bod(bokPole.getDouble(0), bokPole.getDouble(1))
        val trup = o.getDouble("trup")
        val nP = koncatina(o.get("nP"))
        val rP = koncatina(o.get("rP"))
        val nZ = if (o.has("nZ")) koncatina(o.get("nZ")) else if (zboku) nP else zrkadlo(nP, bok.x, noha = true)
        val rZ = if (o.has("rZ")) koncatina(o.get("rZ")) else if (zboku) rP else zrkadlo(rP, bok.x, noha = false)
        return Poza(
            bok = bok,
            trup = trup,
            hlava = o.optDouble("hlava", trup),
            chrbat = o.optDouble("chrbat", 0.0),
            lopatky = o.optDouble("lopatky", 0.0),
            koncatiny = mapOf(Clen.NP to nP, Clen.NZ to nZ, Clen.RP to rP, Clen.RZ to rZ),
            t = o.optInt("t", 1000),
            drz = o.optInt("drz", 0),
        )
    }

    private fun koncatina(h: Any): Koncatina = when (h) {
        is JSONArray -> Koncatina.Uhly(
            h.getDouble(0), h.getDouble(1), if (h.length() > 2) h.getDouble(2) else null,
        )
        is JSONObject -> {
            val c = if (h.has("c")) h.getDouble("c") else null
            if (h.has("ik")) {
                val p = h.getJSONArray("ik")
                Koncatina.Ik(p.getDouble(0), p.getDouble(1), if (h.has("o")) h.getInt("o") else null, c)
            } else {
                val p = h.getJSONArray("na")
                Koncatina.Na(p.getDouble(0), p.getDouble(1), h.getDouble("u2"), c)
            }
        }
        else -> error("Neznáma končatina: $h")
    }

    /** Zrkadlenie bližšej končatiny na vzdialenejšiu pri pohľade spredu. */
    fun zrkadlo(k: Koncatina, osX: Double, noha: Boolean): Koncatina = when (k) {
        is Koncatina.Uhly -> Koncatina.Uhly(180 - k.u1, 180 - k.u2, k.u3?.let { 180 - it })
        is Koncatina.Ik -> Koncatina.Ik(2 * osX - k.x, k.y, -(k.o ?: if (noha) 1 else -1), k.c?.let { 180 - it })
        is Koncatina.Na -> Koncatina.Na(2 * osX - k.x, k.y, 180 - k.u2, k.c?.let { 180 - it })
    }
}
