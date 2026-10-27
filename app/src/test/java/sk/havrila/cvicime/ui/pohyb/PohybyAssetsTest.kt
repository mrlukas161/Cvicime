package sk.havrila.cvicime.ui.pohyb

import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sk.havrila.cvicime.ui.komponenty.pohyb.Clen
import sk.havrila.cvicime.ui.komponenty.pohyb.Kinematika
import sk.havrila.cvicime.ui.komponenty.pohyb.Pohyby
import sk.havrila.cvicime.ui.komponenty.pohyb.PohybyJson

/** Skutočné assets: pohyby.json sa dá načítať, každý cvik má platný pohyb, kosti nemenia dĺžku. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PohybyAssetsTest {

    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun asset(meno: String) = app.assets.open(meno).bufferedReader().use { it.readText() }

    private val katalog by lazy { PohybyJson.parsuj(asset("pohyby.json")) }

    @Test
    fun `pohyby json sa nacita a id su jedinecne`() {
        val ids = katalog.vzory.map { it.id }
        assertTrue("vzorov má byť aspoň 60, je ${ids.size}", ids.size >= 60)
        assertEquals("duplicitné id", ids.size, ids.toSet().size)
        ids.forEach { assertTrue("id '$it' má byť ascii bez medzier", it.matches(Regex("[a-z0-9-]+"))) }
        assertEquals(ids.size, Pohyby.katalog(app).vzory.size)
    }

    @Test
    fun `kazdy vzor ma aspon dve klucove pozy a kladny cyklus`() {
        for (v in katalog.vzory) {
            assertTrue("${v.id}: menej ako 2 pózy", v.pozy.size >= 2)
            assertTrue("${v.id}: nulový cyklus", v.pozy.sumOf { it.t + it.drz } > 0)
            assertTrue("${v.id}: neznámy druh ${v.druh}", v.druh in setOf("opakovanie", "vydrz"))
        }
    }

    @Test
    fun `kazdy cvik v katalogu ma pohyb ktory existuje`() {
        val k = JSONObject(asset("katalog.json"))
        assertTrue("katalóg má byť aspoň v4", k.getInt("verzia") >= 4)
        val rebriky = k.getJSONArray("rebriky")
        var pocet = 0
        for (i in 0 until rebriky.length()) {
            val cviky = rebriky.getJSONObject(i).getJSONArray("cviky")
            for (j in 0 until cviky.length()) {
                val c = cviky.getJSONObject(j)
                val pohyb = c.optString("pohyb")
                assertTrue("cvik ${c.getString("id")} nemá pohyb", pohyb.isNotBlank())
                assertTrue("cvik ${c.getString("id")}: neznámy pohyb '$pohyb'", katalog.ma(pohyb))
                pocet++
            }
        }
        assertTrue(pocet > 150)
    }

    @Test
    fun `predvolene pohyby rebrikov existuju`() {
        val k = JSONObject(asset("katalog.json")).getJSONArray("rebriky")
        for (i in 0 until k.length()) {
            val id = k.getJSONObject(i).getString("id")
            val p = Pohyby.predvolenyPre(id)
            assertTrue("rebrík $id nemá predvolený pohyb", p != null)
            assertTrue("rebrík $id: neznámy predvolený pohyb $p", katalog.ma(p))
        }
    }

    @Test
    fun `dlzky kosti su pevne vo vsetkych vzoroch po celom cykle`() {
        val r = katalog.rozmery
        val e = 1e-6
        for (v in katalog.vzory) {
            for (i in 0 until 60) {
                val cas = v.cyklus * i / 60.0
                val t = Kinematika.telo(v, cas)
                val kde = "${v.id} t=${cas.toInt()}"
                assertEquals("trup $kde", r.trup * v.priemetTrupu, Kinematika.vzdialenost(t.bok, t.krk), e)
                assertEquals("krk $kde", r.krk, Kinematika.vzdialenost(t.krk, t.hlava), e)
                for (c in Clen.entries) {
                    val k = t[c]
                    assertEquals("$c/1 $kde", if (c.noha) r.stehno else r.rameno, Kinematika.vzdialenost(k.koren, k.klb), e)
                    assertEquals("$c/2 $kde", if (c.noha) r.holen else r.predlaktie, Kinematika.vzdialenost(k.klb, k.koniec), e)
                }
            }
            val ramec = v.ramec
            assertTrue("${v.id}: rámec", ramec.strana >= 1.0 && ramec.strana < 4.0)
        }
    }
}
