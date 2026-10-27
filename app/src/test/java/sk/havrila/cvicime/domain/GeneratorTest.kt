package sk.havrila.cvicime.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia

/**
 * Generátor je najdrahšia časť appky na ladenie, lebo jeho chyby nie sú vidieť -
 * tréning "nejaký" vyjde vždy. Preto sa testujú pravidlá, nie výstup.
 */
class GeneratorTest {

    private fun cvik(
        rebrik: String,
        uroven: Int,
        partie: String,
        naradie: String = "ziadne",
        meranie: String = "opakovania",
    ) = CvikEntity(
        id = "$rebrik-$uroven",
        rebrikId = rebrik,
        uroven = uroven,
        nazov = "$rebrik $uroven",
        meranie = meranie,
        cielMin = 8,
        cielMax = 12,
        naradie = naradie,
        partie = partie,
        cue = "pokyn",
        chyby = "chyba",
    )

    private fun rebrik(id: String, skupina: String, zrucnost: Boolean = false) =
        RebrikEntity(id, id, "", skupina, zrucnost, 10)

    private val rebriky = listOf(
        rebrik("rozcvicka", "rozohriatie"),
        rebrik("kliky", "tlak"),
        rebrik("pritahy", "tah"),
        rebrik("drep", "nohy"),
        rebrik("lytka", "nohy"),
        rebrik("plank", "stred"),
        rebrik("lsit", "zrucnost", zrucnost = true),
        rebrik("stojka", "zrucnost", zrucnost = true),
        rebrik("ramena-mobilita", "mobilita"),
    )

    private val cviky = mapOf(
        // Rozohriatie je zásobník, nie progresia - v jednom rebríku ich je viac.
        "rozcvicka" to listOf(
            cvik("rozcvicka", 1, ""),
            cvik("rozcvicka", 2, ""),
            cvik("rozcvicka", 3, ""),
            cvik("rozcvicka", 4, ""),
            cvik("rozcvicka", 5, ""),
        ),
        "kliky" to listOf(cvik("kliky", 1, "hrudnik:1.0,triceps:0.5")),
        "pritahy" to listOf(cvik("pritahy", 1, "chrbat:1.0,biceps:0.5", naradie = "hrazda")),
        "drep" to listOf(cvik("drep", 1, "stehna:1.0,zadok:0.5")),
        "lytka" to listOf(cvik("lytka", 1, "lytka:1.0", naradie = "schod")),
        "plank" to listOf(cvik("plank", 1, "stred:1.0", meranie = "vydrz")),
        // Stupeň 2 vyžaduje schod - používa sa na test ústupu na dostupný stupeň.
        "lsit" to listOf(
            cvik("lsit", 1, "stred:1.0", meranie = "vydrz"),
            cvik("lsit", 2, "stred:1.0", meranie = "vydrz", naradie = "schod"),
            cvik("lsit", 3, "stred:1.0", meranie = "vydrz"),
        ),
        "stojka" to listOf(cvik("stojka", 1, "ramena:1.0", meranie = "vydrz", naradie = "stena")),
        "ramena-mobilita" to listOf(cvik("ramena-mobilita", 1, "", meranie = "vydrz")),
    )

    private fun vstup(
        minuty: Int = 30,
        naradie: Set<Naradie> = setOf(Naradie.ZIADNE, Naradie.HRAZDA, Naradie.SCHOD, Naradie.STENA),
        ciele: List<String> = listOf("lsit", "stojka"),
        prioritne: Set<Partia> = setOf(Partia.LYTKA),
        objem: Map<Partia, Double> = emptyMap(),
        poslednaZataz: Map<Partia, Long> = emptyMap(),
        deload: Boolean = false,
        nastavenie: NastavenieTreningu = NastavenieTreningu(),
    ) = VstupGeneratora(
        dostupneMinuty = minuty,
        naradie = naradie,
        urovne = emptyMap(),
        cieleZrucnosti = ciele,
        prioritnePartie = prioritne,
        objemTyzdna = objem,
        poslednaZataz = poslednaZataz,
        dnesEpochDen = 20000,
        tyzdnovOdZaciatku = 0,
        deload = deload,
        rebriky = rebriky,
        cvikyPodlaRebrika = cviky,
        nastavenie = nastavenie,
    )

