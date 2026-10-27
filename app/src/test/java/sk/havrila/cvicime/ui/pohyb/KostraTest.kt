package sk.havrila.cvicime.ui.pohyb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.havrila.cvicime.ui.komponenty.pohyb.Bod
import sk.havrila.cvicime.ui.komponenty.pohyb.Clen
import sk.havrila.cvicime.ui.komponenty.pohyb.Kinematika
import sk.havrila.cvicime.ui.komponenty.pohyb.Koncatina
import sk.havrila.cvicime.ui.komponenty.pohyb.Pohlad
import sk.havrila.cvicime.ui.komponenty.pohyb.Poza
import sk.havrila.cvicime.ui.komponenty.pohyb.Rozmery
import sk.havrila.cvicime.ui.komponenty.pohyb.Telo
import sk.havrila.cvicime.ui.komponenty.pohyb.Vzor

/** Kinematika bez Androidu: pevné dĺžky kostí, IK na dosiahnuteľný cieľ, interpolácia. */
class KostraTest {

    private val r = Rozmery()

    private fun poza(
        bok: Bod, trup: Double, nP: Koncatina, rP: Koncatina, nZ: Koncatina = nP, rZ: Koncatina = rP,
        t: Int = 1000, drz: Int = 0, lopatky: Double = 0.0,
    ) = Poza(bok, trup, trup, 0.0, lopatky, mapOf(Clen.NP to nP, Clen.NZ to nZ, Clen.RP to rP, Clen.RZ to rZ), t, drz)

    private fun vzor(vararg pozy: Poza, pohlad: Pohlad = Pohlad.BOK, druh: String = "opakovanie") = Vzor(
        id = "test", nazov = "Test", pohlad = pohlad, druh = druh, posun = 0.0, linearna = false,
        dych = druh == "vydrz", priemetTrupu = 1.0, oko = true, rekvizity = emptyList(), pozy = pozy.toList(),
        rozmery = r,
    )

    /** Klik: dlane IK na zemi, nohy IK na špičkách, medzi hore a dole. */
    private val klik = vzor(
        poza(Bod(0.05, 0.25), 22.0, Koncatina.Ik(-0.40, 0.07, -1), Koncatina.Ik(0.33, 0.03, -1, 0.0), t = 2000),
        poza(Bod(0.08, 0.09), 4.0, Koncatina.Ik(-0.40, 0.07, -1), Koncatina.Ik(0.33, 0.03, -1, 0.0), t = 1000),
    )

    /** Zmiešané typy: uhly <-> IK <-> smer na bod, aj spredu. */
    private val zmes = vzor(
        poza(Bod(0.0, 0.52), 90.0, Koncatina.Ik(0.02, 0.03), Koncatina.Uhly(-90.0, -90.0)),
        poza(Bod(0.0, 0.285), 16.0, Koncatina.Na(0.0, 0.04, 180.0, 180.0), Koncatina.Ik(0.3, 0.03, -1, 0.0), lopatky = -0.05),
        poza(Bod(0.1, 0.4), 60.0, Koncatina.Uhly(10.0, -80.0), Koncatina.Uhly(400.0, 120.0, 30.0), t = 0),
        pohlad = Pohlad.PRED,
    )

    private fun overDlzky(v: Vzor, t: Telo, kde: String) {
        val e = 1e-6
        assertEquals("trup $kde", r.trup, Kinematika.vzdialenost(t.bok, t.krk), e)
        assertEquals("krk $kde", r.krk, Kinematika.vzdialenost(t.krk, t.hlava), e)
        for (c in Clen.entries) {
            val k = t[c]
            val l1 = if (c.noha) r.stehno else r.rameno
            val l2 = if (c.noha) r.holen else r.predlaktie
            val l3 = if (c.noha) r.chodidlo else r.dlan
            assertEquals("$c segment 1 $kde", l1, Kinematika.vzdialenost(k.koren, k.klb), e)
            assertEquals("$c segment 2 $kde", l2, Kinematika.vzdialenost(k.klb, k.koniec), e)
            assertEquals("$c segment 3 $kde", l3, Kinematika.vzdialenost(k.koniec, k.spicka), e)
        }
    }

    @Test
    fun `doprednou kinematikou sa dlzky kosti nemenia v ziadnom case`() {
        for (v in listOf(klik, zmes)) {
            for (i in 0..200) {
                val cas = v.cyklus * i / 200.0
                overDlzky(v, Kinematika.telo(v, cas), "${v.pohlad} t=$cas")
            }
        }
    }

    @Test
    fun `IK dosiahne dosiahnutelny ciel presne a kontakt drzi aj medzi pozami`() {
        for (i in 0..50) {
            val t = Kinematika.telo(klik, klik.cyklus * i / 50.0)
            val ruka = t[Clen.RP].koniec
            val clenok = t[Clen.NP].koniec
            assertEquals(0.33, ruka.x, 1e-6)
            assertEquals(0.03, ruka.y, 1e-6)
            assertEquals(-0.40, clenok.x, 1e-6)
            assertEquals(0.07, clenok.y, 1e-6)
        }
    }

    @Test
    fun `nedosiahnutelny ciel vystrie koncatinu bez natiahnutia`() {
        val (u1, u2) = Kinematika.ik(Bod(0.0, 0.0), Bod(5.0, 0.0), 0.18, 0.16, -1)
        assertEquals(0.0, u1, 2.0)
        assertEquals(0.0, u2, 2.0)
    }

    @Test
    fun `IK ohne klb na zvolenu stranu`() {
        // koleno dopredu (+1), keď je cieľ pod bokom
        val (u1, _) = Kinematika.ik(Bod(0.0, 0.5), Bod(0.0, 0.2), r.stehno, r.holen, 1)
        val koleno = Kinematika.posun(Bod(0.0, 0.5), u1, r.stehno)
        assertTrue("koleno má ísť dopredu, je ${koleno.x}", koleno.x > 0)
        val (v1, _) = Kinematika.ik(Bod(0.0, 0.5), Bod(0.0, 0.2), r.stehno, r.holen, -1)
        assertTrue(Kinematika.posun(Bod(0.0, 0.5), v1, r.stehno).x < 0)
    }

    @Test
    fun `najblizsi uhol neide dlhou cestou`() {
        assertEquals(190.0, Kinematika.najblizsi(-170.0, 170.0), 1e-9)
        assertEquals(-10.0, Kinematika.najblizsi(350.0, 0.0), 1e-9)
    }

    @Test
    fun `kluce pozy sedia v case a vydrz drzi pozu`() {
        val v = vzor(
            poza(Bod(0.0, 0.5), 90.0, Koncatina.Uhly(-90.0, -90.0), Koncatina.Uhly(-90.0, -90.0), t = 1000, drz = 500),
            poza(Bod(0.0, 0.3), 60.0, Koncatina.Uhly(-60.0, -120.0), Koncatina.Uhly(0.0, 0.0), t = 1000),
        )
        assertEquals(2500, v.cyklus)
        assertEquals(0.5, Kinematika.pozaVCase(v, 250.0).bok.y, 1e-9)
        assertEquals(0.3, Kinematika.pozaVCase(v, 1500.0).bok.y, 1e-9)
        val stred = Kinematika.pozaVCase(v, 1000.0).bok.y
        assertEquals(0.4, stred, 1e-9)
        // po cykle späť na začiatok
        assertEquals(0.5, Kinematika.pozaVCase(v, 2500.0 + 100.0).bok.y, 1e-9)
    }
}
