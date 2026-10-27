package sk.havrila.cvicime.domain

/**
 * Ako sa má dnešný tréning cvičiť - volí sa na Domove pred štartom („Ako dnes")
 * a pamätá sa. Predvolené hodnoty sú tie, ktoré odporúča rešerš 0.8 (docs/specs/2026-10-26-0.8-navrh.md).
 */
data class NastavenieTreningu(
    val styl: StylTreningu = StylTreningu.KLASIKA,
    /** true = aj cviky na opakovania sa cvičia na odpočet [pracaS] (počet sa zapíše po sérii) */
    val vsetkoNaCas: Boolean = false,
    val pracaS: Int = 40,
    val pauzy: DlzkaPauz = DlzkaPauz.AUTOMATICKY,
)

enum class StylTreningu(val kluc: String, val nazov: String, val popis: String) {
    KLASIKA(
        "klasika", "Klasika",
        "Série jedného cviku za sebou, medzi nimi pauza. Najlepšie na silu.",
    ),
    PARY(
        "pary", "Páry",
        "Dva cviky na iné svaly (napr. dipy + príťahy) striedavo - kratšie, rovnaký výsledok.",
    ),
    KRUH(
        "kruh", "Intenzívny kruh",
        "Jednoduché cviky na čas dokola, takmer bez pauzy. Zdvihne tep a zacvičí, " +
            "ale na maximálnu silu a zručnosti je lepšia Klasika alebo Páry. Najviac 2–3× do týždňa.",
    );

    companion object {
        fun z(kluc: String?): StylTreningu = entries.firstOrNull { it.kluc == kluc } ?: KLASIKA
    }
}

enum class DlzkaPauz(val kluc: String, val nazov: String, val nasobok: Double) {
    KRATSIE("kratsie", "Kratšie", 0.67),
    AUTOMATICKY("auto", "Automaticky", 1.0),
    DLHSIE("dlhsie", "Dlhšie", 1.33);

    companion object {
        fun z(kluc: String?): DlzkaPauz = entries.firstOrNull { it.kluc == kluc } ?: AUTOMATICKY
    }
}

/** Ponúkané dĺžky práce pri „všetko na čas" a v kruhu. */
val PONUKA_PRACE_S = listOf(30, 40, 45, 60)
