package sk.havrila.cvicime.domain

import sk.havrila.cvicime.data.model.Partia

/**
 * Týždenný objem na svalovú partiu.
 *
 * Podklad: meta-regresia dose-response (Pelland et al., Sports Medicine 2025, 67 štúdií / 2058 ľudí)
 * - rast stúpa s objemom, ale s klesajúcim výnosom. Preto appka NEŽENIE objem hore donekonečna:
 * začína nízko, dvíha pomaly a má strop.
 *
 * Lýtka majú vlastný, nižší strop: pri ~9 sériách/týždeň je výsledok prakticky rovnaký
 * ako pri 12, takže vyšší objem je len zbytočná únava.
 */
object Objem {

    /** Štartovací týždenný objem na partiu pre začiatočníka. */
    const val START_SERII = 8.0

    /** O koľko sérií sa cieľ dvíha za každé 2 týždne pravidelného tréningu. */
    private const val PRIRASTOK_ZA_2_TYZDNE = 1.0

    /** Strop pre partiu, ktorú si používateľ zvolil ako cieľ. */
    private const val STROP_PRIORITNA = 16.0

    /** Strop pre ostatné partie - udržiavací objem. */
    private const val STROP_BEZNA = 12.0

    /** Lýtka: nad ~12 sérií už podľa štúdií nič nepribúda. */
    private const val STROP_LYTKA = 12.0

    /**
     * Cieľový počet sérií na partiu za týždeň.
     *
     * @param partia partia
     * @param prioritna true, ak si ju používateľ zvolil ako cieľ
     * @param tyzdnovOdZaciatku koľko týždňov už trénuje (rampa objemu)
     * @param deload true = tento týždeň sa objem zámerne zníži
     */
    fun cielSerii(
        partia: Partia,
        prioritna: Boolean,
        tyzdnovOdZaciatku: Int,
        deload: Boolean = false,
    ): Double {
        val strop = when {
            partia == Partia.LYTKA -> STROP_LYTKA
            prioritna -> STROP_PRIORITNA
            else -> STROP_BEZNA
        }
        val rampa = START_SERII + (tyzdnovOdZaciatku / 2) * PRIRASTOK_ZA_2_TYZDNE
        val ciel = minOf(rampa, strop)
        return if (deload) ciel * 0.6 else ciel
    }

    /**
     * Deficit = koľko sérií partii tento týždeň ešte chýba do cieľa.
     * Engine vyberá do silového bloku partiu s najväčším deficitom.
     */
    fun deficit(ciel: Double, odcvicene: Double): Double = (ciel - odcvicene).coerceAtLeast(0.0)
}