    @Test
    fun `bloky su vzdy v pevnom poradi`() {
        val plan = Generator.zostav(vstup(minuty = 45))
        val poradie = plan.bloky.map { it.blok.poradie }
        assertEquals(poradie.sorted(), poradie)
    }

    /** Zručnosť sa nesmie obetovať ani pri najkratšom tréningu - je to používateľov cieľ. */
    @Test
    fun `zrucnost ostava aj pri pätnastich minutach`() {
        val plan = Generator.zostav(vstup(minuty = 15))
        assertTrue(plan.bloky.any { it.blok == Blok.SKILL })
    }

    @Test
    fun `kondicny blok vypadne pri kratkom case`() {
        assertFalse(Generator.zostav(vstup(minuty = 15)).bloky.any { it.blok == Blok.KONDICIA })
        assertTrue(Generator.zostav(vstup(minuty = 45)).bloky.any { it.blok == Blok.KONDICIA })
    }

    @Test
    fun `silovy blok ma v klasike pauzu 90 sekund`() {
        val sila = Generator.zostav(vstup()).bloky.first { it.blok == Blok.SILA }
        assertTrue(sila.polozky.all { it.pauzaS == Pravidla.PAUZA_SILA_S })
    }

    /** Skill blok nesmie ísť blízko k zlyhaniu - únava kazí učenie pohybu. */
    @Test
    fun `skill blok ma velku rezervu`() {
        val skill = Generator.zostav(vstup()).bloky.first { it.blok == Blok.SKILL }
        assertTrue(skill.polozky.all { it.cielRir >= Pravidla.CIEL_RIR_SKILL })
    }

    @Test
    fun `cvik vyzadujuci nedostupne naradie sa nikdy nepouzije`() {
        val plan = Generator.zostav(vstup(naradie = setOf(Naradie.ZIADNE)))
        val pouzite = plan.vsetkyPolozky.map { it.second.cvik.id }
        assertFalse("príťahy vyžadujú hrazdu", pouzite.contains("pritahy-1"))
        assertFalse("lýtka vyžadujú schod", pouzite.contains("lytka-1"))
        assertFalse("stojka vyžaduje stenu", pouzite.contains("stojka-1"))
    }

    @Test
    fun `partia s vacsim deficitom ma prednost`() {
        // Hrudník je už tento týždeň odcvičený, chrbát nie -> ťah má vyhrať.
        val plan = Generator.zostav(
            vstup(objem = mapOf(Partia.HRUDNIK to 12.0, Partia.TRICEPS to 12.0))
        )
        val silove = plan.bloky.first { it.blok == Blok.SILA }.polozky.map { it.rebrikId }
        assertTrue("v silovom bloku mal byť ťah, bolo: $silove", silove.contains("pritahy"))
    }

    @Test
    fun `deload znizi serie a rezervu`() {
        val bez = Generator.zostav(vstup()).bloky.first { it.blok == Blok.SILA }
        val s = Generator.zostav(vstup(deload = true)).bloky.first { it.blok == Blok.SILA }
        assertTrue(s.polozky.first().serie < bez.polozky.first().serie)
        assertTrue(s.polozky.first().cielRir > bez.polozky.first().cielRir)
        assertTrue(s.polozky.first().cielRir >= 3)
    }

    @Test
    fun `deload nepridava kondicny blok`() {
        val plan = Generator.zostav(vstup(minuty = 45, deload = true))
        assertFalse(plan.bloky.any { it.blok == Blok.KONDICIA })
    }

    @Test
    fun `bez ciela zrucnosti sa skill blok nezaradi`() {
        val plan = Generator.zostav(vstup(ciele = emptyList()))
        assertFalse(plan.bloky.any { it.blok == Blok.SKILL })
        assertTrue("aj tak musí vzniknúť tréning", plan.bloky.isNotEmpty())
    }

    @Test
    fun `rovnaky rebrik sa v jednom treningu neopakuje`() {
        val silove = Generator.zostav(vstup(minuty = 60))
            .bloky.first { it.blok == Blok.SILA }.polozky.map { it.rebrikId }
        assertEquals(silove.distinct().size, silove.size)
    }

