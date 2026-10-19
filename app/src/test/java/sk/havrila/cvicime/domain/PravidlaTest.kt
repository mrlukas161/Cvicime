package sk.havrila.cvicime.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.havrila.cvicime.data.denik.SeriaEntity
import sk.havrila.cvicime.data.katalog.CvikEntity

/** Pravidlá progresie sú jadro appky - keď sa pokazia, appka tichšie kazí tréning. */
class PravidlaTest {

    private val cvik = CvikEntity(
        id = "kliky-4",
        rebrikId = "kliky",
        uroven = 4,
        nazov = "Kliky",
        meranie = "opakovania",
        cielMin = 8,
        cielMax = 12,
        naradie = "ziadne",
        partie = "hrudnik:1.0,triceps:0.5",
        cue = "",
        chyby = "",
    )

    private fun seria(hodnota: Int?, rir: Int?, blok: String = "sila", priznak: Boolean = false) = SeriaEntity(
        treningId = 1,
        epochDen = 20000,
        blok = blok,
        poradie = 1,
        cvikId = cvik.id,
        rebrikId = cvik.rebrikId,
        uroven = cvik.uroven,
        meranie = cvik.meranie,
        partie = cvik.partie,
        planHodnota = 10,
        skutocnaHodnota = hodnota,
        rir = rir,
        priznak = priznak,
    )

    /** Režim Panvové dno: tlak, ťah alebo únik v jedinej sérii = v tomto rebríku sa nepostupuje. */
    @Test
    fun `ziadny postup ked seria mala priznak tela`() {
        val serie = listOf(seria(12, 1), seria(12, 2, priznak = true), seria(13, 1))
        assertFalse(Pravidla.maPostupitVyssie(serie, cvik))
    }

    @Test
    fun `postup vyssie ked vsetky serie splnili horny ciel a nieco ostalo v zalohe`() {
        val serie = listOf(seria(12, 1), seria(12, 2), seria(13, 1))
        assertTrue(Pravidla.maPostupitVyssie(serie, cvik))
    }

    @Test
    fun `ziadny postup ked jedna seria nesplnila horny ciel`() {
        val serie = listOf(seria(12, 1), seria(11, 1), seria(12, 2))
        assertFalse(Pravidla.maPostupitVyssie(serie, cvik))
    }

    /** Toto je dôležité: splniť cieľ na doraz neznamená, že je čas na ťažšiu variáciu. */
    @Test
    fun `ziadny postup ked vsetko islo na doraz`() {
        val serie = listOf(seria(12, 0), seria(12, 0), seria(12, 0))
        assertFalse(Pravidla.maPostupitVyssie(serie, cvik))
    }

    @Test
    fun `pokles nizsie ked ani spodny ciel nepadol a vsetko bolo na doraz`() {
        val serie = listOf(seria(5, 0), seria(4, 0))
        assertTrue(Pravidla.maKlesnutNizsie(serie, cvik))
    }

    @Test
    fun `ziadny pokles po jedinej slabej serii`() {
        assertFalse(Pravidla.maKlesnutNizsie(listOf(seria(5, 0)), cvik))
    }

    @Test
    fun `deload az po dvoch treningoch na doraz`() {
        val naDoraz = List(4) { seria(10, 0) }
        val vpohode = List(4) { seria(10, 2) }
        assertTrue(Pravidla.potrebujeDeload(listOf(naDoraz, naDoraz)))
        assertFalse(Pravidla.potrebujeDeload(listOf(naDoraz, vpohode)))
        assertFalse(Pravidla.potrebujeDeload(listOf(naDoraz)))
    }

    @Test
    fun `realna zmena - jedno opakovanie navyse este nie je progres`() {
        assertFalse(Pravidla.jeRealnaZmena(10, 11, "opakovania"))
        assertTrue(Pravidla.jeRealnaZmena(10, 12, "opakovania"))
        assertFalse(Pravidla.jeRealnaZmena(20, 22, "vydrz"))
        assertTrue(Pravidla.jeRealnaZmena(20, 23, "vydrz"))
    }

    @Test
    fun `objem ma strop a lytka nizsi`() {
        val poRokoch = 200
        val hrudnikPrioritny = Objem.cielSerii(
            sk.havrila.cvicime.data.model.Partia.HRUDNIK, true, poRokoch,
        )
        val lytkaPrioritne = Objem.cielSerii(
            sk.havrila.cvicime.data.model.Partia.LYTKA, true, poRokoch,
        )
        assertTrue("hrudník má mať strop 16, má $hrudnikPrioritny", hrudnikPrioritny == 16.0)
        assertTrue("lýtka majú mať nižší strop, majú $lytkaPrioritne", lytkaPrioritne == 12.0)
    }

    @Test
    fun `objem zacina nizko a rastie pomaly`() {
        val p = sk.havrila.cvicime.data.model.Partia.HRUDNIK
        assertTrue(Objem.cielSerii(p, false, 0) == 8.0)
        assertTrue(Objem.cielSerii(p, false, 2) == 9.0)
        assertTrue(Objem.cielSerii(p, false, 4) == 10.0)
    }

    /** Prioritná partia musí mať vyšší cieľ hneď, nie až keď rampa dorastie na strop. */
    @Test
    fun `prioritna partia startuje vyssie ale stale pod stropom`() {
        val p = sk.havrila.cvicime.data.model.Partia.HRUDNIK
        val prioritna = Objem.cielSerii(p, true, 0)
        assertTrue("priorita nemá efekt v prvom týždni", prioritna > Objem.cielSerii(p, false, 0))
        assertTrue("priorita nesmie hneď preskočiť strop", prioritna <= 16.0)
    }
}
