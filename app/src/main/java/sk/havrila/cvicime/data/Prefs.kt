package sk.havrila.cvicime.data

import android.content.Context

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
