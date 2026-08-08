package sk.havrila.cvicime.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pokrok k cieľom ukazuje používateľovi, čo ho drží - keď sa pokazí, appka radí zle. */
class PokrokTest {

    /** muscleup=15, pritahy=10, vis=8 - bežný katalóg s tromi rebríkmi. */
    private val maxUrovne = mapOf("muscleup" to 15, "pritahy" to 10, "vis" to 8)

    private fun muscleup(urovne: Map<String, Int>) =
        Pokrok.vyhodnotZrucnost("muscleup", "Muscle-up", urovne, maxUrovne)

    @Test
    fun `muscleup na zaciatku ma pokrok blizko nuly a brzdu`() {
        val ciel = muscleup(emptyMap())
        assertTrue("na začiatku má byť pokrok < 0.1, je ${ciel.pokrok}", ciel.pokrok < 0.1)
        assertNotNull("na začiatku musí existovať brzda", ciel.brzda)
    }

    @Test
    fun `splnena brana ma pokrok 1 a nie je brzdou`() {
        // potrebná úroveň príťahov = ceil(10 * 0.7) = 7
        val ciel = muscleup(mapOf("pritahy" to 7))
        val pritahy = ciel.brany.first { it.brana.rebrikId == "pritahy" }
        assertEquals(7, pritahy.potrebnaUroven)
        assertEquals(1.0, pritahy.pokrok, 1e-9)
        assertNotNull(ciel.brzda)
        assertFalse("splnená brána nesmie byť brzdou", ciel.brzda!!.brana.rebrikId == "pritahy")
    }

    @Test
    fun `brzda je brana s najnizsim pokrokom`() {
        // muscleup 9/14, vis 3/4 - príťahy 1/6 sú výrazne najnižšie
        val ciel = muscleup(mapOf("muscleup" to 10, "pritahy" to 2, "vis" to 4))
        assertNotNull(ciel.brzda)
        assertEquals("pritahy", ciel.brzda!!.brana.rebrikId)
        val najnizsi = ciel.brany.minOf { it.pokrok }
        assertEquals(najnizsi, ciel.brzda!!.pokrok, 1e-9)
    }

    @Test
    fun `neznamy rebrik v maxUrovniach sa preskoci`() {
        val bezVisu = mapOf("muscleup" to 15, "pritahy" to 10)
        val ciel = Pokrok.vyhodnotZrucnost("muscleup", "Muscle-up", emptyMap(), bezVisu)
        assertEquals(2, ciel.brany.size)
        assertTrue("brány nesmú obsahovať vis", ciel.brany.none { it.brana.rebrikId == "vis" })
    }

    @Test
    fun `veta brzdy obsahuje nazov brany a stupne`() {
        val ciel = muscleup(mapOf("muscleup" to 10, "pritahy" to 2, "vis" to 4))
        val veta = Pokrok.vetaBrzdy(ciel)
        assertNotNull(veta)
        assertTrue("veta má obsahovať názov brány: $veta", veta!!.contains("Príťahy"))
        assertTrue("veta má obsahovať aktuálny stupeň: $veta", veta.contains("stupni 2"))
        assertTrue("veta má obsahovať potrebný stupeň: $veta", veta.contains("potrebných 7"))

        val hotovo = muscleup(mapOf("muscleup" to 15, "pritahy" to 10, "vis" to 8))
        assertEquals(1.0, hotovo.pokrok, 1e-9)
        assertNull("pri 100 % pokroku nie je čo brzdí", Pokrok.vetaBrzdy(hotovo))
    }

    @Test
    fun `pokrok je vzdy v rozsahu 0 az 1`() {
        val extremy = listOf(
            emptyMap(),
            mapOf("muscleup" to 0, "pritahy" to 0, "vis" to 0),
            mapOf("muscleup" to 999, "pritahy" to 999, "vis" to 999),
            mapOf("muscleup" to 999, "pritahy" to 0, "vis" to 4),
        )
        for (urovne in extremy) {
            val ciel = muscleup(urovne)
            assertTrue("pokrok cieľa mimo rozsahu: ${ciel.pokrok} pre $urovne", ciel.pokrok in 0.0..1.0)
            for (b in ciel.brany) {
                assertTrue("pokrok brány mimo rozsahu: ${b.pokrok} pre $urovne", b.pokrok in 0.0..1.0)
            }
        }
    }
}
