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
    /**
     * Hrazda s voľným priestorom NAD ňou (workout ihrisko), ktorá znesie prácu v podpore.
     * Oddelené od bežnej hrazdy zámerne: dverová hrazda neznesie muscle-up, ale ťahové
     * a prechodové drilly áno - takto sa dá cieľ trénovať doma a vrchné stupne na ihrisku.
     */
    PEVNA_HRAZDA("pevna_hrazda", "Pevná hrazda (vonku)"),
    /** Bradlá samostatné alebo ako súčasť veže (hrazda + bradlá). Odomkne dipy a podpory. */
    BRADLA("bradla", "Bradlá / veža"),
    /** Nízke paralety - L-sit, kliky do stojky s väčším rozsahom, podpory. */
    PARALETY("paralety", "Paralety"),
    KRUHY("kruhy", "Gymnastické kruhy"),
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
    /** Intenzívny kruh (štýl KRUH) - nahrádza silu aj kondíciu; do postupov v rebríkoch sa neráta. */
    KRUH("kruh", "Intenzívny kruh", 4),
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

/**
 * Zdravotné/pohybové obmedzenie ďalšieho cvičiaceho. Každé vylúči cviky s danými značkami
 * (pole `znacky` v katalógu). Engine potom ustúpi na nižší stupeň bez značky alebo rebrík vynechá -
 * rovnako ako pri chýbajúcom náradí.
 */
enum class Obmedzenie(
    val kluc: String,
    val nazov: String,
    val popis: String,
    val vylucujeZnacky: Set<String>,
    /** dlhší text režimu pre info pri prepínači; null = stačí [popis] */
    val vysvetlenie: String? = null,
    /** otázka po sérii, ktorej „áno" zastaví postup v rebríku; null = režim sa nepýta */
    val otazkaPoSerii: String? = null,
) {
    PANVOVE_DNO(
        "panvove_dno", "Šetriť panvové dno",
        "Bez skokov a cvikov s veľkým tlakom do brucha. Pridá tréning panvového dna.",
        setOf("skok", "tlak_brucho"),
        vysvetlenie = "Režim Panvové dno je pre ženy, ktoré po pôrodoch cítia tlak, ťah alebo pocit, " +
            "že niečo tlačí dole, prípadne im pri pohybe unikne moč. Pohyb neruší, len vymení skoky, " +
            "beh a cviky s veľkým tlakom do brucha za verzie, ktoré robíš popri ostatných v tom istom " +
            "bloku. Pred každou námahou jemne stiahni panvové dno a počas záťaže vydýchni, dych " +
            "nezadržiavaj. Riaď sa telom: keď cítiš tlak, ťah alebo únik, uber záťaž alebo sa vráť " +
            "o stupeň nižšie – nie je to zlyhanie, ale informácia. Cviky panvového dna účinkujú, keď " +
            "ich robíš pravidelne niekoľko mesiacov, najlepšie s fyzioterapeutom. Appka nenahrádza " +
            "vyšetrenie: ak ťažkosti trvajú alebo cítiš či vidíš vydutie pri vchode do pošvy, choď " +
            "ku gynekológovi.",
        otazkaPoSerii = "Cítila si tlak, ťah alebo únik?",
    ),
    BEZ_SKOKOV("bez_skokov", "Bez skokov", "Žiadne doskoky ani poskoky.", setOf("skok")),
    RAMENO("rameno", "Šetriť rameno", "Bez visov, záťaže nad hlavou a hlbokých vzporov.", setOf("vis", "nad_hlavu", "rameno_zataz"));

    companion object {
        fun z(kluc: String): Obmedzenie? = entries.firstOrNull { it.kluc == kluc }

        fun zCsv(csv: String): Set<Obmedzenie> =
            csv.split(",").mapNotNull { z(it.trim()) }.toSet()
    }
}

