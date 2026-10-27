package sk.havrila.cvicime.domain

import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Meranie

/**
 * Jedna séria v poradí, v akom sa reálne odcvičí, a koľko sa po nej oddychuje.
 * [kolo]/[kol] má zmysel pri pároch a v kruhu (kolo 2 z 3), inak je to číslo série.
 */
data class RozvrhKrok(
    val blok: Blok,
    val polozka: PlanPolozka,
    val cisloSerie: Int,
    val zoSerii: Int,
    val pauzaPoS: Int,
)

/**
 * Z plánu urobí poradie sérií. Tu sa rozhoduje o pároch a kruhu - prehrávač už len ide krok za krokom.
 *  - samostatný cvik: všetky jeho série za sebou, po každej [PlanPolozka.pauzaS],
 *  - pár (rovnaké [PlanPolozka.par]): A1 → krátky prechod → A2 → pauza → A1 …,
 *  - kruh: kolo = každý cvik raz s prechodom, po kole dlhšia pauza.
 */
object Rozvrh {

    fun kroky(plan: PlanTreningu): List<RozvrhKrok> = plan.bloky.flatMap { kroky(it) }

    fun kroky(blok: PlanBloku): List<RozvrhKrok> {
        if (blok.blok == Blok.KRUH) return kruh(blok)
        val vysledok = mutableListOf<RozvrhKrok>()
        val spracovane = mutableSetOf<Int>()
        blok.polozky.forEachIndexed { i, p ->
            if (i in spracovane) return@forEachIndexed
            val par = p.par
            if (par == null) {
                spracovane += i
                (1..p.serie).forEach { s -> vysledok += RozvrhKrok(blok.blok, p, s, p.serie, p.pauzaS) }
                return@forEachIndexed
            }
            val clenovia = blok.polozky.withIndex().filter { it.value.par == par }
            spracovane += clenovia.map { it.index }
            val kol = clenovia.maxOf { it.value.serie }
            for (s in 1..kol) {
                val vKole = clenovia.filter { s <= it.value.serie }
                vKole.forEachIndexed { j, c ->
                    val posledny = j == vKole.lastIndex
                    vysledok += RozvrhKrok(
                        blok = blok.blok,
                        polozka = c.value,
                        cisloSerie = s,
                        zoSerii = c.value.serie,
                        pauzaPoS = if (posledny) c.value.pauzaS else c.value.prechodS,
                    )
                }
            }
        }
        return vysledok
    }

    private fun kruh(blok: PlanBloku): List<RozvrhKrok> {
        val kol = blok.polozky.maxOfOrNull { it.serie } ?: return emptyList()
        return (1..kol).flatMap { k ->
            val vKole = blok.polozky.filter { k <= it.serie }
            vKole.mapIndexed { j, p ->
                RozvrhKrok(
                    blok = blok.blok,
                    polozka = p,
                    cisloSerie = k,
                    zoSerii = kol,
                    pauzaPoS = if (j == vKole.lastIndex) p.pauzaS else p.prechodS,
                )
            }
        }
    }

    /** Sekundy práce jednej série - na čas podľa odpočtu, inak odhad z opakovaní. */
    fun sekundPrace(p: PlanPolozka): Int = p.naCasS ?: when (Meranie.z(p.cvik.meranie)) {
        Meranie.VYDRZ -> p.ciel
        Meranie.VZDIALENOST -> 30
        // ~3,5 s na opakovanie + nastavenie do polohy; kratšia séria než 20 s reálne netrvá
        Meranie.OPAKOVANIA -> (p.ciel * 3.5).toInt().coerceAtLeast(20)
    }

    /**
     * Poctivý odhad trvania: práca + pauzy po sériách (aj medzi cvikmi) + ~8 s na presun
     * k ďalšiemu cviku. Posledná pauza tréningu sa neráta.
     */
    fun odhadMinut(kroky: List<RozvrhKrok>): Int {
        if (kroky.isEmpty()) return 0
        var sekundy = 0
        kroky.forEachIndexed { i, k ->
            sekundy += sekundPrace(k.polozka)
            if (i < kroky.lastIndex) {
                sekundy += k.pauzaPoS
                if (kroky[i + 1].polozka.cvik.id != k.polozka.cvik.id) sekundy += 8
            }
        }
        return ((sekundy + 59) / 60)
    }
}
