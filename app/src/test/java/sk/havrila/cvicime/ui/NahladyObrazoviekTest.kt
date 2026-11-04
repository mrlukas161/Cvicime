package sk.havrila.cvicime.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.KatalogLoader
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Rir
import sk.havrila.cvicime.domain.PlanPolozka
import sk.havrila.cvicime.ui.obrazovky.DetskyKrok
import sk.havrila.cvicime.ui.obrazovky.DetskyStav
import sk.havrila.cvicime.ui.obrazovky.KrokTreningu
import sk.havrila.cvicime.ui.obrazovky.PartnerKrok
import sk.havrila.cvicime.ui.obrazovky.PartnerStopa
import sk.havrila.cvicime.ui.obrazovky.PartnerZapis
import sk.havrila.cvicime.ui.obrazovky.PrehravacModel
import sk.havrila.cvicime.ui.obrazovky.PrehravacObrazovka
import sk.havrila.cvicime.ui.obrazovky.StavPrehravaca
import sk.havrila.cvicime.ui.theme.CvicimeTheme
import java.io.File

/**
 * Náhľady prehrávača na rôznych telefónoch - SKUTOČNÉ vykreslenie (Robolectric native graphics).
 * Najhorší prípad naraz: ty, druhý dospelý so svojím odpočtom a dieťa s cvikom.
 * Obrázky idú do app/build/nahlady/ - kontroluje sa, či je všetko dôležité na jednej obrazovke.
 * Test zlyhá len pri páde vykreslenia; rozloženie posudzuje človek (alebo Claude) z obrázkov.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NahladyObrazoviekTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun cvik(id: String): CvikEntity = runBlocking {
        KatalogLoader.nacitajAkTreba(app)
        Repozitar.ziskaj(app).katalog.cvik(id) ?: error("v katalógu chýba $id")
    }

    private fun polozka(id: String, ciel: Int, naCas: Int? = null, serie: Int = 3) = cvik(id).let {
        PlanPolozka(it, it.rebrikId, it.rebrikId, serie, ciel, 90, 1, naCasS = naCas)
    }

    private val partner = PartnerStopa(ucastnikId = 1, meno = "Partnerka", farba = 0xFF2F8F83.toInt(), otazka = "Cítila si tlak, ťah alebo únik?")

    private fun snimky(zariadenie: String) {
        val model = PrehravacModel(app)
        val drep = polozka("drep-4", 10, naCas = 40)
        val kliky = polozka("kliky-5", 10)
        val jej = PartnerKrok(polozka("stred-setrne-3", 8, naCas = 40), 2, 3)
        val krokCas = KrokTreningu(Blok.SILA, drep, 2, 3, partner = jej, pauzaPoS = 90)
        val krokPocet = KrokTreningu(Blok.SILA, kliky, 1, 3, partner = PartnerKrok(polozka("kliky-3", 8), 1, 3), pauzaPoS = 90)
        val detskyCvik = runBlocking { Repozitar.ziskaj(app).katalog.detskeCviky() }
        val dieta = DetskyStav(dietaId = 1, meno = "Dieťa", krok = DetskyKrok.CVICI, aktualny = detskyCvik.first(), labky = 2)
        val vyber = dieta.copy(krok = DetskyKrok.VYBER, aktualny = null, moznosti = detskyCvik.take(2))

        compose.mainClock.autoAdvance = false
        compose.setContent {
            CvicimeTheme { PrehravacObrazovka(minuty = 30, dietaId = -1, hotovo = {}, model = model) }
        }
        fun odfot(nazov: String) {
            compose.mainClock.advanceTimeBy(700)
            compose.waitForIdle()
            // Celé okno priamo do bitmapy (captureToImage čaká na snímku, ktorá pri zastavených
            // hodinách nepríde).
            val okno = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(okno.width, okno.height, Bitmap.Config.ARGB_8888)
            okno.draw(Canvas(bmp))
            val f = File("build/nahlady/$zariadenie-$nazov.png")
            f.parentFile?.mkdirs()
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        model.nastavNahlad(listOf(krokCas, krokPocet), 0, StavPrehravaca.CVICENIE, odpocetMs = 40_000, detsky = dieta, partner = partner)
        odfot("1-cas-partner-dieta")
        model.nastavNahlad(listOf(krokCas, krokPocet), 1, StavPrehravaca.CVICENIE, detsky = vyber, partner = partner)
        odfot("2-pocet-partner-vyber")
        model.nastavNahlad(
            listOf(krokCas, krokPocet), 1, StavPrehravaca.PAUZA, odpocetMs = 90_000, detsky = dieta, partner = partner,
            partnerZapis = PartnerZapis(jej, Blok.SILA, 8, Rir.JEDNO_DVE),
        )
        odfot("3-pauza")
        model.nastavNahlad(listOf(krokCas, krokPocet), 1, StavPrehravaca.OTAZKA, detsky = dieta, partner = partner)
        odfot("4-otazka")
    }

    @Test @Config(qualifiers = "w360dp-h640dp-port-xhdpi")
    fun maly() = snimky("maly-360x640")

    @Test @Config(qualifiers = "w360dp-h780dp-port-xxhdpi")
    fun bezny() = snimky("bezny-360x780")

    @Test @Config(qualifiers = "w393dp-h852dp-port-xxhdpi")
    fun stredny() = snimky("stredny-393x852")

    @Test @Config(qualifiers = "w412dp-h915dp-port-xxhdpi")
    fun velky() = snimky("velky-412x915")

    @Test @Config(qualifiers = "w800dp-h360dp-land-xxhdpi")
    fun lezato() = snimky("lezato-800x360")

    @Test @Config(qualifiers = "w360dp-h780dp-port-xxhdpi", fontScale = 1.3f)
    fun vacsiePismo() = snimky("pismo130-360x780")
}
