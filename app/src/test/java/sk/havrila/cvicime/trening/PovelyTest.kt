package sk.havrila.cvicime.trening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PovelyTest {

    @Test
    fun `cisla slovom aj cislicami`() {
        assertEquals(10, Povely.cislo("10"))
        assertEquals(10, Povely.cislo("Desať"))
        assertEquals(12, Povely.cislo("dvanásť opakovaní"))
        assertEquals(25, Povely.cislo("dvadsaťpäť"))
        assertEquals(25, Povely.cislo("dvadsať päť"))
        assertEquals(20, Povely.cislo("dvadsať"))
        assertEquals(4, Povely.cislo("štyri"))
        assertEquals(2, Povely.cislo("dve"))
        assertEquals(8, Povely.cislo("spravil som 8"))
        assertNull(Povely.cislo("neviem"))
    }

    @Test
    fun `povely`() {
        assertEquals(Povel.Cislo(10), Povely.rozpoznaj("desať"))
        assertEquals(Povel.Preskoc, Povely.rozpoznaj("preskoč pauzu"))
        assertEquals(Povel.Dalej, Povely.rozpoznaj("Ďalší"))
        assertEquals(Povel.Dalej, Povely.rozpoznaj("hotovo"))
        assertEquals(Povel.Pozastav, Povely.rozpoznaj("pauza"))
        assertEquals(Povel.Pokracuj, Povely.rozpoznaj("pokračuj"))
        assertEquals(Povel.Pridaj, Povely.rozpoznaj("pridaj"))
        assertEquals(Povel.Nic, Povely.rozpoznaj("už nič"))
        assertNull(Povely.rozpoznaj("hmm"))
    }

    @Test
    fun `rezerva z odpovede`() {
        assertEquals(0, Povely.rezerva("nič"))
        assertEquals(1, Povely.rezerva("dve"))
        assertEquals(1, Povely.rezerva("jedno"))
        assertEquals(3, Povely.rezerva("päť"))
        assertEquals(3, Povely.rezerva("ešte veľa"))
        assertNull(Povely.rezerva("neviem"))
    }
}
