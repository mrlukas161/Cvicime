package sk.havrila.cvicime.trening

import java.text.Normalizer

/**
 * Výklad toho, čo cvičiaci povedal (text z rozpoznávača reči). Čistý Kotlin - testuje sa bez Androidu.
 *
 * Zámerne jednoduché pravidlá a malá slovná zásoba: krátke povely počas cvičenia, nie rozhovor.
 * Čo sa nedá spoľahlivo pochopiť, vráti null - appka vtedy nič nerobí a ostanú tlačidlá.
 */
sealed interface Povel {
    /** Číslo (počet opakovaní, sekúnd, rezerva). */
    data class Cislo(val hodnota: Int) : Povel
    /** „hotovo", „ďalej", „ďalší" - ukončiť sériu / ísť na ďalší krok. */
    data object Dalej : Povel
    /** „preskoč" - preskočiť pauzu alebo sériu. */
    data object Preskoc : Povel
    /** „pauza", „stop", „počkaj" - pozastaviť. */
    data object Pozastav : Povel
    /** „pokračuj", „ideme" - pokračovať po pozastavení. */
    data object Pokracuj : Povel
    /** „pridaj", „ešte chvíľu" - +15 s pauzy. */
    data object Pridaj : Povel
    /** „nič", „už nič" - rezerva 0. */
    data object Nic : Povel
}

object Povely {

    private val JEDNOTKY = mapOf(
        "nula" to 0,
        "jeden" to 1, "jedna" to 1, "jedno" to 1, "jedenkrat" to 1,
        "dva" to 2, "dve" to 2, "dvakrat" to 2,
        "tri" to 3, "trikrat" to 3,
        "styri" to 4, "stiri" to 4,
        "pat" to 5,
        "sest" to 6,
        "sedem" to 7,
        "osem" to 8,
        "devat" to 9,
        "desat" to 10,
        "jedenast" to 11,
        "dvanast" to 12,
        "trinast" to 13,
        "strnast" to 14,
        "patnast" to 15,
        "sestnast" to 16,
        "sedemnast" to 17,
        "osemnast" to 18,
        "devatnast" to 19,
    )

    private val DESIATKY = mapOf(
        "dvadsat" to 20,
        "tridsat" to 30,
        "styridsat" to 40,
        "patdesiat" to 50,
        "sestdesiat" to 60,
    )

    /** Bez diakritiky a malými - rozpoznávač niekedy vráti „Desať", inokedy „desat". */
    fun normalizuj(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Prvé číslo v texte: „10", „desať", „dvadsaťpäť", „dvadsať päť". null = žiadne.
     * Rozsah 0-99 stačí na opakovania aj sekundy výdrže.
     */
    fun cislo(text: String): Int? {
        val slova = normalizuj(text).split(" ").filter { it.isNotBlank() }
        for ((i, s) in slova.withIndex()) {
            s.toIntOrNull()?.let { if (it in 0..999) return it }
            DESIATKY.entries.firstOrNull { s.startsWith(it.key) }?.let { (kluc, d) ->
                val zvysok = s.removePrefix(kluc)
                val jednotka = JEDNOTKY[zvysok] ?: slova.getOrNull(i + 1)?.let { JEDNOTKY[it] }?.takeIf { it in 1..9 }
                return d + (jednotka ?: 0)
            }
            JEDNOTKY[s]?.let { return it }
        }
        return null
    }

    /** Povel z textu; čísla majú prednosť (odpoveď na „koľko"), potom slová. */
    fun rozpoznaj(text: String): Povel? {
        val t = normalizuj(text)
        if (t.isBlank()) return null
        if (Regex("\\b(uz )?nic\\b").containsMatchIn(t) && cislo(t) == null) return Povel.Nic
        cislo(t)?.let { return Povel.Cislo(it) }
        return when {
            Regex("\\bpreskoc|\\bvynechaj|\\bskip").containsMatchIn(t) -> Povel.Preskoc
            Regex("\\bpokracuj|\\bideme|\\bpodme|\\bstart").containsMatchIn(t) -> Povel.Pokracuj
            Regex("\\bpauza|\\bstop|\\bpockaj|\\bzastav").containsMatchIn(t) -> Povel.Pozastav
            Regex("\\bpridaj|\\beste chvil|\\bviac casu").containsMatchIn(t) -> Povel.Pridaj
            Regex("\\bhotovo|\\bdalej|\\bdalsi|\\bdalsie|\\bmam\\b|\\bdost").containsMatchIn(t) -> Povel.Dalej
            else -> null
        }
    }

    /** Rezerva (RIR) z odpovede na „Koľko si mal ešte v sebe?": 0 / 1-2 / 3+. */
    fun rezerva(text: String): Int? = when (val p = rozpoznaj(text)) {
        Povel.Nic -> 0
        is Povel.Cislo -> when {
            p.hodnota <= 0 -> 0
            p.hodnota <= 2 -> 1
            else -> 3
        }
        else -> if (Regex("\\bvela|\\bkopec|\\bdost").containsMatchIn(normalizuj(text))) 3 else null
    }
}