    // ---------------------------------------------------------------- regresie z testu na emulátore 04.08.2026

    /**
     * Chyba nájdená na emulátore: zvolená zručnosť z tréningu ticho zmizla,
     * lebo práve ten stupeň vyžadoval náradie, ktoré používateľ nemal.
     */
    @Test
    fun `zrucnost ustupi na dostupny stupen namiesto toho aby zmizla`() {
        // Používateľ je na stupni 2, ktorý vyžaduje schod - a ten práve nemá.
        val v = vstup(
            ciele = listOf("lsit"),
            naradie = setOf(Naradie.ZIADNE, Naradie.HRAZDA, Naradie.STENA),
        ).copy(urovne = mapOf("lsit" to 2))
        val plan = Generator.zostav(v)
        val skill = plan.bloky.firstOrNull { it.blok == Blok.SKILL }
        assertTrue("skill blok úplne zmizol", skill != null)
        assertEquals("mal ustúpiť na stupeň 1", 1, skill!!.polozky.first().cvik.uroven)
    }

    /**
     * Chyba nájdená na emulátore: lýtka boli zvolené ako cieľ, náradie na ne bolo,
     * a aj tak sa do tréningu nedostali - viackĺbový cvik sčíta príspevky z dvoch partií
     * a izolovaný jednopartiový nikdy nevyhrá.
     */
    @Test
    fun `zvolena partia sa naozaj dostane do treningu`() {
        val plan = Generator.zostav(vstup(minuty = 30, prioritne = setOf(Partia.LYTKA)))
        val zatazuje = plan.bloky
            .filter { it.blok == Blok.SILA }
            .flatMap { it.polozky }
            .any { it.cvik.partie.contains("lytka") }
        val cviky = plan.bloky.first { it.blok == Blok.SILA }.polozky.map { it.cvik.nazov }
        assertTrue("lýtka sú cieľ, ale v silovom bloku nie sú: $cviky", zatazuje)
    }

    /**
     * Chyba nájdená na emulátore: po zvýhodnení prioritnej partie sa do silového bloku
     * dostali dva tlaky a žiadny ťah. Nerovnováha tlak/ťah je najčastejší zdroj
     * problémov s ramenami - engine ju nesmie vyrobiť sám.
     */
    @Test
    fun `silovy blok pokryje tlak aj tah skor nez zdvojí skupinu`() {
        val plan = Generator.zostav(vstup(minuty = 30, prioritne = setOf(Partia.LYTKA)))
        val skupiny = plan.bloky.first { it.blok == Blok.SILA }.polozky
            .map { p -> rebriky.first { it.id == p.rebrikId }.skupina }
        assertTrue("chýba tlak: $skupiny", skupiny.contains("tlak"))
        assertTrue("chýba ťah: $skupiny", skupiny.contains("tah"))
    }

    /** Cieľ, na ktorý nie je náradie, sa nesmie zamlčať. */
    @Test
    fun `nesplnitelny ciel sa oznami v zdovodneni`() {
        val plan = Generator.zostav(
            vstup(naradie = setOf(Naradie.ZIADNE), ciele = listOf("stojka"), prioritne = setOf(Partia.LYTKA))
        )
        val text = plan.zdovodnenie.joinToString(" ")
        assertTrue("nepovedal, že stojka sa nedá: $text", text.contains("Stojka") || text.contains("stojka"))
        assertTrue("nepovedal, že lýtka sa nedajú: $text", text.contains("Lýtka") || text.contains("lýtka"))
    }

    /**
     * Chyba nájdená na emulátore: voľba prioritnej partie dvíhala len strop objemu,
     * na ktorý sa rampa v prvých týždňoch ani nedostane - takže nerobila nič.
     */
    @Test
    fun `prioritna partia ma vyssi ciel objemu hned od zaciatku`() {
        val prioritna = Objem.cielSerii(Partia.LYTKA, prioritna = true, tyzdnovOdZaciatku = 0)
        val bezna = Objem.cielSerii(Partia.LYTKA, prioritna = false, tyzdnovOdZaciatku = 0)
        assertTrue("priorita v prvom týždni nemá žiadny efekt", prioritna > bezna)
    }

