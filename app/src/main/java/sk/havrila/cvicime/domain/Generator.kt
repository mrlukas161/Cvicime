package sk.havrila.cvicime.domain

import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.katalog.rozparsujPartie
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Meranie
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia
import sk.havrila.cvicime.data.model.Skupina

/** Všetko, čo generátor potrebuje vedieť. Zámerne čistý vstup - dá sa testovať bez databázy. */
data class VstupGeneratora(
    val dostupneMinuty: Int,
    val naradie: Set<Naradie>,
    /** rebrikId -> aktuálna úroveň používateľa */
    val urovne: Map<String, Int>,
    /** rebrikId zručností, ktoré má používateľ ako cieľ */
    val cieleZrucnosti: List<String>,
    val prioritnePartie: Set<Partia>,
    /** partia -> koľko sérií už tento týždeň odcvičil */
    val objemTyzdna: Map<Partia, Double>,
    /** partia -> epochDay poslednej záťaže */
    val poslednaZataz: Map<Partia, Long>,
    val dnesEpochDen: Long,
    val tyzdnovOdZaciatku: Int,
    val deload: Boolean,
    val rebriky: List<RebrikEntity>,
    val cvikyPodlaRebrika: Map<String, List<CvikEntity>>,
    /** značky cvikov, ktoré cvičiaci robiť nesmie (z jeho obmedzení) - viď Obmedzenie */
    val vylucitZnacky: Set<String> = emptySet(),
)

data class PlanPolozka(
    val cvik: CvikEntity,
    val rebrikId: String,
    val rebrikNazov: String,
    val serie: Int,
    /** cieľová hodnota jednej série - opakovania alebo sekundy */
    val ciel: Int,
    val pauzaS: Int,
    val cielRir: Int,
)

data class PlanBloku(val blok: Blok, val polozky: List<PlanPolozka>)

data class PlanTreningu(
    val bloky: List<PlanBloku>,
    val odhadMinut: Int,
    /** krátke vety, prečo je tréning zložený takto - appka ich vie ukázať */
    val zdovodnenie: List<String>,
    val deload: Boolean,
) {
    val vsetkyPolozky: List<Pair<Blok, PlanPolozka>>
        get() = bloky.flatMap { b -> b.polozky.map { b.blok to it } }
}

/**
 * Poskladá dnešný tréning.
 *
 * Poradie blokov je pevné a nie je kozmetické:
 * rozohriatie -> ZRUČNOSŤ (kým je človek čerstvý) -> SILA -> kondícia -> mobilita.
 * Zručnosť sa neškrtá nikdy, aj keď je času málo - stojí málo únavy a je to používateľov cieľ.
 */
object Generator {

    /** O koľko sa zvýhodní cvik zaťažujúci partiu, ktorú si používateľ zvolil ako cieľ. */
    private const val PRIORITNY_NASOBOK = 1.6

