package sk.havrila.cvicime.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.DenikDatabase
import sk.havrila.cvicime.data.denik.MIGRACIA_1_2
import sk.havrila.cvicime.data.denik.MIGRACIA_2_3
import java.io.File

/**
 * Denník sa nikdy nesmie stratiť. Tento test postaví databázu presne podľa exportovanej
 * schémy verzie 1 (tak, ako ju má Lukáš v telefóne), naplní ju a nechá Room spustiť migráciu.
 * Room po migrácii porovná výslednú schému s entitami - akýkoľvek rozdiel = pád testu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DenikMigraciaTest {

    @Test
    fun `migracia 1 na 2 zachova historiu a priradi ju hlavnemu pouzivatelovi`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val nazov = "migracia-test.db"
        val subor = ctx.getDatabasePath(nazov)
        subor.parentFile?.mkdirs()
        subor.delete()

        postavPodlaSchemy(subor, verzia = 1) { db ->
            db.execSQL(
                "INSERT INTO trening(id, zaciatok, koniec, epoch_den, minuty_dna, planovane_minuty, rodinny, dokonceny, poznamka) " +
                    "VALUES (1, 1000, 2000, 20000, 1080, 30, 0, 1, '')"
            )
            db.execSQL(
                "INSERT INTO seria(trening_id, epoch_den, blok, poradie, cvik_id, rebrik_id, uroven, meranie, partie, " +
                    "plan_hodnota, skutocna_hodnota, rir, preskocena) VALUES (1, 20000, 'sila', 1, 'kliky-5', 'kliky', 5, " +
                    "'opakovania', 'hrudnik:1.0', 10, 11, 1, 0)"
            )
            db.execSQL("INSERT INTO ciel(druh, kluc, nazov, aktivny, priorita) VALUES ('zrucnost', 'lsit', 'L-sit', 1, 1)")
            db.execSQL(
                "INSERT INTO test(epoch_den, minuty_dna, rebrik_id, cvik_id, uroven, meranie, hodnota, je_vstupny) " +
                    "VALUES (20000, 1080, 'kliky', 'kliky-5', 5, 'opakovania', 9, 1)"
            )
        }

        val room = Room.databaseBuilder(ctx, DenikDatabase::class.java, nazov)
            .addMigrations(MIGRACIA_1_2, MIGRACIA_2_3)
            .allowMainThreadQueries()
            .build()
        val dao = room.dao()

        val serie = dao.serieTreningu(1)
        assertEquals(1, serie.size)
        assertEquals(0L, serie.single().ucastnikId)
        assertEquals(11, serie.single().skutocnaHodnota)
        assertFalse(serie.single().priznak)
        assertEquals(listOf("lsit"), dao.aktivneCiele().map { it.kluc })
        assertEquals(1, dao.vsetkyTesty().size)

        // Po migrácii smie mať ďalší cvičiaci cieľ s rovnakým kľúčom (unikátnosť je na dvojici).
        dao.ulozCiel(CielEntity(ucastnikId = 1, druh = "zrucnost", kluc = "lsit", nazov = "L-sit"))
        assertEquals(1, dao.aktivneCiele(ucastnikId = 1).size)
        assertEquals(1, dao.aktivneCiele().size)
        room.close()
    }

    @Test
    fun `migracia 2 na 3 zachova serie a prida priznak bez varovania`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val nazov = "migracia-2-3-test.db"
        val subor = ctx.getDatabasePath(nazov)
        subor.parentFile?.mkdirs()
        subor.delete()

        postavPodlaSchemy(subor, verzia = 2) { db ->
            db.execSQL(
                "INSERT INTO trening(id, zaciatok, koniec, epoch_den, minuty_dna, planovane_minuty, rodinny, dokonceny, poznamka) " +
                    "VALUES (1, 1000, 2000, 20000, 1080, 30, 0, 1, '')"
            )
            db.execSQL(
                "INSERT INTO ucastnik(id, meno, farba, obmedzenia, aktivny, zaciatok_epoch_den) " +
                    "VALUES (1, 'Druhy', -1, 'panvove_dno', 1, 20000)"
            )
            db.execSQL(
                "INSERT INTO seria(trening_id, epoch_den, blok, poradie, cvik_id, rebrik_id, uroven, meranie, partie, " +
                    "plan_hodnota, skutocna_hodnota, rir, preskocena, ucastnik_id) VALUES (1, 20000, 'sila', 1, 'kliky-5', " +
                    "'kliky', 5, 'opakovania', 'hrudnik:1.0', 10, 11, 1, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO seria(trening_id, epoch_den, blok, poradie, cvik_id, rebrik_id, uroven, meranie, partie, " +
                    "plan_hodnota, skutocna_hodnota, rir, preskocena, ucastnik_id) VALUES (1, 20000, 'sila', 2, 'kliky-2', " +
                    "'kliky', 2, 'opakovania', 'hrudnik:1.0', 8, 8, 1, 0, 1)"
            )
        }

        val room = Room.databaseBuilder(ctx, DenikDatabase::class.java, nazov)
            .addMigrations(MIGRACIA_1_2, MIGRACIA_2_3)
            .allowMainThreadQueries()
            .build()
        val dao = room.dao()

        val serie = dao.serieTreningu(1)
        assertEquals(2, serie.size)
        assertEquals(listOf(0L, 1L), serie.map { it.ucastnikId })
        assertEquals(listOf(11, 8), serie.map { it.skutocnaHodnota })
        assertTrue(serie.none { it.priznak })
        assertEquals("panvove_dno", dao.ucastnik(1)?.obmedzenia)

        // Príznak sa dá dodatočne nastaviť a prežije načítanie.
        dao.nastavPriznak(serie[1].id, true)
        assertTrue(dao.serieTreningu(1)[1].priznak)
        room.close()
    }

    /** Postaví databázu presne podľa exportovanej schémy danej verzie a nechá ju naplniť. */
    private fun postavPodlaSchemy(subor: File, verzia: Int, napln: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(
            File("schemas/sk.havrila.cvicime.data.denik.DenikDatabase/$verzia.json").readText()
        ).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(subor, null).use { db ->
            val entity = schema.getJSONArray("entities")
            for (i in 0 until entity.length()) {
                val e = entity.getJSONObject(i)
                val tabulka = e.getString("tableName")
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", tabulka))
                val indexy = e.optJSONArray("indices") ?: continue
                for (j in 0 until indexy.length()) {
                    db.execSQL(indexy.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tabulka))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = verzia
            napln(db)
        }
    }
}