    /**
     * Chyba nájdená na emulátore: rozcvička dala jediný cvik, lebo engine bral
     * z rebríka len aktuálny stupeň. Rozohriatie ale nie je progresia, je to zásobník.
     */
    @Test
    fun `rozohriatie berie viac cvikov z jedneho rebrika`() {
        val rozohriatie = Generator.zostav(vstup(minuty = 30))
            .bloky.first { it.blok == Blok.ROZOHRIATIE }
        assertTrue("rozcvička má len ${rozohriatie.polozky.size} cvik(y)", rozohriatie.polozky.size >= 3)
    }

    @Test
    fun `odhad casu je v rozumnom pomere k dostupnemu`() {
        for (m in listOf(15, 30, 45)) {
            val plan = Generator.zostav(vstup(minuty = m))
            assertTrue(
                "pri $m min vyšiel odhad ${plan.odhadMinut} min",
                plan.odhadMinut in 1..(m + 12),
            )
        }
    }

    /**
     * Obmedzenie (napr. šetrenie panvového dna) musí cvik so zakázanou značkou spoľahlivo vylúčiť,
     * ale rebrík kvôli tomu nezahodiť - engine ustúpi na stupeň bez značky, ako pri chýbajúcom náradí.
     */
    @Test
    fun `obmedzenie vylúči cviky so zakazanou znackou a ustupi na povoleny stupen`() {
        val sZnackami = cviky + mapOf(
            "drep" to listOf(
                cvik("drep", 1, "stehna:1.0,zadok:0.5"),
                cvik("drep", 2, "stehna:1.0,zadok:0.5").copy(znacky = "skok"),
            ),
            "plank" to listOf(cvik("plank", 1, "stred:1.0", meranie = "vydrz").copy(znacky = "tlak_brucho")),
        )
        val v = vstup(minuty = 45).copy(
            cvikyPodlaRebrika = sZnackami,
            urovne = mapOf("drep" to 2),
            vylucitZnacky = setOf("skok", "tlak_brucho"),
        )
        val vsetky = Generator.zostav(v).bloky.flatMap { it.polozky }.map { it.cvik }
        assertTrue("prešiel cvik so zakázanou značkou", vsetky.none { it.znacky.isNotBlank() })
        assertTrue("drep mal ustúpiť na stupeň 1", vsetky.any { it.id == "drep-1" })
        assertFalse(vsetky.any { it.rebrikId == "plank" })
    }

    @Test
    fun `bez obmedzeni sa znacky ignoruju`() {
        val sZnackami = cviky + mapOf(
            "drep" to listOf(cvik("drep", 1, "stehna:1.0,zadok:0.5").copy(znacky = "skok")),
        )
        val vsetky = Generator.zostav(vstup(minuty = 45).copy(cvikyPodlaRebrika = sZnackami))
            .bloky.flatMap { it.polozky }.map { it.cvik.id }
        assertTrue(vsetky.contains("drep-1"))
    }

    // ---------------------------------------------------------------- 0.8: štýly a rozvrh

    @Test
    fun `plan je pre rovnaky den a vstup vzdy rovnaky`() {
        // Domov ukazuje plán a prehrávač si ho skladá znova - musia sa zhodovať.
        val a = Generator.zostav(vstup(minuty = 45))
        val b = Generator.zostav(vstup(minuty = 45))
        assertEquals(a.vsetkyPolozky.map { it.second.cvik.id }, b.vsetkyPolozky.map { it.second.cvik.id })
    }

    @Test
    fun `pary nikdy nespoja dva cviky na tu istu hlavnu partiu`() {
        val plan = Generator.zostav(vstup(minuty = 45, nastavenie = NastavenieTreningu(styl = StylTreningu.PARY)))
        val sila = plan.bloky.first { it.blok == Blok.SILA }.polozky
        val pary = sila.filter { it.par != null }.groupBy { it.par }
        assertTrue("žiadny pár", pary.isNotEmpty())
        for ((_, clenovia) in pary) {
            assertEquals(2, clenovia.size)
            val hlavne = clenovia.map { p ->
                p.cvik.partie.split(",").filter { it.endsWith(":1.0") }.map { it.substringBefore(":") }.toSet()
            }
            assertTrue("pár ${clenovia.map { it.cvik.id }} zdieľa partiu", hlavne[0].intersect(hlavne[1]).isEmpty())
        }
    }

