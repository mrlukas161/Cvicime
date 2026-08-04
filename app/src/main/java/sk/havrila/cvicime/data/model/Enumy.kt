package sk.havrila.cvicime.data.model

/** Ako sa cvik meria - urcuje aj rezim prehravaca. */
enum class Meranie(val kluc: String, val jednotka: String) {
    OPAKOVANIA("opakovania", "×"),
    VYDRZ("vydrz", "s"),
    VZDIALENOST("vzdialenost", "m");

    companion object {
        fun z(kluc: String): Meranie = entries.firstOrNull { it.kluc == kluc } ?: OPAKOVANIA
    }
}

/**
 * Svalove partie, na ktorych sa sleduje tyzdenny objem.
 * Zamerne hrubsie delenie - jemnejsie by pri kalistenike nemalo co merat.
 */
enum class Partia(val kluc: String, val nazov: String) {
    HRUDNIK("hrudnik", "Hrudník"),
    CHRBAT("chrbat", "Chrbát"),
    RAMENA("ramena", "Ramená"),
    BICEPS("biceps", "Biceps"),
    TRICEPS("triceps", "Triceps"),
    PREDLAKTIA("predlaktia", "Predlaktia"),
    STRED("stred", "Stred tela"),
    STEHNA("stehna", "Stehná"),
    ZADOK("zadok", "Zadok"),
    HAMSTRINGY("hamstringy", "Zadné stehná"),
    LYTKA("lytka", "Lýtka");

    companion object {
        fun z(kluc: String): Partia? = entries.firstOrNull { it.kluc == kluc }
    }
}

/** Naradie, ktore cvik vyzaduje. */
enum class Naradie(val kluc: String, val nazov: String) {
    ZIADNE("ziadne", "Bez náradia"),
    HRAZDA("hrazda", "Hrazda"),
    JEDNORUCKY("jednorucky", "Jednoručky"),
    PODLOZKA("podlozka", "Podložka"),
    STOLICKA("stolicka", "Stolička"),
    SCHOD("schod", "Schod / stupienok"),
    STENA("stena", "Stena");

    companion object {
        fun z(kluc: String): Naradie? = entries.firstOrNull { it.kluc == kluc }
    }
}

/**
 * Bloky treningu v PEVNOM poradi. Poradie nie je kozmeticke - vyplyva z vyskumu:
 * skill sa trenuje kym je clovek cerstvy, sila pred kondiciou, mobilita az na konci.
 */
enum class Blok(val kluc: String, val nazov: String, val poradie: Int) {
    ROZOHRIATIE("rozohriatie", "Rozohriatie", 1),
    SKILL("skill", "Zručnosť", 2),
    SILA("sila", "Sila", 3),
    KONDICIA("kondicia", "Kondičný kruh", 4),
    MOBILITA("mobilita", "Mobilita", 5);

    companion object {
        fun z(kluc: String): Blok = entries.firstOrNull { it.kluc == kluc } ?: SILA
    }
}

/** Typ ciela pouzivatela. Appka je tym univerzalna - ciele nie su zadratovane. */
enum class DruhCiela(val kluc: String) {
    ZRUCNOST("zrucnost"),
    PARTIA("partia"),
    VSEOBECNY("vseobecny");

    companion object {
        fun z(kluc: String): DruhCiela = entries.firstOrNull { it.kluc == kluc } ?: VSEOBECNY
    }
}

/**
 * Odpoved na otazku po serii: "Kolko si mal este v sebe?"
 * Zamerne len tri moznosti - jemnejsia skala je u zaciatocnika aj tak sum.
 */
enum class Rir(val hodnota: Int, val popis: String) {
    NIC(0, "Už nič"),
    JEDNO_DVE(1, "Ešte 1–2"),
    TRI_A_VIAC(3, "Ešte 3+");

    companion object {
        fun z(hodnota: Int): Rir = entries.firstOrNull { it.hodnota == hodnota } ?: JEDNO_DVE
    }
}

/** Skupina rebrika - pouziva sa pri vybere cvikov do silového bloku. */
enum class Skupina(val kluc: String, val nazov: String) {
    TLAK("tlak", "Tlak"),
    TAH("tah", "Ťah"),
    NOHY("nohy", "Nohy"),
    STRED("stred", "Stred tela"),
    ZRUCNOST("zrucnost", "Zručnosť"),
    MOBILITA("mobilita", "Mobilita"),
    ROZOHRIATIE("rozohriatie", "Rozohriatie"),
    DETSKY("detsky", "Detské");

    companion object {
        fun z(kluc: String): Skupina = entries.firstOrNull { it.kluc == kluc } ?: TLAK
    }
}
