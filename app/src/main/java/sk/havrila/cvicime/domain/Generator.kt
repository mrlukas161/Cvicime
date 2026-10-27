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
    /** štýl, meranie a pauzy zvolené pred tréningom („Ako dnes") */
    val nastavenie: NastavenieTreningu = NastavenieTreningu(),
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
    /** cvik na opakovania cvičený na odpočet („všetko na čas", kruh); null = podľa merania cviku */
    val naCasS: Int? = null,
    /** pár (superset) - položky s rovnakým číslom sa striedajú, viď [Rozvrh] */
    val par: Int? = null,
    /** oddych po sérii, keď ďalej ide druhý cvik páru alebo ďalšie stanovište kruhu */
    val prechodS: Int = 0,
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
        val styl = v.nastavenie.styl

        val rozohriatie = zostavRozohriatie(v, rozpocet.rozohriatieMin)
        val mobilita = zostavMobilitu(v, rozpocet.mobilitaMin)
        if (styl == StylTreningu.KRUH) return zostavKruhovy(v, rozpocet, rozohriatie, mobilita, zdovodnenie)

        val skill = zostavSkill(v, rozpocet.skillMin, zdovodnenie)
        var sila = zostavSilu(v, rozpocet.silaMin, zdovodnenie)
        if (styl == StylTreningu.PARY) sila = sparuj(sila, zdovodnenie)
        val kondicia = if (rozpocet.kondiciaMin >= 4 && !v.deload) {
            zostavKondiciu(v, rozpocet.kondiciaMin)
        } else {
            emptyList()
        }

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
        return dokonci(v, bloky, zdovodnenie)
    }

    /**
     * Spoločný záver: „všetko na čas", dĺžka páuz podľa voľby a poctivý odhad z rozvrhu.
     * Pauzy sa násobia AŽ tu, aby každý blok mohol rátať s pravidlami z [Pravidla].
     */
    private fun dokonci(v: VstupGeneratora, bloky: List<PlanBloku>, zdovodnenie: List<String>): PlanTreningu {
        val n = v.nastavenie
        val upravene = bloky.map { b ->
            b.copy(polozky = b.polozky.map { p ->
                val naCas = p.naCasS ?: if (n.vsetkoNaCas && Meranie.z(p.cvik.meranie) != Meranie.VYDRZ) n.pracaS else null
                p.copy(
                    naCasS = naCas,
                    pauzaS = upravPauzu(p.pauzaS, n.pauzy),
                    prechodS = upravPauzu(p.prechodS, n.pauzy),
                )
            })
        }
        val plan = vtesnajDoCasu(PlanTreningu(upravene, 0, zdovodnenie, v.deload), v.dostupneMinuty)
        return plan.copy(odhadMinut = Rozvrh.odhadMinut(Rozvrh.kroky(plan)))
    }

    /**
     * Zvolený čas je sľub. Kým poctivý odhad presahuje čas o viac než 2 minúty, uberá sa -
     * v poradí, ktoré najmenej bolí: kondícia, série sily (najviac na 2), mobilita, posledný
     * silový cvik (aspoň 2 ostávajú), série zručnosti (najviac na 2), stanovište kruhu.
     * Pauzy sa neskracujú - tie si volí používateľ.
     */
    private fun vtesnajDoCasu(plan: PlanTreningu, minuty: Int): PlanTreningu {
        var p = plan
        fun odhad(x: PlanTreningu) = Rozvrh.odhadMinut(Rozvrh.kroky(x))
        fun uprav(blok: Blok, zmena: (List<PlanPolozka>) -> List<PlanPolozka>?): PlanTreningu? {
            val b = p.bloky.firstOrNull { it.blok == blok } ?: return null
            val nove = zmena(b.polozky) ?: return null
            return p.copy(bloky = p.bloky.mapNotNull { if (it.blok == blok) b.copy(polozky = nove).takeIf { nove.isNotEmpty() } else it })
        }
        val kroky: List<() -> PlanTreningu?> = listOf(
            { uprav(Blok.KONDICIA) { emptyList() } },
            {
                uprav(Blok.SILA) { pol ->
                    val i = pol.indices.filter { pol[it].serie > 2 }.maxByOrNull { pol[it].serie } ?: return@uprav null
                    // pár uberá spolu, aby sa striedanie nerozbilo
                    val par = pol[i].par
                    pol.mapIndexed { j, x -> if (j == i || (par != null && x.par == par && x.serie > 2)) x.copy(serie = x.serie - 1) else x }
                }
            },
            { uprav(Blok.MOBILITA) { pol -> if (pol.size > 1) pol.dropLast(1) else null } },
            {
                uprav(Blok.SILA) { pol ->
                    if (pol.size <= 2) return@uprav null
                    val posledny = pol.last()
                    // druhý z páru ostane samostatný
                    pol.dropLast(1).map { if (posledny.par != null && it.par == posledny.par) it.copy(par = null, prechodS = 0, pauzaS = Pravidla.PAUZA_SILA_S) else it }
                }
            },
            { uprav(Blok.SKILL) { pol -> pol.indexOfFirst { it.serie > 2 }.takeIf { it >= 0 }?.let { i -> pol.mapIndexed { j, x -> if (j == i) x.copy(serie = x.serie - 1) else x } } } },
            { uprav(Blok.KRUH) { pol -> if (pol.size > 5) pol.dropLast(1) else null } },
            { uprav(Blok.KRUH) { pol -> if (pol.first().serie > 1) pol.map { it.copy(serie = it.serie - 1) } else null } },
        )
        var bezpecnost = 0
        while (odhad(p) > minuty + 2 && bezpecnost++ < 60) {
            p = kroky.firstNotNullOfOrNull { it() } ?: break
        }
        return p
    }

    /** Pauza podľa voľby, zaokrúhlená na 5 s. Nulová (rozohriatie) ostáva nulová. */
    private fun upravPauzu(s: Int, dlzka: DlzkaPauz): Int {
        if (s <= 0 || dlzka == DlzkaPauz.AUTOMATICKY) return s
        return (Math.round(s * dlzka.nasobok / 5.0) * 5).toInt().coerceAtLeast(5)
    }

    // ---------------------------------------------------------------- páry a kruh

    /**
     * Páry (supersety): cviky BEZ spoločnej hlavnej partie sa striedajú - kým jeden sval
     * oddychuje, druhý pracuje. Prednosť má tlak + ťah. Dva cviky na ten istý sval sa nepárujú:
     * to by objem znižovalo (Zhang 2025). Kto nemá dvojicu, ide samostatne s bežnou pauzou.
     */
    internal fun sparuj(sila: List<PlanPolozka>, zdovodnenie: MutableList<String>): List<PlanPolozka> {
        val volne = sila.indices.toMutableList()
        val dvojice = mutableListOf<Pair<Int, Int>>()
        while (volne.size >= 2) {
            val a = volne.removeAt(0)
            val ha = hlavnePartie(sila[a])
            val kandidati = volne.filter { b -> hlavnePartie(sila[b]).none { it in ha } }
            // tlak ↔ ťah je najlepší pár (ramená sa navzájom vyvažujú), potom čokoľvek bez prekrytia
            val b = kandidati.firstOrNull { jeTlakTah(sila[a], sila[it]) }
                ?: kandidati.firstOrNull()
                ?: continue
            volne.remove(b)
            dvojice += a to b
        }
        if (dvojice.isEmpty()) return sila
        zdovodnenie.removeAll { it.startsWith("Pauzy") }
        zdovodnenie += "Páry: dva cviky na iné svaly sa striedajú, pauza až po oboch. " +
            "Tréning je kratší a podľa meta-analýzy (Zhang 2025) dáva rovnakú silu aj svaly."
        val vysledok = mutableListOf<PlanPolozka>()
        val pouzite = mutableSetOf<Int>()
        dvojice.forEachIndexed { cislo, (a, b) ->
            listOf(a, b).forEach { i ->
                vysledok += sila[i].copy(par = cislo + 1, prechodS = Pravidla.PRECHOD_PAR_S, pauzaS = Pravidla.PAUZA_PAR_S)
                pouzite += i
            }
        }
        sila.indices.filter { it !in pouzite }.forEach { vysledok += sila[it] }
        return vysledok
    }

    private fun hlavnePartie(p: PlanPolozka): Set<String> =
        rozparsujPartie(p.cvik.partie).filter { it.second >= 1.0 }.map { it.first }.toSet()

    private fun jeTlakTah(a: PlanPolozka, b: PlanPolozka): Boolean {
        val tlak = setOf("hrudnik", "triceps", "ramena")
        val tah = setOf("chrbat", "biceps")
        val ha = hlavnePartie(a)
        val hb = hlavnePartie(b)
        return (ha.any { it in tlak } && hb.any { it in tah }) || (ha.any { it in tah } && hb.any { it in tlak })
    }

    /** Rebríky, ktoré do kruhu nepatria: technika alebo úchop sa v únave kazí (vis, predlaktia, kliky do stojky). */
    private val MIMO_KRUHU = setOf("vis", "predlaktia", "kliky-do-stojky", "panvove-dno")

    /**
     * Intenzívny kruh: 6-10 jednoduchých stanovíšť na čas, 2-3 kolá. Zručnosti nikdy v kruhu -
     * únava zhoršuje učenie pohybu (Branscheidt 2019); ak má človek cieľ zručnosti a čas,
     * dostane krátky skill blok pred kruhom. Do postupov v rebríkoch sa kruh neráta.
     */
    private fun zostavKruhovy(
        v: VstupGeneratora,
        rozpocet: Rozpocet,
        rozohriatie: List<PlanPolozka>,
        mobilita: List<PlanPolozka>,
        zdovodnenie: MutableList<String>,
    ): PlanTreningu {
        val skill = if (v.dostupneMinuty >= 20) zostavSkill(v, minOf(rozpocet.skillMin, 5), zdovodnenie)
            .take(1).map { it.copy(serie = 3) } else emptyList()
        val praca = v.nastavenie.pracaS
        val naKruhMin = (v.dostupneMinuty - rozpocet.rozohriatieMin - rozpocet.mobilitaMin -
            (if (skill.isNotEmpty()) 5 else 0)).coerceAtLeast(5)
        val stanovisko = praca + Pravidla.PRECHOD_KRUH_S
        // počet stanovíšť a kôl tak, aby sa zmestili do času: najprv 2 kolá, pri dlhšom tréningu 3
        var kola = if (naKruhMin >= 18) 3 else 2
        var pocet = ((naKruhMin * 60 / kola - Pravidla.PAUZA_KOLO_S) / stanovisko).coerceIn(6, 10)
        if (kola == 3 && pocet < 7) { kola = 2; pocet = ((naKruhMin * 60 / kola - Pravidla.PAUZA_KOLO_S) / stanovisko).coerceIn(6, 10) }

        val nahoda = kotlin.random.Random(v.dnesEpochDen)
        val podlaSkupiny = dostupneCviky(v, null)
            .filter { (r, _) -> r.id !in MIMO_KRUHU }
            .filter { (r, _) -> Skupina.z(r.skupina) in setOf(Skupina.TLAK, Skupina.TAH, Skupina.NOHY, Skupina.STRED) }
            .shuffled(nahoda)
            .groupBy { Skupina.z(it.first.skupina) }
        // na tep: 1-2 kardio cviky z rozohriatia (vysoké kolená a pod.), len tie na výdrž
        val kardio = zasobnikCvikov(v, Skupina.ROZOHRIATIE)
            .filter { Meranie.z(it.second.meranie) == Meranie.VYDRZ }
            .shuffled(nahoda)
            .take(if (pocet >= 8) 2 else 1)
        // striedanie partií: nohy, tlak, stred, ťah... aby jedna partia neodpočívala len cez prechod
        val poradie = listOf(Skupina.NOHY, Skupina.TLAK, Skupina.STRED, Skupina.TAH)
        val vybrane = mutableListOf<Pair<RebrikEntity, CvikEntity>>()
        val fronty = poradie.associateWith { (podlaSkupiny[it] ?: emptyList()).toMutableList() }
        var kardioIdx = 0
        while (vybrane.size < pocet) {
            var pridane = false
            for (sk in poradie) {
                if (vybrane.size >= pocet) break
                val f = fronty[sk] ?: continue
                if (f.isNotEmpty()) { vybrane += f.removeAt(0); pridane = true }
            }
            if (kardioIdx < kardio.size && vybrane.size < pocet) { vybrane += kardio[kardioIdx++]; pridane = true }
            if (!pridane) break
        }
        val kruh = vybrane.map { (rebrik, cvik) ->
            PlanPolozka(
                cvik = cvik,
                rebrikId = rebrik.id,
                rebrikNazov = rebrik.nazov,
                serie = kola,
                ciel = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) praca else cvik.cielMin,
                pauzaS = Pravidla.PAUZA_KOLO_S,
                cielRir = 2,
                naCasS = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) null else praca,
                prechodS = Pravidla.PRECHOD_KRUH_S,
            )
        }
        zdovodnenie += "Intenzívny kruh: ${kruh.size} stanovíšť × $kola kolá, $praca s práca a ${Pravidla.PRECHOD_KRUH_S} s prechod. " +
            "Zdvihne tep a zacvičí (meta-analýzy: VO₂max +6 %), ale na maximálnu silu a zručnosti je lepšia Klasika či Páry."
        if (v.vylucitZnacky.isNotEmpty()) zdovodnenie += "Cviky, ktoré tvoje obmedzenia vylučujú, sa do kruhu nedostali."
        val bloky = listOfNotNull(
            rozohriatie.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.ROZOHRIATIE, it) },
            skill.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.SKILL, it) },
            kruh.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.KRUH, it) },
            mobilita.takeIf { it.isNotEmpty() }?.let { PlanBloku(Blok.MOBILITA, it) },
        )
        return dokonci(v, bloky, zdovodnenie)
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
        minuty <= 10 -> Rozpocet(2, 3, 4, 0, 1)
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
        // Každý deň iný výrez zásobníka - inak by to boli navždy prvé cviky katalógu.
        return rotuj(zasobnikCvikov(v, Skupina.ROZOHRIATIE), v.dnesEpochDen)
            .take(pocet)
            .map { (rebrik, cvik) ->
                PlanPolozka(
                    cvik = cvik,
                    rebrikId = rebrik.id,
                    rebrikNazov = rebrik.nazov,
                    serie = 1,
                    ciel = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) 30 else cvik.cielMin,
                    // krátke „priprav sa" - prehrávač počas neho predstaví ďalší cvik
                    pauzaS = Pravidla.PRIPRAV_SA_S,
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
        // Páry ušetria približne tretinu času - do toho istého rozpočtu sa zmestí cvik navyše.
        val navyse = if (v.nastavenie.styl == StylTreningu.PARY && minuty > 8) 1 else 0
        val pocetCvikov = when {
            minuty <= 8 -> 2
            minuty <= 16 -> 3
            minuty <= 24 -> 4
            else -> 5
        } + navyse
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
                pauzaS = if (cvik.cielMax <= 5) Pravidla.PAUZA_SILA_TAZKA_S else Pravidla.PAUZA_SILA_S,
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
        zdovodnenie += "Pauzy 90 s (pri najťažších stupňoch 2,5 min). Nad 90 s sa pre rast svalu " +
            "podľa meta-analýzy nič merateľné nepridá — dlhšia pauza pomáha hlavne maximálnej sile."

        return vybrane
    }

    private fun zostavKondiciu(v: VstupGeneratora, minuty: Int): List<PlanPolozka> {
        val pocet = if (minuty >= 8) 4 else 3
        return dostupneCviky(v, null)
            .filter { (rebrik, _) -> Skupina.z(rebrik.skupina) in setOf(Skupina.NOHY, Skupina.STRED) }
            // deterministicky podľa dňa - plán na Domove a v prehrávači musí byť ten istý
            .shuffled(kotlin.random.Random(v.dnesEpochDen))
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
        // Rebríky mobility sa striedajú po dňoch; z každého jeden cvik (rotácia aj v rámci rebríka).
        val podlaRebrika = zasobnikCvikov(v, Skupina.MOBILITA).groupBy { it.first.id }.values.toList()
        if (podlaRebrika.isEmpty()) return emptyList()
        return rotuj(podlaRebrika, v.dnesEpochDen)
            .map { cviky -> cviky[Math.floorMod(v.dnesEpochDen / 7, cviky.size.toLong()).toInt()] }
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

    /** Zoznam od posunu podľa dňa (deterministicky) - rozohriatie a mobilita sa tak striedajú. */
    private fun <T> rotuj(zoznam: List<T>, den: Long): List<T> {
        if (zoznam.isEmpty()) return zoznam
        val posun = Math.floorMod(den, zoznam.size.toLong()).toInt()
        return zoznam.drop(posun) + zoznam.take(posun)
    }
}