    @Test
    fun `pary sa v rozvrhu striedaju a pauza je az po druhom cviku`() {
        val plan = Generator.zostav(vstup(minuty = 45, nastavenie = NastavenieTreningu(styl = StylTreningu.PARY)))
        val sila = plan.bloky.first { it.blok == Blok.SILA }
        val kroky = Rozvrh.kroky(sila)
        val par = sila.polozky.first { it.par != null }.par
        val krokyParu = kroky.filter { it.polozka.par == par }
        val prve = krokyParu[0].polozka.cvik.id
        val druhe = krokyParu[1].polozka.cvik.id
        assertTrue(prve != druhe)
        assertEquals(prve, krokyParu[2].polozka.cvik.id)
        assertEquals(Pravidla.PRECHOD_PAR_S, krokyParu[0].pauzaPoS)
        assertEquals(Pravidla.PAUZA_PAR_S, krokyParu[1].pauzaPoS)
    }

    @Test
    fun `pary su kratsie nez klasika pri rovnakom pocte cvikov`() {
        val polozky = listOf(
            PlanPolozka(cviky.getValue("kliky")[0], "kliky", "kliky", 3, 10, 90, 1),
            PlanPolozka(cviky.getValue("pritahy")[0], "pritahy", "pritahy", 3, 10, 90, 1),
        )
        val klasika = PlanTreningu(listOf(PlanBloku(Blok.SILA, polozky)), 0, emptyList(), false)
        val pary = PlanTreningu(
            listOf(PlanBloku(Blok.SILA, Generator.sparuj(polozky, mutableListOf()))), 0, emptyList(), false,
        )
        assertTrue(Rozvrh.odhadMinut(Rozvrh.kroky(pary)) < Rozvrh.odhadMinut(Rozvrh.kroky(klasika)))
    }

    @Test
    fun `kruh neobsahuje zrucnost ani silovy blok a ide na cas`() {
        val plan = Generator.zostav(vstup(minuty = 30, nastavenie = NastavenieTreningu(styl = StylTreningu.KRUH)))
        val kruh = plan.bloky.first { it.blok == Blok.KRUH }
        assertFalse(plan.bloky.any { it.blok == Blok.SILA || it.blok == Blok.KONDICIA })
        assertTrue(kruh.polozky.none { it.rebrikId in setOf("lsit", "stojka") })
        assertTrue(kruh.polozky.all { it.naCasS != null || it.cvik.meranie == "vydrz" })
        // každé kolo končí dlhšou pauzou, inak len prechod
        val kroky = Rozvrh.kroky(kruh)
        val vKole = kruh.polozky.size
        assertEquals(Pravidla.PRECHOD_KRUH_S, kroky.first().pauzaPoS)
        assertEquals(Pravidla.PAUZA_KOLO_S, kroky[vKole - 1].pauzaPoS)
    }

    @Test
    fun `vsetko na cas prevedie opakovania na odpocet ale vydrz necha`() {
        val plan = Generator.zostav(
            vstup(minuty = 30, nastavenie = NastavenieTreningu(vsetkoNaCas = true, pracaS = 40)),
        )
        val polozky = plan.vsetkyPolozky.map { it.second }
        assertTrue(polozky.filter { it.cvik.meranie == "opakovania" }.all { it.naCasS == 40 })
        assertTrue(polozky.filter { it.cvik.meranie == "vydrz" }.all { it.naCasS == null })
    }

    @Test
    fun `kratsie pauzy skratia pauzu sily`() {
        val plan = Generator.zostav(vstup(nastavenie = NastavenieTreningu(pauzy = DlzkaPauz.KRATSIE)))
        val sila = plan.bloky.first { it.blok == Blok.SILA }
        assertTrue(sila.polozky.all { it.pauzaS < Pravidla.PAUZA_SILA_S })
    }

    @Test
    fun `desat minut je tiez trening`() {
        val plan = Generator.zostav(vstup(minuty = 10))
        assertTrue(plan.bloky.isNotEmpty())
        assertTrue("odhad ${plan.odhadMinut}", plan.odhadMinut in 1..20)
    }
}
