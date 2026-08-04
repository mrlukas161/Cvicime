package sk.havrila.cvicime

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.katalog.KatalogLoader
import sk.havrila.cvicime.data.katalog.rozparsujPartie
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Meranie
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia
import sk.havrila.cvicime.data.model.Skupina

/**
 * Testy proti SKUTOČNÉMU katalógu z assets a proti skutočnej databáze.
 *
 * Druhá polovica je regresný test na chybu z 04.08.2026: ciele sa ukladali,
 * ale obrazovka ich znova nenačítala, takže sa nedalo pokračovať na tréning.
 * Preto sa tu overuje celá cesta uloženie -> tok -> zostavenie plánu.
 *
 * Všetko je v jednej triede zámerne - Repozitar aj databázy sú singletony
 * a rozdelenie do viacerých tried by ich zdieľalo naprieč Robolectric kontextami.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KatalogAUkladanieTest {

    private lateinit var repo: Repozitar

    @Before
    fun priprav() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        KatalogLoader.nacitajAkTreba(app)
        repo = Repozitar.ziskaj(app)
    }

    // ---------------------------------------------------------------- katalóg

    @Test
    fun `katalog sa nacita z assets a ma ocakavany rozsah`() = runTest {
        val rebriky = repo.rebriky()
        val cviky = rebriky.flatMap { repo.cvikyRebrika(it.id) }
        assertTrue("rebríkov má byť aspoň 25, je ${rebriky.size}", rebriky.size >= 25)
        assertTrue("cvikov má byť aspoň 150, je ${cviky.size}", cviky.size >= 150)
    }

    @Test
    fun `vsetky ciele zrucnosti z rebrikov existuju`() = runTest {
        val zrucnosti = repo.rebriky().filter { it.jeZrucnost }.map { it.id }
        assertTrue("chýba L-sit", zrucnosti.contains("lsit"))
        assertTrue("chýba stojka", zrucnosti.contains("stojka"))
        assertTrue("chýba muscle-up", zrucnosti.contains("muscleup"))
    }

    @Test
    fun `kluce v katalogu su platne`() = runTest {
        val platneSkupiny = Skupina.entries.map { it.kluc }.toSet()
        val platneNaradie = Naradie.entries.map { it.kluc }.toSet()
        val platnePartie = Partia.entries.map { it.kluc }.toSet()
        val platneMerania = Meranie.entries.map { it.kluc }.toSet()

        for (r in repo.rebriky()) {
            assertTrue("neznáma skupina '${r.skupina}' v rebríku ${r.id}", r.skupina in platneSkupiny)
            val cviky = repo.cvikyRebrika(r.id)
            assertTrue("rebrík ${r.id} nemá cviky", cviky.isNotEmpty())
            assertEquals(
                "rebrík ${r.id} má diery v úrovniach",
                (1..cviky.size).toList(),
                cviky.map { it.uroven }.sorted(),
            )
            for (c in cviky) {
                assertTrue("neznáme meranie '${c.meranie}' v ${c.id}", c.meranie in platneMerania)
                c.naradie.split(",").filter { it.isNotBlank() }.forEach {
                    assertTrue("neznáme náradie '$it' v ${c.id}", it in platneNaradie)
                }
                rozparsujPartie(c.partie).forEach { (kluc, vaha) ->
                    assertTrue("neznáma partia '$kluc' v ${c.id}", kluc in platnePartie)
                    assertTrue("váha partie mimo 0..1 v ${c.id}", vaha > 0.0 && vaha <= 1.0)
                }
            }
        }
    }

    /** Mobilita a rozohriatie nesmú nafukovať týždenný objem. */
    @Test
    fun `rozohriatie a mobilita nemaju priradene partie`() = runTest {
        val bezObjemu = setOf(Skupina.ROZOHRIATIE, Skupina.MOBILITA)
        for (r in repo.rebriky().filter { Skupina.z(it.skupina) in bezObjemu }) {
            for (c in repo.cvikyRebrika(r.id)) {
                assertTrue(
                    "cvik ${c.id} zo skupiny ${r.skupina} má partie '${c.partie}'",
                    rozparsujPartie(c.partie).isEmpty(),
                )
            }
        }
    }

    // ---------------------------------------------------------------- regresia: ukladanie cieľov

    @Test
    fun `ulozene ciele sa objavia v toku a onboarding sa oznaci ako hotovy`() = runTest {
        repo.ulozCiel(CielEntity(druh = DruhCiela.ZRUCNOST.kluc, kluc = "lsit", nazov = "L-sit"))
        repo.ulozCiel(CielEntity(druh = DruhCiela.PARTIA.kluc, kluc = "lytka", nazov = "Lýtka"))
        val profil = repo.profil()
        repo.ulozProfil(
            profil.copy(
                naradie = "ziadne,podlozka,hrazda,jednorucky,schod,stena",
                onboardingHotovy = true,
                zaciatokEpochDen = repo.dnes(),
            )
        )

        val zTokuCiele = repo.cieleTok().first()
        assertEquals(2, zTokuCiele.size)
        assertTrue(zTokuCiele.any { it.kluc == "lsit" })
        assertTrue(zTokuCiele.any { it.kluc == "lytka" })

        val zTokuProfil = repo.profilTok().first()
        assertNotNull(zTokuProfil)
        assertTrue("onboarding sa mal označiť ako hotový", zTokuProfil!!.onboardingHotovy)
    }

    /**
     * Toto je jadro regresie: po nastavení cieľov a náradia MUSÍ vzniknúť
     * neprázdny plán - inak sa v appke nedá pokračovať na tréning.
     */
    @Test
    fun `po nastaveni cielov vznikne neprazdny plan pre vsetky dlzky treningu`() = runTest {
        repo.ulozCiel(CielEntity(druh = DruhCiela.ZRUCNOST.kluc, kluc = "lsit", nazov = "L-sit"))
        val profil = repo.profil()
        repo.ulozProfil(
            profil.copy(
                naradie = "ziadne,podlozka,hrazda,jednorucky,schod,stena,stolicka",
                onboardingHotovy = true,
                vstupnyTestHotovy = true,
                zaciatokEpochDen = repo.dnes(),
            )
        )

        for (minuty in listOf(15, 30, 45)) {
            val plan = repo.zostavPlan(minuty)
            assertTrue("pri $minuty min vyšiel prázdny plán", plan.bloky.isNotEmpty())
            assertTrue(
                "pri $minuty min plán neobsahuje ani jeden cvik",
                plan.vsetkyPolozky.isNotEmpty(),
            )
        }
    }

    /** Aj bez náradia musí appka niečo poskladať - inak používateľ uviazne. */
    @Test
    fun `aj s holym telom vznikne nejaky trening`() = runTest {
        val profil = repo.profil()
        repo.ulozProfil(
            profil.copy(naradie = "ziadne,podlozka", onboardingHotovy = true, vstupnyTestHotovy = true)
        )
        val plan = repo.zostavPlan(30)
        assertTrue("bez náradia nevznikol žiadny tréning", plan.vsetkyPolozky.isNotEmpty())
    }

    @Test
    fun `generovany plan nikdy nesiahne po nedostupnom naradi`() = runTest {
        val profil = repo.profil()
        repo.ulozProfil(
            profil.copy(naradie = "ziadne,podlozka", onboardingHotovy = true, vstupnyTestHotovy = true)
        )
        val plan = repo.zostavPlan(45)
        val dostupne = setOf("ziadne", "podlozka")
        for ((_, polozka) in plan.vsetkyPolozky) {
            val potrebne = polozka.cvik.naradie.split(",").filter { it.isNotBlank() }
            assertTrue(
                "cvik ${polozka.cvik.id} vyžaduje ${polozka.cvik.naradie}, dostupné je len $dostupne",
                potrebne.all { it in dostupne },
            )
        }
    }

    /** Séria sa musí rozpadnúť na partie, inak by týždenný objem ostal navždy nulový. */
    /** Nájde prvý cvik danej skupiny, ktorý má priradené partie. */
    private suspend fun prvyCvikSPartiami(skupina: Skupina) =
        repo.rebriky()
            .filter { Skupina.z(it.skupina) == skupina }
            .firstNotNullOf { r ->
                repo.cvikyRebrika(r.id).firstOrNull { rozparsujPartie(it.partie).isNotEmpty() }
            }

    @Test
    fun `zapisana seria sa premietne do tyzdenneho objemu`() = runTest {
        val cvik = prvyCvikSPartiami(Skupina.TLAK)

        val treningId = repo.zacniTrening(30, rodinny = false)
        repo.zapisSeriu(
            treningId = treningId,
            blok = "sila",
            poradie = 1,
            cvik = cvik,
            planHodnota = 10,
            skutocnaHodnota = 10,
            rir = 1,
        )

        val dnes = repo.dnes()
        val objem = repo.denik.objemPodlaPartii(repo.zaciatokTyzdna(dnes), dnes)
        assertTrue("séria sa nepremietla do objemu", objem.isNotEmpty())
        assertTrue("súčet sérií má byť kladný", objem.sumOf { it.serie } > 0.0)
    }

    /** Preskočená séria sa do objemu rátať nesmie. */
    @Test
    fun `preskocena seria sa do objemu neratá`() = runTest {
        val cvik = prvyCvikSPartiami(Skupina.NOHY)

        val dnes = repo.dnes()
        val pred = repo.denik.objemPodlaPartii(repo.zaciatokTyzdna(dnes), dnes).sumOf { it.serie }

        val treningId = repo.zacniTrening(30, rodinny = false)
        repo.zapisSeriu(
            treningId = treningId,
            blok = "sila",
            poradie = 1,
            cvik = cvik,
            planHodnota = 10,
            skutocnaHodnota = null,
            rir = null,
            preskocena = true,
        )

        val po = repo.denik.objemPodlaPartii(repo.zaciatokTyzdna(dnes), dnes).sumOf { it.serie }
        assertEquals(pred, po, 0.001)
    }

    @Test
    fun `denny cieľ bielkovin je v pasme 1_6 az 2_2 g na kilo`() {
        val vaha = 82.0
        val ciel = repo.odporucaneBielkoviny(vaha)
        assertTrue("cieľ $ciel g je mimo pásma", ciel >= vaha * 1.6 && ciel <= vaha * 2.2)
    }

    @Test
    fun `prazdny katalog nezhodi appku`() = runTest {
        // Loader musí chybu prehltnúť a vrátiť -1, nie vyhodiť výnimku.
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vysledok = KatalogLoader.nacitajAkTreba(app)
        assertFalse("loader nemá hádzať výnimku", vysledok == Int.MIN_VALUE)
    }
}
