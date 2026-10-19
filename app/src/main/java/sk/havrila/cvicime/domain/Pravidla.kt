package sk.havrila.cvicime.domain

import sk.havrila.cvicime.data.denik.SeriaEntity
import sk.havrila.cvicime.data.katalog.CvikEntity

/**
 * Pravidlá progresie a autoregulácie. Sú vedome jednoduché - vstupom je len RIR
 * a splnené opakovania, lebo čokoľvek jemnejšie by u začiatočníka bolo meranie šumu.
 */
object Pravidla {

    /** Pauza medzi sériami v sekundách podľa typu bloku. */
    const val PAUZA_SILA_S = 120
    const val PAUZA_SKILL_S = 60
    const val PAUZA_KONDICIA_S = 20
    const val PAUZA_MOBILITA_S = 15

    /**
     * Cieľové RIR (koľko opakovaní má ostať v zálohe).
     * Silový blok ide blízko k zlyhaniu, ale nie do neho - meta-analýzy nenašli
     * výhodu zlyhania pre silu a pri hypertrofii je náskok nanajvýš triviálny.
     * Skill blok ide zámerne ďaleko od zlyhania - únava kazí učenie pohybu.
     */
    const val CIEL_RIR_SILA = 1
    const val CIEL_RIR_SKILL = 3
    const val CIEL_RIR_KONDICIA = 1

    /**
     * Posun o stupeň VYŠŠIE: vo všetkých sériách cviku dosiahnutý horný cieľ
     * a aspoň v jednej ostalo ešte niečo v zálohe (RIR >= 1).
     * Progresia pri vlastnej váhe je ťažšia variácia, nie donekonečna viac opakovaní.
     * Séria s príznakom tela (tlak, ťah, únik) postup vždy zastaví - telo ešte nie je pripravené.
     */
    fun maPostupitVyssie(serie: List<SeriaEntity>, cvik: CvikEntity): Boolean {
        if (serie.isEmpty()) return false
        if (serie.any { it.priznak }) return false
        val vsetkySplnene = serie.all { (it.skutocnaHodnota ?: 0) >= cvik.cielMax }
        val ostaloVZalohe = serie.any { (it.rir ?: 0) >= 1 }
        return vsetkySplnene && ostaloVZalohe
    }

    /**
     * Posun o stupeň NIŽŠIE: ani spodný cieľ sa nedosiahol v žiadnej sérii
     * a nič neostalo v zálohe. Cvik je jednoducho priťažký.
     */
    fun maKlesnutNizsie(serie: List<SeriaEntity>, cvik: CvikEntity): Boolean {
        if (serie.size < 2) return false
        val nikdyNesplnene = serie.none { (it.skutocnaHodnota ?: 0) >= cvik.cielMin }
        val vzdyNaDoraz = serie.all { (it.rir ?: 3) == 0 }
        return nikdyNesplnene && vzdyNaDoraz
    }

    /**
     * Deload: dva tréningy po sebe, v ktorých väčšina silových sérií skončila na doraz (RIR 0).
     * Nie je to trest - je to znak, že objem predbehol regeneráciu.
     */
    fun potrebujeDeload(serieDvochPoslednychTreningov: List<List<SeriaEntity>>): Boolean {
        if (serieDvochPoslednychTreningov.size < 2) return false
        return serieDvochPoslednychTreningov.all { serie ->
            val silove = serie.filter { it.blok == "sila" && it.rir != null }
            silove.size >= 3 && silove.count { it.rir == 0 } >= silove.size * 0.6
        }
    }

    /**
     * Kedy je retest zmysluplný. Kratší odstup meria hlavne dennú formu, nie zmenu.
     */
    const val RETEST_PO_DNOCH = 28

    /**
     * Sila kolíše počas dňa - retest mimo pôvodnej dennej doby môže ukázať "progres",
     * ktorý je len kolísanie. Tolerancia, v ktorej appka retest nekomentuje.
     */
    const val TOLERANCIA_DENNEJ_DOBY_MINUT = 150

    /**
     * Najmenšia zmena, ktorú má zmysel hlásiť ako progres.
     * Pri opakovaniach do zlyhania je bežné kolísanie medzi dňami rádovo jedno opakovanie.
     */
    fun jeRealnaZmena(stara: Int, nova: Int, meranie: String): Boolean {
        val rozdiel = nova - stara
        return when (meranie) {
            "vydrz" -> kotlin.math.abs(rozdiel) >= 3 // sekundy
            else -> kotlin.math.abs(rozdiel) >= 2 // opakovania
        }
    }
}