    fun zostav(v: VstupGeneratora): PlanTreningu {
        val zdovodnenie = mutableListOf<String>()
        val rozpocet = rozdelCas(v.dostupneMinuty)

        val rozohriatie = zostavRozohriatie(v, rozpocet.rozohriatieMin)
        val skill = zostavSkill(v, rozpocet.skillMin, zdovodnenie)
        val sila = zostavSilu(v, rozpocet.silaMin, zdovodnenie)
        val kondicia = if (rozpocet.kondiciaMin >= 4 && !v.deload) {
            zostavKondiciu(v, rozpocet.kondiciaMin)
        } else {
            emptyList()
        }
        val mobilita = zostavMobilitu(v, rozpocet.mobilitaMin)

        if (v.deload) {
            zdovodnenie += "Objem je dnes znížený. Posledné dva tréningy si končil na doraz — to je znak, " +
                "že telo nestíha regenerovať, nie dôvod na výčitky."
        }
        if (v.dostupneMinuty <= 15) {
            zdovodnenie += "Málo času: vypadol kondičný blok a ubrali sa série. Zručnosť ostáva — je lacná na únavu."
        }
        zdovodnenie += nesplnitelneCiele(v)

        val bloky = listOfNotNull(
            rozohriatie.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.ROZOHRIATIE, it) },
            skill.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.SKILL, it) },
            sila.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.SILA, it) },
            kondicia.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.KONDICIA, it) },
            mobilita.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.MOBILITA, it) },
        )

        return PlanTreningu(
            bloky = bloky,
            odhadMinut = bloky.sumOf { b -> b.polozky.sumOf { odhadMinutPolozky(it) } }.toInt(),
            zdovodnenie = zdovodnenie,
            deload = v.deload,
        )
    }

    // ---------------------------------------------------------------- rozpočet času

    private data class Rozpocet(
        val rozohriatieMin: Int,
        val skillMin: Int,
        val silaMin: Int,
        val kondiciaMin: Int,
        val mobilitaMin: Int,
    )

    private fun rozdelCas(minuty: Int): Rozpocet = when {
        minuty <= 15 -> Rozpocet(3, 4, 7, 0, 1)
        minuty <= 20 -> Rozpocet(3, 5, 10, 0, 2)
        minuty <= 30 -> Rozpocet(4, 6, 16, 0, 4)
        minuty <= 45 -> Rozpocet(5, 7, 24, 5, 4)
        else -> Rozpocet(6, 8, 30, 10, 6)
    }

    // ---------------------------------------------------------------- bloky

    private fun zostavRozohriatie(v: VstupGeneratora, minuty: Int): List<PlanPolozka> {
        if (minuty <= 0) return emptyList()
        val pocet = when {
            minuty <= 3 -> 3
            minuty <= 5 -> 4
            else -> 5
        }
        return zasobnikCvikov(v, Skupina.ROZOHRIATIE)
            .take(pocet)
            .map { (rebrik, cvik) ->
                PlanPolozka(
                    cvik = cvik,
                    rebrikId = rebrik.id,
                    rebrikNazov = rebrik.nazov,
                    serie = 1,
                    ciel = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) 30 else cvik.cielMin,
                    pauzaS = 0,
                    cielRir = 5,
                )
            }
    }

    private fun zostavSkill(
        v: VstupGeneratora,
        minuty: Int,
        zdovodnenie: MutableList<String>,
    ): List<PlanPolozka> {
        if (minuty <= 0) return emptyList()

        val zrucnosti = v.cieleZrucnosti
            .mapNotNull { id -> v.rebriky.firstOrNull { it.id == id } }
            .filter { jeDostupny(v, it) }
            .take(if (minuty >= 7) 2 else 1)

        if (zrucnosti.isEmpty()) return emptyList()

        val serieNaZrucnost = if (zrucnosti.size == 1) 4 else 3
        if (zdovodnenie.none { it.startsWith("Zručnosť") }) {
            zdovodnenie += "Zručnosť je hneď na začiatku zámerne — pohyb sa učí, kým si čerstvý, " +
                "a nikdy sa netrénuje do zlyhania."
        }

        return zrucnosti.mapNotNull { rebrik ->
            val cvik = aktualnyCvik(v, rebrik) ?: return@mapNotNull null
            PlanPolozka(
                cvik = cvik,
                rebrikId = rebrik.id,
                rebrikNazov = rebrik.nazov,
                serie = serieNaZrucnost,
                ciel = cielProCvik(cvik, konzervativne = true),
                pauzaS = Pravidla.PAUZA_SKILL_S,
                cielRir = Pravidla.CIEL_RIR_SKILL,
            )
        }
    }

    private fun zostavSilu(
        v: VstupGeneratora,
        minuty: Int,
        zdovodnenie: MutableList<String>,
    ): List<PlanPolozka> {
        if (minuty <= 0) return emptyList()

        val deficity = deficityPartii(v)
        val pocetCvikov = when {
            minuty <= 8 -> 2
            minuty <= 16 -> 3
            minuty <= 24 -> 4
            else -> 5
        }
        val serie = when {
            minuty <= 8 -> 2
            minuty <= 24 -> 3
            else -> 4
        }

        val kandidati = dostupneCviky(v, null)
            .filter { (rebrik, _) ->
                Skupina.z(rebrik.skupina) in setOf(Skupina.TLAK, Skupina.TAH, Skupina.NOHY, Skupina.STRED)
            }

        // Skóre cviku = koľko deficitu pokryje, s penalizáciou za nedávnu záťaž tých istých partií.
        val zoradene = kandidati
            .map { (rebrik, cvik) -> Triple(rebrik, cvik, skoreCviku(cvik, deficity, v)) }
            .filter { it.third > 0.0 }
            .sortedByDescending { it.third }

        val vybrane = mutableListOf<PlanPolozka>()
        val pouziteRebriky = mutableSetOf<String>()
        val pokryteSkupiny = mutableSetOf<Skupina>()

        fun pridaj(rebrik: RebrikEntity, cvik: CvikEntity) {
            vybrane += PlanPolozka(
                cvik = cvik,
                rebrikId = rebrik.id,
                rebrikNazov = rebrik.nazov,
                serie = if (v.deload) maxOf(1, serie - 1) else serie,
                ciel = cielProCvik(cvik, konzervativne = v.deload),
                pauzaS = Pravidla.PAUZA_SILA_S,
                cielRir = if (v.deload) 3 else Pravidla.CIEL_RIR_SILA,
            )
            pouziteRebriky += rebrik.id
            pokryteSkupiny += Skupina.z(rebrik.skupina)
        }

        // PRVÝ PRIECHOD: z každej pohybovej skupiny najlepší cvik.
        // Bez tohto by sa do tréningu dostali dva tlaky a žiadny ťah - a práve
        // nerovnováha medzi tlakom a ťahom je najčastejší zdroj problémov s ramenami.
        for ((rebrik, cvik, _) in zoradene) {
            if (vybrane.size >= pocetCvikov) break
            if (rebrik.id in pouziteRebriky) continue
            if (Skupina.z(rebrik.skupina) in pokryteSkupiny) continue
            pridaj(rebrik, cvik)
        }

        // DRUHÝ PRIECHOD: zvyšné miesta doplní najvyššie skórujúci cvik bez ohľadu na skupinu.
        for ((rebrik, cvik, _) in zoradene) {
            if (vybrane.size >= pocetCvikov) break
            if (rebrik.id in pouziteRebriky) continue
            pridaj(rebrik, cvik)
        }

        val najvacsiDeficit = deficity.maxByOrNull { it.value }
        if (najvacsiDeficit != null && najvacsiDeficit.value > 0.5) {
            zdovodnenie += "Dnes je dôraz na partiu „${najvacsiDeficit.key.nazov}\" — " +
                "tento týždeň jej chýba najviac sérií do cieľa."
        }
        zdovodnenie += "Pauzy 2 minúty. Kratšia pauza znie tvrdšie, ale podľa štúdií znamená menší prírastok."

        return vybrane
    }

    private fun zostavKondiciu(v: VstupGeneratora, minuty: Int): List<PlanPolozka> {
        val pocet = if (minuty >= 8) 4 else 3
        return dostupneCviky(v, null)
            .filter { (rebrik, _) -> Skupina.z(rebrik.skupina) in setOf(Skupina.NOHY, Skupina.STRED) }
            .shuffled()
            .distinctBy { it.first.id }
            .take(pocet)
            .map { (rebrik, cvik) ->
                PlanPolozka(
                    cvik = cvik,
                    rebrikId = rebrik.id,
                    rebrikNazov = rebrik.nazov,
                    serie = 2,
                    ciel = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) 30 else cvik.cielMin,
                    pauzaS = Pravidla.PAUZA_KONDICIA_S,
                    cielRir = Pravidla.CIEL_RIR_KONDICIA,
                )
            }
    }

    private fun zostavMobilitu(v: VstupGeneratora, minuty: Int): List<PlanPolozka> {
        if (minuty <= 0) return emptyList()
        val pocet = if (minuty >= 4) 3 else 2
        return zasobnikCvikov(v, Skupina.MOBILITA)
            .distinctBy { it.first.id }
            .take(pocet)
            .map { (rebrik, cvik) ->
                PlanPolozka(
                    cvik = cvik,
                    rebrikId = rebrik.id,
                    rebrikNazov = rebrik.nazov,
                    serie = 1,
                    ciel = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) 40 else cvik.cielMin,
                    pauzaS = Pravidla.PAUZA_MOBILITA_S,
                    cielRir = 5,
                )
            }
    }

    // ---------------------------------------------------------------- pomocné

    /** Dvojice (rebrík, cvik na aktuálnej úrovni) pre cviky, ktoré sa dajú spraviť s dostupným náradím. */
    private fun dostupneCviky(
        v: VstupGeneratora,
        skupina: Skupina?,
    ): List<Pair<RebrikEntity, CvikEntity>> =
        v.rebriky
            .asSequence()
            .filter { skupina == null || Skupina.z(it.skupina) == skupina }
            .filter { Skupina.z(it.skupina) != Skupina.DETSKY }
            .filter { skupina != null || !it.jeZrucnost } // zručnosti rieši skill blok
            .mapNotNull { rebrik -> aktualnyCvik(v, rebrik)?.let { rebrik to it } }
            .toList()

    /**
     * Rozohriatie a mobilita NIE SÚ progresné rebríky - je to zásobník cvikov.
     * Brať z nich len "aktuálny stupeň" by dalo jediný cvik na celú rozcvičku.
     */
    private fun zasobnikCvikov(
        v: VstupGeneratora,
        skupina: Skupina,
    ): List<Pair<RebrikEntity, CvikEntity>> =
        v.rebriky
            .filter { Skupina.z(it.skupina) == skupina }
            .flatMap { rebrik ->
                (v.cvikyPodlaRebrika[rebrik.id] ?: emptyList())
                    .filter { !it.jeDetsky && jePovoleny(v, it) }
                    .map { rebrik to it }
            }

    private fun jeDostupny(v: VstupGeneratora, rebrik: RebrikEntity): Boolean =
        aktualnyCvik(v, rebrik) != null

    /**
     * Cvik, ktorý má používateľ v danom rebríku robiť.
     *
     * Kľúčové: vyberá sa LEN z cvikov, ktoré sa dajú spraviť s dostupným náradím.
     * Ak presne ten stupeň náradie nemá (napr. L-sit na vyvýšených rukách potrebuje schod),
     * ustúpi sa na najbližší nižší dostupný - nikdy sa celý rebrík ticho nezahodí.
     */
    private fun aktualnyCvik(v: VstupGeneratora, rebrik: RebrikEntity): CvikEntity? {
        val cviky = v.cvikyPodlaRebrika[rebrik.id]?.filter { !it.jeDetsky } ?: return null
        val dostupne = cviky.filter { jePovoleny(v, it) }
        if (dostupne.isEmpty()) return null
        val uroven = v.urovne[rebrik.id] ?: 1
        dostupne.firstOrNull { it.uroven == uroven }?.let { return it }
        return dostupne.filter { it.uroven < uroven }.maxByOrNull { it.uroven }
            ?: dostupne.minByOrNull { it.uroven }
    }

    /**
     * Ciele, ktoré sa do tréningu nedostali, lebo na ne nie je náradie.
     * Appka to musí povedať nahlas - ticho zahodený cieľ je horší než žiadny.
     */
    private fun nesplnitelneCiele(v: VstupGeneratora): List<String> {
        val chyba = mutableListOf<String>()

        for (id in v.cieleZrucnosti) {
            val rebrik = v.rebriky.firstOrNull { it.id == id } ?: continue
            if (aktualnyCvik(v, rebrik) == null) {
                chyba += "„${rebrik.nazov}\" sa dnes nedá trénovať — žiadny jeho stupeň " +
                    "sa nezaobíde bez náradia, ktoré nemáš zapnuté."
            }
        }

        for (partia in v.prioritnePartie) {
            val jePreNu = dostupneCviky(v, null).any { (_, cvik) ->
                rozparsujPartie(cvik.partie).any { (kluc, vaha) -> kluc == partia.kluc && vaha >= 1.0 }
            }
            if (!jePreNu) {
                chyba += "Na partiu „${partia.nazov}\" nemáš zapnuté potrebné náradie, " +
                    "takže sa do tréningu nedostane."
            }
        }
        return chyba
    }

    /** Cvik sa dá zaradiť: je naň náradie a nemá značku, ktorú cvičiaci robiť nesmie. */
    private fun jePovoleny(v: VstupGeneratora, cvik: CvikEntity): Boolean {
        if (!maNaradie(v.naradie, cvik)) return false
        if (v.vylucitZnacky.isEmpty() || cvik.znacky.isBlank()) return true
        return cvik.znacky.split(",").none { it.trim() in v.vylucitZnacky }
    }

    private fun maNaradie(dostupne: Set<Naradie>, cvik: CvikEntity): Boolean {
        val potrebne = cvik.naradie
            .split(",")
            .mapNotNull { Naradie.z(it.trim()) }
            .filter { it != Naradie.ZIADNE }
        return potrebne.all { it in dostupne }
    }

    private fun deficityPartii(v: VstupGeneratora): Map<Partia, Double> =
        Partia.entries.associateWith { partia ->
            val ciel = Objem.cielSerii(
                partia = partia,
                prioritna = partia in v.prioritnePartie,
                tyzdnovOdZaciatku = v.tyzdnovOdZaciatku,
                deload = v.deload,
            )
            Objem.deficit(ciel, v.objemTyzdna[partia] ?: 0.0)
        }

    private fun skoreCviku(
        cvik: CvikEntity,
        deficity: Map<Partia, Double>,
        v: VstupGeneratora,
    ): Double {
        var skore = 0.0
        for ((kluc, vaha) in rozparsujPartie(cvik.partie)) {
            val partia = Partia.z(kluc) ?: continue
            val deficit = deficity[partia] ?: 0.0
            var prispevok = deficit * vaha
            // Bez tohto izolovaná partia (lýtka, predlaktia) nikdy nevyhrá nad viackĺbovým
            // cvikom, ktorý sčíta príspevky z dvoch partií - a zvolený cieľ by sa
            // do tréningu nedostal, hoci naň náradie je.
            if (partia in v.prioritnePartie) prispevok *= PRIORITNY_NASOBOK
            // Partia zaťažená včera alebo dnes sa penalizuje - potrebuje regeneráciu.
            val posledna = v.poslednaZataz[partia]
            if (posledna != null) {
                val dni = v.dnesEpochDen - posledna
                if (dni <= 0L) prispevok *= 0.2
                else if (dni == 1L) prispevok *= 0.6
            }
            skore += prispevok
        }
        return skore
    }

    private fun cielProCvik(cvik: CvikEntity, konzervativne: Boolean): Int =
        if (konzervativne) cvik.cielMin else ((cvik.cielMin + cvik.cielMax) / 2).coerceAtLeast(1)

    private fun odhadMinutPolozky(p: PlanPolozka): Double {
        val sekundNaSeriu = when (Meranie.z(p.cvik.meranie)) {
            Meranie.VYDRZ -> p.ciel.toDouble()
            Meranie.VZDIALENOST -> 30.0
            Meranie.OPAKOVANIA -> (p.ciel * 3.0).coerceAtLeast(20.0)
        }
        val celkom = p.serie * sekundNaSeriu + (p.serie - 1).coerceAtLeast(0) * p.pauzaS
        return celkom / 60.0
    }
}
