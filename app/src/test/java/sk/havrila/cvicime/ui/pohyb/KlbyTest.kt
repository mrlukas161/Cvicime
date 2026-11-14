package sk.havrila.cvicime.ui.pohyb

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sk.havrila.cvicime.ui.komponenty.pohyb.Clen
import sk.havrila.cvicime.ui.komponenty.pohyb.Kinematika
import sk.havrila.cvicime.ui.komponenty.pohyb.Koncatina
import sk.havrila.cvicime.ui.komponenty.pohyb.Poza
import sk.havrila.cvicime.ui.komponenty.pohyb.PohybyJson
import sk.havrila.cvicime.ui.komponenty.pohyb.Vzor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Anatomická kontrola všetkých vzorov z boku po celom cykle: koleno sa nelomí dozadu, stehno
 * neprejde cez trup, segment neskáče a kĺb sa počas presunu neohne mimo rozsahu koncových póz.
 * Rovnaké pravidlá a výnimky má tools/pohyby_nahlad.py (červené políčka) - pri zmene upraviť oboje.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KlbyTest {

    companion object {
        /** ohnutie kolena: stehno - holeň (záporné = koleno sa lomí dozadu) */
        const val KOLENO_MIN = -3.0
        const val KOLENO_MAX = 160.0

        /** stehno voči trupu: 0 = v predĺžení trupu, + = flexia (stehno dopredu) */
        const val BEDRO_MIN = -40.0
        const val BEDRO_MAX = 150.0

        /** max. zmena uhla segmentu medzi dvoma vzorkami */
        const val KROK_MAX = 25.0

        /** ohnutie počas presunu smie prekročiť rozsah koncových póz najviac o toľko */
        const val PRESAH = 20.0

        /**
         * Bez kontroly: detské zvieratká sú zámerne štylizované (chôdze po štyroch, plazenie,
         * skoky, výdrže zvieratiek) a používateľ ich chce nechať tak, ako vyzerajú.
         */
        val VYNIMKY = setOf(
            "medvedia-chodza", "krabia-chodza", "hadie-plazenie", "kacacia-chodza", "husenica", "krokodil",
            "tucniak", "slon", "pavucia-chodza", "rak", "zirafa",
            "zabie-skoky", "zajacie-skoky", "klokan", "delfin",
            "bocian", "plameniak", "klbko", "psik", "kobra",
        )
    }

    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    private val katalog by lazy {
        PohybyJson.parsuj(app.assets.open("pohyby.json").bufferedReader().use { it.readText() })
    }

    private fun uhly(v: Vzor, p: Poza): Map<Clen, Triple<Double, Double, Double>> = Clen.entries.associateWith { c ->
        val (u1, u2, u3) = Kinematika.vyries(v, c, p[c], Kinematika.koren(v, c, p.bok, p.trup, p.lopatky))
        Triple(u1, u2, u3 ?: Kinematika.predvolenyKoniec(v, c, u2))
    }

    /** Index presunu z pózy i do i+1 v čase, null počas výdrže. */
    private fun presun(v: Vzor, cas: Double): Int? {
        var c = ((cas % v.cyklus) + v.cyklus) % v.cyklus
        for ((i, p) in v.pozy.withIndex()) {
            if (c < p.drz) return null
            c -= p.drz
            if (c < p.t || i == v.pozy.size - 1) return i
            c -= p.t
        }
        return null
    }

    /** Končatina drží ten istý bod (IK s rovnakým cieľom) - ohnutie vtedy určuje geometria. */
    private fun pevnyKontakt(v: Vzor, i: Int, c: Clen): Boolean {
        val a = v.pozy[i][c]
        val b = v.pozy[(i + 1) % v.pozy.size][c]
        return a is Koncatina.Ik && b is Koncatina.Ik && a.x == b.x && a.y == b.y
    }

    private fun porusenia(v: Vzor): List<String> {
        val chyby = ArrayList<String>()
        val kluc = v.pozy.indices.map { i ->
            val zaciatok = v.pozy.take(i).sumOf { it.drz + it.t } + v.pozy[i].drz
            uhly(v, Kinematika.pozaVCase(v, zaciatok.toDouble()))
        }
        val n = max(120, ceil(v.cyklus / 20.0).toInt())
        var pred: Map<Clen, Triple<Double, Double, Double>>? = null
        for (k in 0 until n) {
            val cas = v.cyklus.toDouble() * k / n
            val p = Kinematika.pozaVCase(v, cas)
            val uh = uhly(v, p)
            val i = presun(v, cas)
            for ((c, u) in uh) {
                val f = Kinematika.ohyb(c, u.first, u.second)
                if (c.noha) {
                    if (f !in KOLENO_MIN..KOLENO_MAX) chyby += "t=${cas.toInt()} $c koleno ${f.toInt()}°"
                    val bedro = Kinematika.najblizsi(u.first - p.trup + 180, 0.0)
                    if (bedro !in BEDRO_MIN..BEDRO_MAX) chyby += "t=${cas.toInt()} $c bedro ${bedro.toInt()}°"
                }
                if (i != null && !pevnyKontakt(v, i, c)) {
                    val a = kluc[i].getValue(c)
                    val b = kluc[(i + 1) % kluc.size].getValue(c)
                    val fa = Kinematika.ohyb(c, a.first, a.second)
                    val fb = Kinematika.ohyb(c, b.first, b.second)
                    if (f < minOf(fa, fb) - PRESAH || f > maxOf(fa, fb) + PRESAH) {
                        chyby += "t=${cas.toInt()} $c ohnutie ${f.toInt()}° mimo ${fa.toInt()}..${fb.toInt()}"
                    }
                }
                pred?.getValue(c)?.let { s ->
                    val skoky = listOf(u.first - s.first, u.second - s.second, u.third - s.third)
                    skoky.forEachIndexed { j, d ->
                        val dd = Kinematika.najblizsi(d, 0.0)
                        if (abs(dd) > KROK_MAX) chyby += "t=${cas.toInt()} $c segment ${j + 1} skok ${dd.toInt()}°"
                    }
                }
            }
            pred = uh
        }
        return chyby
    }

    @Test
    fun `klby vo vzoroch z boku sa neohybaju nefyziologicky`() {
        val kontrolovane = katalog.vzory.filter { it.zboku && it.id !in VYNIMKY }
        assertTrue("kontrolovaných vzorov je málo: ${kontrolovane.size}", kontrolovane.size > 100)
        val zle = kontrolovane.mapNotNull { v ->
            porusenia(v).takeIf { it.isNotEmpty() }?.let { "${v.id}: ${it.size}× napr. ${it.take(3)}" }
        }
        assertTrue("nefyziologické kĺby (pozri python3 tools/pohyby_nahlad.py):\n" + zle.joinToString("\n"), zle.isEmpty())
    }

    @Test
    fun `vynimky su existujuce vzory`() {
        for (id in VYNIMKY) assertTrue("výnimka $id neexistuje", katalog.ma(id))
    }
}
