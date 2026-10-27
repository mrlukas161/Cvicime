package sk.havrila.cvicime.data

import android.content.Context
import sk.havrila.cvicime.domain.DlzkaPauz
import sk.havrila.cvicime.domain.NastavenieTreningu
import sk.havrila.cvicime.domain.StylTreningu
import sk.havrila.cvicime.trening.RezimZvuku

/** Drobné nastavenia, ktoré nemá zmysel držať v databáze. */
class Prefs(context: Context) {

    private val p = context.applicationContext.getSharedPreferences("cvicime", Context.MODE_PRIVATE)

    var poslednyPozdravDen: Long
        get() = p.getLong("posledny_pozdrav_den", 0)
        set(v) = p.edit().putLong("posledny_pozdrav_den", v).apply()

    /** Naposledy zvolený dostupný čas (minúty) - predvyplní sa nabudúce. */
    var poslednyCasMinut: Int
        get() = p.getInt("posledny_cas_minut", 30)
        set(v) = p.edit().putInt("posledny_cas_minut", v).apply()

    var hlasZapnuty: Boolean
        get() = p.getBoolean("hlas_zapnuty", true)
        set(v) = p.edit().putBoolean("hlas_zapnuty", v).apply()

    /** Zvuk prehrávača (tlačidlo reproduktora). Kým nie je zvolený, odvodí sa zo starého prepínača hlasu. */
    var rezimZvuku: RezimZvuku
        get() = RezimZvuku.z(p.getString("rezim_zvuku", null))
            ?: if (hlasZapnuty) RezimZvuku.HLAS else RezimZvuku.PIPANIE
        set(v) = p.edit().putString("rezim_zvuku", v.kluc).apply()

    /** Prehrávač s väčším písmom a číslami na zrkadlenie na TV. */
    var tvRezim: Boolean
        get() = p.getBoolean("tv_rezim", false)
        set(v) = p.edit().putBoolean("tv_rezim", v).apply()

    /** Počas tréningu držať obrazovku na ležato (telefón leží, obraz ide na TV). */
    var zamknutNaLezato: Boolean
        get() = p.getBoolean("zamknut_na_lezato", false)
        set(v) = p.edit().putBoolean("zamknut_na_lezato", v).apply()

    /** „Ako dnes" - pamätá sa posledná voľba. */
    var nastavenieTreningu: NastavenieTreningu
        get() = NastavenieTreningu(
            styl = StylTreningu.z(p.getString("styl_treningu", null)),
            vsetkoNaCas = p.getBoolean("vsetko_na_cas", false),
            pracaS = p.getInt("praca_s", 40),
            pauzy = DlzkaPauz.z(p.getString("dlzka_pauz", null)),
        )
        set(v) = p.edit()
            .putString("styl_treningu", v.styl.kluc)
            .putBoolean("vsetko_na_cas", v.vsetkoNaCas)
            .putInt("praca_s", v.pracaS)
            .putString("dlzka_pauz", v.pauzy.kluc)
            .apply()

    var vibracieZapnute: Boolean
        get() = p.getBoolean("vibracie_zapnute", true)
        set(v) = p.edit().putBoolean("vibracie_zapnute", v).apply()

    var obrazovkaStaleZapnuta: Boolean
        get() = p.getBoolean("obrazovka_stale_zapnuta", true)
        set(v) = p.edit().putBoolean("obrazovka_stale_zapnuta", v).apply()

    /** Bol už používateľovi vysvetlený prvý mesiac (neurálne adaptácie)? */
    var vysvetlenyPrvyMesiac: Boolean
        get() = p.getBoolean("vysvetleny_prvy_mesiac", false)
        set(v) = p.edit().putBoolean("vysvetleny_prvy_mesiac", v).apply()

    var poslednaKontrolaAktualizacie: Long
        get() = p.getLong("posledna_kontrola_aktualizacie", 0)
        set(v) = p.edit().putLong("posledna_kontrola_aktualizacie", v).apply()

    /** Id rozcvičeného tréningu, ktorý sa má po návrate do appky obnoviť. */
    var beziaciTreningId: Long
        get() = p.getLong("beziaci_trening_id", -1)
        set(v) = p.edit().putLong("beziaci_trening_id", v).apply()

    /** Deň, kedy sa naposledy položila otázka na prekážku - aby sa nepýtala dvakrát. */
    var prekazkaDen: Long
        get() = p.getLong("prekazka_den", 0)
        set(v) = p.edit().putLong("prekazka_den", v).apply()
}
