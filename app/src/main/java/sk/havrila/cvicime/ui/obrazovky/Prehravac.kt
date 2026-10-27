package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import android.os.SystemClock
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Rekord
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.UcastEntity
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Meranie
import sk.havrila.cvicime.data.model.Rir
import sk.havrila.cvicime.deti.Zviera
import sk.havrila.cvicime.deti.Zvierata
import sk.havrila.cvicime.domain.PlanPolozka
import sk.havrila.cvicime.domain.Pravidla
import sk.havrila.cvicime.domain.Rozvrh
import sk.havrila.cvicime.trening.RezimZvuku
import sk.havrila.cvicime.trening.Hlas
import sk.havrila.cvicime.trening.TreningService
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.Konfety
import sk.havrila.cvicime.ui.komponenty.Inicialka
import sk.havrila.cvicime.ui.komponenty.KruhPostupu
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.SegmentovyPruh
import sk.havrila.cvicime.ui.komponenty.StatDlazdica
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.formatujHodnotu
import sk.havrila.cvicime.ui.komponenty.formatujTrvanie
import sk.havrila.cvicime.ui.komponenty.pruzneStlacenie
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk
import sk.havrila.cvicime.ui.theme.StylVelkeCislo
import kotlin.math.roundToInt

/** Jedna séria v poradí, ako sa reálne odcvičí. */
data class KrokTreningu(
    val blok: Blok,
    val polozka: PlanPolozka,
    val cisloSerie: Int,
    val zoSerii: Int,
    /** čo v tej istej chvíli cvičí druhý dospelý (null = v tomto kroku nič) */
    val partner: PartnerKrok? = null,
    /** oddych po tejto sérii - pri pároch a v kruhu krátky prechod, inak pauza položky */
    val pauzaPoS: Int = polozka.pauzaS,
)

/** Séria ide na odpočet: výdrž, alebo cvik na opakovania cvičený na čas. */
fun KrokTreningu.jeNaCas(): Boolean = polozka.naCasS != null || Meranie.z(polozka.cvik.meranie) == Meranie.VYDRZ

/** Dĺžka odpočtu série v sekundách. */
fun KrokTreningu.sekundOdpoctu(): Int = polozka.naCasS ?: polozka.ciel

/** Po sérii sa pýta na počet a rezervu: sila, kondícia, a zručnosť na čas (počet sa inak nezistí). */
fun KrokTreningu.pytaSaPoSerii(): Boolean =
    blok == Blok.SILA || blok == Blok.KONDICIA || (blok == Blok.SKILL && polozka.naCasS != null)

/** Séria druhého dospelého - z jeho vlastného plánu, spárovaná s krokom hlavného cvičiaceho. */
data class PartnerKrok(
    val polozka: PlanPolozka,
    val cisloSerie: Int,
    val zoSerii: Int,
)

/** Detský cvik na chvíľu na odpočet („Skúsiš to, kým zapípa?"). */
const val DETSKY_ODPOCET_S = 20

/** Druhý dospelý v tréningu. [otazka] = otázka po sérii z jeho obmedzení (napr. panvové dno). */
data class PartnerStopa(
    val ucastnikId: Long,
    val meno: String,
    val farba: Int,
    val otazka: String?,
)

/**
 * Zápis série druhého dospelého. Kým nezačne ďalšia séria, dá sa upraviť (hodnota, rezerva,
 * príznak) - do denníka ide až potom. Predvolene plán: kto nič nemení, nič nemusí ťukať.
 */
data class PartnerZapis(
    val krok: PartnerKrok,
    val blok: Blok,
    val hodnota: Int,
    val rir: Rir?,
    val priznak: Boolean = false,
)

/** Otázka na príznak tela po sérii hlavného cvičiaceho (séria je už zapísaná, príznak sa dopíše). */
data class OtazkaPriznaku(
    val nazovCviku: String,
    val otazka: String,
    val priznak: Boolean = false,
)

/**
 * DOPLNENIE = posledná séria je odcvičená, ale ešte treba dopísať druhého dospelého
 * alebo odpovedať na otázku príznaku - až potom súhrn.
 */
enum class StavPrehravaca { NACITAVA, CVICENIE, OTAZKA, PAUZA, DOPLNENIE, HOTOVO, PRAZDNY }

/** Bloky, v ktorých sa zapisuje skutočný výkon (inde sa plán zapíše ako skutočnosť). */
private val BLOKY_S_VYKONOM = setOf(Blok.SKILL, Blok.SILA, Blok.KONDICIA)

/** Bloky, v ktorých sa po sérii pýta na rezervu (RIR). */
private val BLOKY_S_REZERVOU = setOf(Blok.SILA, Blok.KONDICIA)

/** Detská vrstva rodinného režimu. */
enum class DetskyKrok { VYBER, CVICI, ODHALENIE, HOTOVO }

data class DetskyStav(
    val dietaId: Long,
    val meno: String,
    val krok: DetskyKrok = DetskyKrok.VYBER,
    /** dve možnosti na výber - voľba je podľa rešerše bezpečný motivátor */
    val moznosti: List<CvikEntity> = emptyList(),
    val aktualny: CvikEntity? = null,
    /** naplnené labky 0..3 */
    val labky: Int = 0,
    /** zviera na odhalenie - identita sa NIKDY neukazuje vopred */
    val odhalene: Zviera? = null,
    val spravene: List<String> = emptyList(),
    /** zviera sa dnes už odhalilo - ďalšie cvičenie („Ešte!") je bez ďalšej odmeny */
    val zvieraDnes: Boolean = false,
)

/** Zmena stupňa v rebríku po tréningu; [vyssie] = postup na ťažší stupeň (dôvod na oslavu). */
data class PrehravacPostup(val text: String, val vyssie: Boolean)

/** Súhrn dokončeného tréningu. Čísla sú z denníka (DB), nie z pamäte prehrávača. */
data class PrehravacSuhrn(
    val trvanieMs: Long,
    val serie: Int,
    val opakovania: Int,
    val sekundyVydrzi: Int,
    val rekordy: List<Rekord>,
    val postupy: List<PrehravacPostup>,
    val partner: SuhrnPartnera? = null,
)

/** Súhrn druhého dospelého - jeho série, rekordy a posuny v rebríkoch. */
data class SuhrnPartnera(
    val meno: String,
    val farba: Int,
    val serie: Int,
    val rekordy: List<Rekord>,
    val postupy: List<PrehravacPostup>,
)

class PrehravacModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _kroky = MutableStateFlow<List<KrokTreningu>>(emptyList())
    val kroky = _kroky.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()

    private val _stav = MutableStateFlow(StavPrehravaca.NACITAVA)
    val stav = _stav.asStateFlow()

    private val _koniecOdpoctu = MutableStateFlow(0L)
    val koniecOdpoctu = _koniecOdpoctu.asStateFlow()

    /** Celková dĺžka bežiaceho odpočtu v ms - menovateľ kruhu (pauza sa dá predĺžiť aj skrátiť). */
    private val _celkomOdpoctu = MutableStateFlow(0L)
    val celkomOdpoctu = _celkomOdpoctu.asStateFlow()

    /** Tréning pozastavený používateľom - odpočet aj hlas stoja. */
    private val _pozastavene = MutableStateFlow(false)
    val pozastavene = _pozastavene.asStateFlow()

    /** Koľko ms odpočtu ostávalo v chvíli pozastavenia (0 = žiadny odpočet nebežal). */
    private val _zostatokPozastaveny = MutableStateFlow(0L)
    val zostatokPozastaveny = _zostatokPozastaveny.asStateFlow()

    /** Začiatok tréningu (System.currentTimeMillis) pre hodiny v hlavičke; 0 = ešte nezačal. */
    private val _zaciatok = MutableStateFlow(0L)
    val zaciatok = _zaciatok.asStateFlow()

    private val _detsky = MutableStateFlow<DetskyStav?>(null)
    val detsky = _detsky.asStateFlow()

    private val _suhrn = MutableStateFlow<PrehravacSuhrn?>(null)
    val suhrn = _suhrn.asStateFlow()

    private val _partner = MutableStateFlow<PartnerStopa?>(null)
    val partner = _partner.asStateFlow()

    /** Rozpracovaný zápis druhého dospelého po sérii (null = nič nečaká). */
    private val _partnerZapis = MutableStateFlow<PartnerZapis?>(null)
    val partnerZapis = _partnerZapis.asStateFlow()

    /** Otázka na príznak po sérii hlavného cvičiaceho (len pri obmedzení, ktoré sa pýta). */
    private val _otazkaPriznaku = MutableStateFlow<OtazkaPriznaku?>(null)
    val otazkaPriznaku = _otazkaPriznaku.asStateFlow()

    val vibracieZapnute get() = repo.prefs.vibracieZapnute
    val obrazovkaStaleZapnuta get() = repo.prefs.obrazovkaStaleZapnuta

    /**
     * Hlas patrí modelu, nie obrazovke: otočenie displeja ho nezruší (inak by sa stratila
     * prvá veta po otočení, kým sa nový TTS rozbehne).
     */
    val hlas = Hlas(app)

    private val _zvuk = MutableStateFlow(repo.prefs.rezimZvuku)
    val zvuk = _zvuk.asStateFlow()
    val hlasZapnuty get() = _zvuk.value.hlas
    val pipanieZapnute get() = _zvuk.value.pipanie

    private val _tvRezim = MutableStateFlow(repo.prefs.tvRezim)
    val tvRezim = _tvRezim.asStateFlow()

    private val _zamokLezato = MutableStateFlow(repo.prefs.zamknutNaLezato)
    val zamokLezato = _zamokLezato.asStateFlow()

    fun prepniZvuk() {
        val novy = _zvuk.value.dalsi()
        _zvuk.value = novy
        repo.prefs.rezimZvuku = novy
        if (!novy.hlas) hlas.stichni()
    }

    fun prepniTv() {
        _tvRezim.value = !_tvRezim.value
        repo.prefs.tvRezim = _tvRezim.value
    }

    fun prepniZamok() {
        _zamokLezato.value = !_zamokLezato.value
        repo.prefs.zamknutNaLezato = _zamokLezato.value
    }

    private var sluzbaBezi = false

    private fun spustiSluzbu() {
        if (sluzbaBezi) return
        sluzbaBezi = true
        TreningService.spusti(getApplication(), "Prebieha tréning")
    }

    private fun zastavSluzbu() {
        if (!sluzbaBezi) return
        sluzbaBezi = false
        TreningService.zastav(getApplication())
    }

    override fun onCleared() {
        zastavSluzbu()
        hlas.zavri()
        super.onCleared()
    }

    private var treningId: Long = -1
    private var spusteny = false
    private var poradieSerie = 0
    private var detskeCviky: List<CvikEntity> = emptyList()

    /** Kto je hlavný cvičiaci tréningu: 0 = ty, inak id dospelého (keď ty dnes necvičíš). */
    private var hlavnyId: Long = 0
    private var otazkaHlavneho: String? = null

    /** Zápis poslednej série hlavného cvičiaceho - id série pre dodatočný príznak. */
    private var poslednyZapisHlavneho: Deferred<Long>? = null

    /** Rozbehnuté zápisy sérií - pred ukončením sa na ne počká, aby vyhodnotenie videlo každú sériu. */
    private val zapisy = mutableListOf<Job>()

    /**
     * [dospelyId] = druhý dospelý, ktorý cvičí popri hlavnom (-1 = nikto).
     * [hlavny] = čí plán vedie tréning: 0 = tvoj, inak dospelého (keď ty dnes necvičíš).
     */
    fun spusti(minuty: Int, dietaId: Long, dospelyId: Long = -1, hlavny: Long = 0) {
        // Poistka: po otočení obrazovky sa LaunchedEffect spustí znova. Tréning sa nesmie založiť
        // druhýkrát - ani počas načítania, ani na súhrne (vtedy je treningId už -1).
        if (spusteny || treningId > 0) return
        spusteny = true
        viewModelScope.launch {
            // Neexistujúci (medzičasom zmazaný) dospelý nesmie viesť tréning - vtedy vedieš ty.
            hlavnyId = if (hlavny > 0 && repo.ucastnik(hlavny) != null) hlavny else 0L
            otazkaHlavneho = repo.obmedzenia(hlavnyId).firstNotNullOfOrNull { it.otazkaPoSerii }
            val plan = repo.zostavPlan(minuty, hlavnyId)

            // Druhý dospelý: vlastný plán (úrovne, ciele, obmedzenia), kroky spárované po blokoch -
            // k-ta séria bloku u hlavného ↔ k-ta séria toho istého bloku u neho. Chýbajúca = v tom kroku nič.
            val druhy = repo.ucastnik(dospelyId)?.takeIf { dospelyId > 0 && dospelyId != hlavnyId }
            val partnerovePodlaBloku: Map<Blok, List<PartnerKrok>> = if (druhy != null) {
                _partner.value = PartnerStopa(
                    ucastnikId = druhy.id,
                    meno = druhy.meno,
                    farba = druhy.farba,
                    otazka = repo.obmedzenia(druhy.id).firstNotNullOfOrNull { it.otazkaPoSerii },
                )
                repo.zostavPlan(minuty, druhy.id).bloky.associate { blok ->
                    blok.blok to Rozvrh.kroky(blok).map { k -> PartnerKrok(k.polozka, k.cisloSerie, k.zoSerii) }
                }
            } else {
                emptyMap()
            }

            // Poradie sérií (páry, kruh) určuje Rozvrh - ten istý, z ktorého ráta odhad času Domov.
            val zoznam = plan.bloky.flatMap { blok ->
                val partnerove = partnerovePodlaBloku[blok.blok].orEmpty()
                Rozvrh.kroky(blok).mapIndexed { i, k ->
                    KrokTreningu(
                        blok = k.blok,
                        polozka = k.polozka,
                        cisloSerie = k.cisloSerie,
                        zoSerii = k.zoSerii,
                        partner = partnerove.getOrNull(i),
                        pauzaPoS = k.pauzaPoS,
                    )
                }
            }
            _kroky.value = zoznam
            if (zoznam.isEmpty()) {
                _stav.value = StavPrehravaca.PRAZDNY
                return@launch
            }

            val rodinny = dietaId > 0
            if (rodinny) {
                val dieta = repo.denik.deti().firstOrNull { it.id == dietaId }
                if (dieta != null) {
                    detskeCviky = repo.katalog.detskeCviky()
                    _detsky.value = DetskyStav(
                        dietaId = dieta.id,
                        meno = dieta.meno,
                        moznosti = vyberDvoch(emptyList()),
                    )
                }
            }
            _zaciatok.value = System.currentTimeMillis()
            treningId = repo.zacniTrening(minuty, rodinny = rodinny)
            spustiSluzbu()
            // Krátke „priprav sa" pred prvým cvikom - stihne sa predstaviť a človek sa postaví.
            zacniPauzu(0, Pravidla.PRIPRAV_SA_S)
        }
    }

    private fun zacniPauzu(dalsiIndex: Int, sekundy: Int) {
        _stav.value = StavPrehravaca.PAUZA
        _celkomOdpoctu.value = sekundy * 1000L
        _koniecOdpoctu.value = SystemClock.elapsedRealtime() + sekundy * 1000L
        _index.value = dalsiIndex
    }

    // ------------------------------------------------------------ dospelá vetva

    private fun pripravKrok(i: Int) {
        // Nová séria začína - čo druhý dospelý zapísal k predošlej, je už definitívne.
        potvrdZapisPartnera()
        _otazkaPriznaku.value = null
        poslednyZapisHlavneho = null
        _index.value = i
        val krok = _kroky.value.getOrNull(i) ?: run {
            _stav.value = StavPrehravaca.HOTOVO
            return
        }
        _stav.value = StavPrehravaca.CVICENIE
        val naCas = krok.jeNaCas()
        val ms = krok.sekundOdpoctu() * 1000L
        _celkomOdpoctu.value = if (naCas) ms else 0L
        _koniecOdpoctu.value = if (naCas) SystemClock.elapsedRealtime() + ms else 0L
    }

    fun hotovoCvicenie() {
        val krok = _kroky.value.getOrNull(_index.value) ?: return
        _koniecOdpoctu.value = 0L
        zacniZapisPartnera(krok)
        if (krok.pytaSaPoSerii()) {
            _stav.value = StavPrehravaca.OTAZKA
        } else {
            zapis(krok, krok.polozka.ciel, null)
            dalej()
        }
    }

    fun odpovedRir(rir: Rir, skutocnaHodnota: Int?) {
        val krok = _kroky.value.getOrNull(_index.value) ?: return
        zapis(krok, skutocnaHodnota ?: krok.polozka.ciel, rir.hodnota)
        dalej()
    }

    fun preskoc() {
        val krok = _kroky.value.getOrNull(_index.value) ?: return
        zapis(krok, null, null, preskocena = true)
        // Vynechaná séria sa vynechá obom - druhý dospelý v nej tiež nič nezapisuje.
        krok.partner?.let { p -> zapisPartnera(PartnerZapis(p, krok.blok, p.polozka.ciel, null), preskocena = true) }
        dalej()
    }

    private fun zapis(krok: KrokTreningu, hodnota: Int?, rir: Int?, preskocena: Boolean = false) {
        val id = treningId
        if (id <= 0) return
        poradieSerie += 1
        val poradie = poradieSerie
        val ucastnik = hlavnyId
        val zapis = viewModelScope.async {
            repo.zapisSeriu(
                treningId = id,
                blok = krok.blok.kluc,
                poradie = poradie,
                cvik = krok.polozka.cvik,
                planHodnota = krok.polozka.ciel,
                skutocnaHodnota = hodnota,
                rir = rir,
                preskocena = preskocena,
                ucastnikId = ucastnik,
            )
        }
        zapisy += zapis
        val otazka = otazkaHlavneho
        if (!preskocena && otazka != null && krok.blok in BLOKY_S_VYKONOM) {
            poslednyZapisHlavneho = zapis
            _otazkaPriznaku.value = OtazkaPriznaku(nazovCviku = krok.polozka.cvik.nazov, otazka = otazka)
        }
    }

    /** Odpoveď hlavného cvičiaceho na otázku príznaku - dopíše sa k už zapísanej sérii. */
    fun odpovedPriznak(priznak: Boolean) {
        val o = _otazkaPriznaku.value ?: return
        if (o.priznak == priznak) return
        _otazkaPriznaku.value = o.copy(priznak = priznak)
        val zapis = poslednyZapisHlavneho ?: return
        zapisy += viewModelScope.launch { repo.nastavPriznak(zapis.await(), priznak) }
    }

    // ------------------------------------------------------------ druhý dospelý

    /** Po sérii hlavného: rozpracuje zápis druhého dospelého. Rozohriatie a mobilita sa zapíšu hneď podľa plánu. */
    private fun zacniZapisPartnera(krok: KrokTreningu) {
        val p = krok.partner ?: return
        if (_partner.value == null) return
        potvrdZapisPartnera()
        val zapis = PartnerZapis(
            krok = p,
            blok = krok.blok,
            hodnota = p.polozka.ciel,
            rir = if (krok.blok in BLOKY_S_REZERVOU) Rir.JEDNO_DVE else null,
        )
        if (krok.blok in BLOKY_S_VYKONOM) _partnerZapis.value = zapis else zapisPartnera(zapis)
    }

    fun upravPartnera(zmena: (PartnerZapis) -> PartnerZapis) {
        val z = _partnerZapis.value ?: return
        _partnerZapis.value = zmena(z)
    }

    private fun potvrdZapisPartnera() {
        val z = _partnerZapis.value ?: return
        _partnerZapis.value = null
        zapisPartnera(z)
    }

    private fun zapisPartnera(z: PartnerZapis, preskocena: Boolean = false) {
        val p = _partner.value ?: return
        val id = treningId
        if (id <= 0) return
        poradieSerie += 1
        val poradie = poradieSerie
        zapisy += viewModelScope.launch {
            repo.zapisSeriu(
                treningId = id,
                blok = z.blok.kluc,
                poradie = poradie,
                cvik = z.krok.polozka.cvik,
                planHodnota = z.krok.polozka.ciel,
                skutocnaHodnota = if (preskocena) null else z.hodnota,
                rir = z.rir?.hodnota,
                preskocena = preskocena,
                ucastnikId = p.ucastnikId,
                priznak = z.priznak,
            )
        }
    }

    /** Doplnenie po poslednej sérii je hotové - až teraz súhrn. */
    fun doplnenieHotove() {
        if (_stav.value == StavPrehravaca.DOPLNENIE) ukonci()
    }

    private fun dalej() {
        val dalsiIndex = _index.value + 1
        val krok = _kroky.value.getOrNull(_index.value)
        val dalsi = _kroky.value.getOrNull(dalsiIndex)
        if (dalsi == null) {
            // Posledná séria: ak ešte niečo čaká na doplnenie, súhrn počká.
            if (_partnerZapis.value != null || _otazkaPriznaku.value != null) {
                _koniecOdpoctu.value = 0L
                _stav.value = StavPrehravaca.DOPLNENIE
            } else {
                ukonci()
            }
            return
        }
        val pauza = krok?.pauzaPoS ?: 0
        if (pauza > 0) zacniPauzu(dalsiIndex, pauza) else pripravKrok(dalsiIndex)
    }

    fun pauzaSkoncila() {
        _koniecOdpoctu.value = 0L
        pripravKrok(_index.value)
    }

    /**
     * −15 s / +15 s v pauze medzi sériami. Skrátenie pod nulu pauzu ukončí - odpočet to zistí sám,
     * zapípa a pokračuje ako pri bežnom konci pauzy.
     */
    fun upravPauzu(deltaS: Int) {
        if (_stav.value != StavPrehravaca.PAUZA || _pozastavene.value) return
        val koniec = _koniecOdpoctu.value
        if (koniec <= 0L) return
        val teraz = SystemClock.elapsedRealtime()
        val novyKoniec = (koniec + deltaS * 1000L).coerceAtLeast(teraz)
        _celkomOdpoctu.value = (_celkomOdpoctu.value + deltaS * 1000L)
            .coerceAtLeast(novyKoniec - teraz)
            .coerceAtLeast(1L)
        _koniecOdpoctu.value = novyKoniec
    }

    /** Pozastaví tréning: odpočet zamrzne na zostávajúcom čase, nič neubúda. */
    fun pozastav() {
        if (_pozastavene.value) return
        val s = _stav.value
        if (s != StavPrehravaca.CVICENIE && s != StavPrehravaca.OTAZKA && s != StavPrehravaca.PAUZA &&
            s != StavPrehravaca.DOPLNENIE
        ) return
        val koniec = _koniecOdpoctu.value
        _zostatokPozastaveny.value = if (koniec > 0L) {
            (koniec - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        } else {
            0L
        }
        _pozastavene.value = true
    }

    /** Pokračovanie po pozastavení - odpočet beží od zamrznutého zostatku. */
    fun pokracuj() {
        if (!_pozastavene.value) return
        if (_koniecOdpoctu.value > 0L) {
            _koniecOdpoctu.value = SystemClock.elapsedRealtime() + _zostatokPozastaveny.value
        }
        _zostatokPozastaveny.value = 0L
        _pozastavene.value = false
    }

    fun skrat() {
        val zvysok = _kroky.value.drop(_index.value)
        val skratene = zvysok.groupBy { it.polozka.cvik.id }.values.mapNotNull { it.firstOrNull() }
        _kroky.value = _kroky.value.take(_index.value) + skratene
        pripravKrok(_index.value)
    }

    fun ukonci() {
        val id = treningId
        // Rozpracovaný zápis druhého dospelého sa pri ukončení neztratí.
        potvrdZapisPartnera()
        _otazkaPriznaku.value = null
        zastavSluzbu()
        _pozastavene.value = false
        _zostatokPozastaveny.value = 0L
        _stav.value = StavPrehravaca.HOTOVO
        if (id <= 0) return
        val d = _detsky.value
        val druhy = _partner.value
        val hlavny = hlavnyId
        val cakajuceZapisy = zapisy.toList()
        viewModelScope.launch {
            // Posledná séria sa mohla ešte zapisovať - vyhodnotenie postupov ju musí vidieť.
            cakajuceZapisy.joinAll()
            // Len čítanie: stupne PRED vyhodnotením, aby súhrn vedel povedať smer (vyššie / nižšie).
            val urovnePred = repo.urovne(hlavny)
            val urovnePredPartnera = druhy?.let { repo.urovne(it.ucastnikId) }.orEmpty()
            // Postupy sa vyhodnotia JEDINÝ raz, v ukonciTrening (druhé volanie posúvalo o ďalší stupeň).
            val vsetkyPostupy = repo.ukonciTrening(id, pocitPo = null, zachovajPrazdny = d != null && d.labky > 0)
            if (d != null) {
                repo.denik.ulozUcast(
                    UcastEntity(treningId = id, dietaId = d.dietaId, splneneKroky = d.labky)
                )
            }
            suspend fun popisPostupov(ucastnikId: Long, pred: Map<String, Int>, tykanie: Boolean) =
                vsetkyPostupy.filter { it.ucastnikId == ucastnikId }.mapNotNull { p ->
                    val rebrikId = p.rebrikId
                    val uroven = p.uroven
                    val r = repo.katalog.rebrik(rebrikId) ?: return@mapNotNull null
                    val cvik = repo.katalog.cvik(rebrikId, uroven)
                    val vyssie = uroven > (pred[rebrikId] ?: 1)
                    val veta = when {
                        tykanie && vyssie -> "postupuješ na stupeň $uroven"
                        tykanie -> "vraciaš sa na stupeň $uroven"
                        vyssie -> "postup na stupeň $uroven"
                        else -> "návrat na stupeň $uroven"
                    }
                    PrehravacPostup(
                        text = "${r.nazov}: $veta" + (cvik?.let { " — ${it.nazov}" } ?: ""),
                        vyssie = vyssie,
                    )
                }
            val postupy = popisPostupov(hlavny, urovnePred, tykanie = true)

            // Série hlavného cvičiaceho - rovnako ako postupy a rekordy.
            val vsetkySerie = repo.denik.serieTreningu(id)
            val odcvicene = vsetkySerie
                .filter { !it.preskocena && it.skutocnaHodnota != null && it.ucastnikId == hlavny }
            val trening = repo.denik.trening(id)
            val trvanie = trening?.let { (it.koniec ?: System.currentTimeMillis()) - it.zaciatok } ?: 0L
            // Rekordy sú len ozdoba súhrnu - chyba pri ich výpočte nesmie zhodiť koniec tréningu.
            val rekordy = runCatching { repo.rekordyTreningu(id, hlavny) }.getOrDefault(emptyList())
            val suhrnPartnera = druhy?.let { p ->
                SuhrnPartnera(
                    meno = p.meno,
                    farba = p.farba,
                    serie = vsetkySerie.count { !it.preskocena && it.skutocnaHodnota != null && it.ucastnikId == p.ucastnikId },
                    rekordy = runCatching { repo.rekordyTreningu(id, p.ucastnikId) }.getOrDefault(emptyList()),
                    postupy = popisPostupov(p.ucastnikId, urovnePredPartnera, tykanie = false),
                )
            }
            _suhrn.value = PrehravacSuhrn(
                trvanieMs = trvanie.coerceAtLeast(0L),
                serie = odcvicene.size,
                opakovania = odcvicene
                    .filter { Meranie.z(it.meranie) == Meranie.OPAKOVANIA }
                    .sumOf { it.skutocnaHodnota ?: 0 },
                sekundyVydrzi = odcvicene
                    .filter { Meranie.z(it.meranie) == Meranie.VYDRZ }
                    .sumOf { it.skutocnaHodnota ?: 0 },
                rekordy = rekordy,
                postupy = postupy,
                partner = suhrnPartnera?.takeIf { it.serie > 0 || it.postupy.isNotEmpty() },
            )
        }
        treningId = -1
    }

    // ------------------------------------------------------------ detská vetva

    private fun vyberDvoch(uzSpravene: List<String>): List<CvikEntity> {
        val kandidati = detskeCviky.filter { it.id !in uzSpravene }
        return (kandidati.ifEmpty { detskeCviky }).shuffled().take(2)
    }

    fun dietaVybralo(cvik: CvikEntity) {
        _detsky.value = _detsky.value?.copy(krok = DetskyKrok.CVICI, aktualny = cvik)
    }

    /**
     * Dieťa cvik spravilo - labka pribúda za POKUS, nie za výkon.
     * Po tretej labke sa odomkne zviera JEDNÉHO z cvikov, ktoré dieťa reálne robilo -
     * náhodné a vopred neprezradené (Lepperov efekt: očakávaná odmena škodí, nečakaná nie).
     */
    fun dietaSpravilo() {
        val d = _detsky.value ?: return
        val cvik = d.aktualny ?: return
        val spravene = d.spravene + cvik.id
        // Labky sa počítajú aj po treťej („Ešte!"), zviera je však len jedno na tréning.
        val labky = d.labky + 1

        if (labky >= 3 && !d.zvieraDnes) {
            viewModelScope.launch {
                val uzMa = repo.denik.zvierataDietata(d.dietaId).map { it.zvieraKluc }.toSet()
                val kandidati = spravene
                    .mapNotNull { id -> detskeCviky.firstOrNull { it.id == id }?.zviera }
                    .filter { it !in uzMa }
                    .distinct()
                val zviera = kandidati.shuffled().firstOrNull()?.let { Zvierata.podla(it) }
                if (zviera != null) repo.odomkniZviera(d.dietaId, zviera.kluc)
                _detsky.value = d.copy(
                    krok = if (zviera != null) DetskyKrok.ODHALENIE else DetskyKrok.HOTOVO,
                    labky = labky,
                    spravene = spravene,
                    odhalene = zviera,
                    aktualny = null,
                    zvieraDnes = true,
                )
            }
        } else {
            _detsky.value = d.copy(
                krok = DetskyKrok.VYBER,
                labky = labky,
                spravene = spravene,
                moznosti = vyberDvoch(spravene),
                aktualny = null,
            )
        }
    }

    fun odhalenieVidene() {
        _detsky.value = _detsky.value?.copy(krok = DetskyKrok.HOTOVO, odhalene = null)
    }

    /** „Ešte!" - dieťa chce cvičiť ďalej. Labky pribúdajú, ďalšie zviera dnes už nie. */
    fun dietaPokracuje() {
        val d = _detsky.value ?: return
        _detsky.value = d.copy(krok = DetskyKrok.VYBER, moznosti = vyberDvoch(d.spravene), aktualny = null)
    }
}

// ================================================================ obrazovka

/** Čo obrazovka práve ukazuje - kľúč prechodu (iný kľúč = iný krok = animovaný prechod). */
private data class PrehravacKluc(
    val stav: StavPrehravaca,
    val index: Int,
    val cvikId: String?,
    val seria: Int,
)

/** Všetky premenlivé údaje kroku naraz - odchádzajúci krok si ich počas animácie drží zmrazené. */
private data class PrehravacObraz(
    val stav: StavPrehravaca,
    val index: Int,
    val krok: KrokTreningu?,
    val koniec: Long,
    val celkom: Long,
    val pozastavene: Boolean,
    val zostatokPozastaveny: Long,
    val detsky: DetskyStav?,
    val suhrn: PrehravacSuhrn?,
    val partner: PartnerStopa?,
    val partnerZapis: PartnerZapis?,
    val otazkaPriznaku: OtazkaPriznaku?,
)

private class PrehravacDrzak<T>(var hodnota: T)

private fun KrokTreningu.jeVydrz() = Meranie.z(polozka.cvik.meranie) == Meranie.VYDRZ

/** Na ležato (zrkadlenie na TV) sa obrazovky prehrávača skladajú do dvoch stĺpcov. */
private val LocalNaLezato = compositionLocalOf { false }

/** Od akej dĺžky sa v pauze/výdrži hovorí „polovica" a predstavuje ďalší cvik (kratšie = hneď). */
private const val POLOVICA_PAUZY_OD_MS = 16_000L

/**
 * Akcia z obsahu, ktorý práve odchádza (beží animácia prechodu), sa ignoruje - inak by dvojité
 * ťuknutie na „Hotovo" zapísalo sériu dvakrát alebo preskočilo nasledujúci krok.
 */
private fun PrehravacModel.akAktualny(k: PrehravacKluc, akcia: () -> Unit) {
    if (stav.value == k.stav && index.value == k.index) akcia()
}

@Composable
fun PrehravacObrazovka(
    minuty: Int,
    dietaId: Long,
    hotovo: () -> Unit,
    dospelyId: Long = -1,
    hlavnyId: Long = 0,
    model: PrehravacModel = viewModel(),
) {
    val kontext = LocalContext.current
    val pohlad = LocalView.current
    val stav by model.stav.collectAsStateWithLifecycle()
    val kroky by model.kroky.collectAsStateWithLifecycle()
    val index by model.index.collectAsStateWithLifecycle()
    val koniec by model.koniecOdpoctu.collectAsStateWithLifecycle()
    val celkom by model.celkomOdpoctu.collectAsStateWithLifecycle()
    val pozastavene by model.pozastavene.collectAsStateWithLifecycle()
    val zostatokPozastaveny by model.zostatokPozastaveny.collectAsStateWithLifecycle()
    val zaciatok by model.zaciatok.collectAsStateWithLifecycle()
    val detsky by model.detsky.collectAsStateWithLifecycle()
    val suhrn by model.suhrn.collectAsStateWithLifecycle()
    val partner by model.partner.collectAsStateWithLifecycle()
    val partnerZapis by model.partnerZapis.collectAsStateWithLifecycle()
    val otazkaPriznaku by model.otazkaPriznaku.collectAsStateWithLifecycle()

    val zvuk by model.zvuk.collectAsStateWithLifecycle()
    val tvRezim by model.tvRezim.collectAsStateWithLifecycle()
    val zamokLezato by model.zamokLezato.collectAsStateWithLifecycle()

    // Hlas aj služba patria modelu - otočenie displeja ich nesmie zastaviť (pád 0.7 po otočení).
    val hlas = model.hlas

    LaunchedEffect(minuty, dietaId, dospelyId, hlavnyId) { model.spusti(minuty, dietaId, dospelyId, hlavnyId) }

    DisposableEffect(Unit) {
        if (model.obrazovkaStaleZapnuta) pohlad.keepScreenOn = true
        onDispose { pohlad.keepScreenOn = false }
    }

    // Zámok na ležato (zrkadlenie na TV, telefón leží) - len kým je otvorený prehrávač.
    val aktivita = LocalActivity.current
    DisposableEffect(zamokLezato, aktivita) {
        aktivita?.requestedOrientation = if (zamokLezato) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose { aktivita?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    val krok = kroky.getOrNull(index)
    val vTreningu = kroky.isNotEmpty() &&
        (stav == StavPrehravaca.CVICENIE || stav == StavPrehravaca.OTAZKA || stav == StavPrehravaca.PAUZA ||
            stav == StavPrehravaca.DOPLNENIE)

    // Hlas pri zmene cviku - názov + dávka. Bez pozerania na displej.
    LaunchedEffect(stav, index) {
        val k = kroky.getOrNull(index) ?: return@LaunchedEffect
        when (stav) {
            StavPrehravaca.CVICENIE -> {
                val davka = if (k.jeNaCas()) "${k.sekundOdpoctu()} sekúnd" else "${k.polozka.ciel} opakovaní"
                val seria = if (k.zoSerii > 1) {
                    if (k.blok == Blok.KRUH) "Kolo ${k.cisloSerie} z ${k.zoSerii}. " else "Séria ${k.cisloSerie} z ${k.zoSerii}. "
                } else {
                    ""
                }
                // Druhý dospelý počuje svoj cvik hneď za tvojím - ak má iný.
                val druhy = model.partner.value?.let { p ->
                    k.partner?.polozka?.cvik?.takeIf { it.id != k.polozka.cvik.id }?.let { " ${p.meno}: ${it.nazov}." }
                }.orEmpty()
                hlas.povedz("$seria${k.polozka.cvik.nazov}. $davka.$druhy", model.hlasZapnuty)
            }
            StavPrehravaca.PAUZA -> {
                val dalsi = kroky.getOrNull(index)
                // Krátka pauza (priprav sa, prechod v kruhu): ďalší cvik hneď. Dlhá: najprv oddych,
                // ďalší cvik sa predstaví v polovici (odpočet nižšie), aby nerušil hneď po sérii.
                val kratka = model.celkomOdpoctu.value < POLOVICA_PAUZY_OD_MS
                hlas.povedz(
                    if (kratka) "Priprav sa: ${dalsi?.polozka?.cvik?.nazov ?: ""}." else "Pauza.",
                    model.hlasZapnuty,
                )
            }
            StavPrehravaca.HOTOVO -> {
                hlas.povedz("Hotovo. Dobrá práca.", model.hlasZapnuty)
                hlas.zavibruj(model.vibracieZapnute, dlho = true)
            }
            else -> {}
        }
    }

    // Odpočet výdrže aj pauzy (pípanie 3-2-1, záverečný tón, vibrácia, posun ďalej).
    // Beží TU, mimo animovaného obsahu: počas prechodu sú na obrazovke dva kroky naraz a odpočet
    // nesmie bežať dvakrát. Je viazaný na elapsedRealtime (neuteká) a na delay, nie na snímky -
    // dobehne aj pri zhasnutej obrazovke. Pri pozastavení stojí.
    LaunchedEffect(stav, index, koniec, pozastavene) {
        if (pozastavene || koniec <= 0L) return@LaunchedEffect
        // Hodnoty v chvíli spustenia - stav sa počas odpočtu môže zmeniť skôr, než sa efekt zruší.
        val stavOdpoctu = stav
        val koniecOdpoctu = koniec
        val k = kroky.getOrNull(index) ?: return@LaunchedEffect
        val vydrz = stavOdpoctu == StavPrehravaca.CVICENIE && k.jeNaCas()
        if (!vydrz && stavOdpoctu != StavPrehravaca.PAUZA) return@LaunchedEffect
        var poslednePipnutie = -1
        val celkomMs = model.celkomOdpoctu.value
        // Po pozastavení sa už povedané vety nezopakujú: platí, len ak je ešte pred nimi.
        var polovicaPovedana = koniecOdpoctu - SystemClock.elapsedRealtime() <= celkomMs / 2
        var piatPovedane = koniecOdpoctu - SystemClock.elapsedRealtime() <= 5_500
        while (true) {
            // Pozastavenie zastaví odpočet hneď, nie až pri ďalšom prekreslení.
            if (model.pozastavene.value) return@LaunchedEffect
            val ms = koniecOdpoctu - SystemClock.elapsedRealtime()
            val s = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
            if (s in 1..3 && s != poslednePipnutie) {
                poslednePipnutie = s
                hlas.pipni(model.pipanieZapnute)
            }
            if (!polovicaPovedana && ms <= celkomMs / 2 && celkomMs >= POLOVICA_PAUZY_OD_MS) {
                polovicaPovedana = true
                if (vydrz) {
                    hlas.povedz("Polovica.", model.hlasZapnuty)
                } else {
                    // Predstavenie ďalšieho cviku v polovici pauzy: názov + prvý pokyn.
                    val pokyn = k.polozka.cvik.cue.split("|").firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                    hlas.povedz("Ďalej ${k.polozka.cvik.nazov}. $pokyn", model.hlasZapnuty)
                }
            }
            if (vydrz && !piatPovedane && ms <= 5_500 && celkomMs >= 15_000) {
                piatPovedane = true
                hlas.povedz("Ešte päť.", model.hlasZapnuty)
            }
            if (ms <= 0) break
            delay(50)
        }
        hlas.pipniKoniec(model.pipanieZapnute)
        hlas.zavibruj(model.vibracieZapnute)
        if (stavOdpoctu == StavPrehravaca.PAUZA) model.pauzaSkoncila() else model.hotovoCvicenie()
    }

    var potvrdenieKonca by rememberSaveable { mutableStateOf(false) }
    var pozastaveneUzPredDialogom by rememberSaveable { mutableStateOf(false) }

    val pozastav: () -> Unit = {
        model.pozastav()
        // QUEUE_FLUSH - utne rozhovorenú vetu, potom hlas mlčí, kým sa nepokračuje.
        hlas.povedz("Pozastavené.", model.hlasZapnuty)
    }
    val pokracuj: () -> Unit = {
        model.pokracuj()
        hlas.povedz("Pokračujeme.", model.hlasZapnuty)
    }
    val pytajSaNaKoniec: () -> Unit = {
        pozastaveneUzPredDialogom = model.pozastavene.value
        // Kým sa rozhoduje, nič neubieha - odpočet nesmie medzitým preskočiť na ďalší krok.
        model.pozastav()
        potvrdenieKonca = true
    }
    val zavriPotvrdenie: () -> Unit = {
        potvrdenieKonca = false
        if (!pozastaveneUzPredDialogom) model.pokracuj()
    }

    // Deti nevedia čítať: appka im prečíta možnosti, zvolený cvik a pochvalu. Za tvojou vetou, nie cez ňu.
    val detskyKrok = detsky?.krok
    val detskyCvik = detsky?.aktualny?.id
    val detskeMoznosti = detsky?.moznosti?.map { it.id }
    LaunchedEffect(detskyKrok, detskyCvik, detskeMoznosti) {
        val d = model.detsky.value ?: return@LaunchedEffect
        val veta = when (d.krok) {
            DetskyKrok.VYBER -> {
                val mena = d.moznosti.map { c -> c.zviera?.let { Zvierata.podla(it) }?.meno ?: c.nazov }
                // Po spravenom cviku najprv pochvala za labku, potom ďalší výber.
                val pochvala = if (d.spravene.isNotEmpty()) "Labka je tvoja! " else ""
                if (mena.size == 2) "$pochvala${d.meno}, vyber si: ${mena[0]}, alebo ${mena[1]}?" else null
            }
            DetskyKrok.CVICI -> d.aktualny?.let { c ->
                val pokyn = c.cue.split("|").firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                "${c.nazov}. $pokyn"
            }
            DetskyKrok.ODHALENIE -> d.odhalene?.let { "Pozri, ${d.meno}! ${it.veta}" }
            DetskyKrok.HOTOVO -> if (d.labky > 0) "Super, ${d.meno}! Labka je tvoja." else null
        }
        if (veta != null) hlas.povedzPotom(veta, model.hlasZapnuty)
    }

    // Systémové „späť" počas tréningu neodíde potichu - spýta sa ako tlačidlo Ukončiť.
    BackHandler(enabled = vTreningu && !potvrdenieKonca) { pytajSaNaKoniec() }

    val obraz = PrehravacObraz(
        stav = stav,
        index = index,
        krok = krok,
        koniec = koniec,
        celkom = celkom,
        pozastavene = pozastavene,
        zostatokPozastaveny = zostatokPozastaveny,
        detsky = detsky,
        suhrn = suhrn,
        partner = partner,
        partnerZapis = partnerZapis,
        otazkaPriznaku = otazkaPriznaku,
    )
    val kluc = PrehravacKluc(stav, index, krok?.polozka?.cvik?.id, krok?.cisloSerie ?: 0)

    // TV režim: všetko o štvrtinu väčšie (čitateľné z gauča). Na ležato dva stĺpce.
    val hustota = LocalDensity.current
    val tvHustota = remember(hustota, tvRezim) {
        if (tvRezim) Density(hustota.density * 1.25f, hustota.fontScale) else hustota
    }
    val detskyPanel = detsky != null && vTreningu
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val naLezato = maxWidth > maxHeight
    CompositionLocalProvider(LocalDensity provides tvHustota, LocalNaLezato provides naLezato) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (naLezato) 16.dp else 20.dp, vertical = if (naLezato) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = vTreningu,
                enter = fadeIn(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Hlavicka(
                    kroky = kroky,
                    index = index,
                    obraz = obraz,
                    zaciatok = zaciatok,
                    naPozastavenie = pozastav,
                    zvuk = zvuk,
                    naZvuk = model::prepniZvuk,
                    tvRezim = tvRezim,
                    naTv = model::prepniTv,
                    zamokLezato = zamokLezato,
                    naZamok = model::prepniZamok,
                )
            }

            Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = kluc,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (targetState.stav == StavPrehravaca.HOTOVO) {
                            (fadeIn(tween(380)) + scaleIn(animationSpec = tween(380), initialScale = 0.94f)) togetherWith
                                fadeOut(tween(200))
                        } else {
                            (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(220)))
                        }
                    },
                    contentAlignment = Alignment.Center,
                    label = "krokPrehravaca",
                ) { k ->
                    val o = zmrazMimoAktualneho(k == kluc, obraz)
                    when (o.stav) {
                        StavPrehravaca.NACITAVA -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }

                        StavPrehravaca.PRAZDNY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PrazdnyStav(
                                titulok = "Dnes to nepôjde",
                                text = "Zatiaľ ti neviem poskladať tréning. Skontroluj ciele a náradie v nastaveniach.",
                                tlacidlo = "Späť",
                                akcia = hotovo,
                            )
                        }

                        StavPrehravaca.CVICENIE -> {
                            val kr = o.krok
                            if (kr != null) {
                                if (kr.jeNaCas()) {
                                    CvicenieVydrz(kr, o)
                                } else {
                                    CvicenieOpakovania(
                                        krok = kr,
                                        partner = o.partner,
                                        naHotovo = {
                                            model.akAktualny(k) {
                                                hlas.zavibruj(true)
                                                model.hotovoCvicenie()
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        StavPrehravaca.OTAZKA -> {
                            val kr = o.krok
                            if (kr != null) {
                                OtazkaRezervy(kr) { rir, hodnota ->
                                    model.akAktualny(k) { model.odpovedRir(rir, hodnota) }
                                }
                            }
                        }

                        StavPrehravaca.PAUZA -> Pauza(
                            o = o,
                            model = model,
                            naUpravu = { delta -> model.akAktualny(k) { model.upravPauzu(delta) } },
                            naPreskocenie = { model.akAktualny(k) { model.pauzaSkoncila() } },
                        )

                        StavPrehravaca.DOPLNENIE -> Doplnenie(
                            o = o,
                            model = model,
                            naHotovo = { model.akAktualny(k) { model.doplnenieHotove() } },
                        )

                        StavPrehravaca.HOTOVO -> Zaver(o.detsky, o.suhrn, hotovo)
                    }
                }
            }
            // Na ležato majú deti vlastný stĺpec vpravo - cvičia počas celého tréningu.
            val d = detsky
            if (detskyPanel && naLezato && d != null) {
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) { DetskyPruh(d, model) }
            }
            }

            // Na výšku je detský pruh kompaktný pod obsahom - nezaberie tvoju sériu.
            val dPruh = detsky
            if (detskyPanel && !naLezato && dPruh != null) {
                Spacer(Modifier.height(8.dp))
                DetskyPruhKompaktny(dPruh, model)
            }

            AnimatedVisibility(
                visible = vTreningu && (stav == StavPrehravaca.CVICENIE || stav == StavPrehravaca.PAUZA),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SpodneOvladanie(
                    naVynechanie = model::preskoc,
                    naSkratenie = model::skrat,
                    naUkoncenie = pytajSaNaKoniec,
                )
            }
        }

        // Odhalené zviera - konfety cez celú obrazovku (v karte by ich orezal jej tvar).
        val odhalene = detsky?.odhalene
        Konfety(
            spustit = detsky?.krok == DetskyKrok.ODHALENIE && odhalene != null,
            modifier = Modifier.fillMaxSize(),
            kluc = odhalene?.kluc ?: Unit,
        )

        AnimatedVisibility(
            visible = pozastavene && vTreningu,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            PozastaveneVrstva(
                zostatokMs = zostatokPozastaveny,
                vPauze = stav == StavPrehravaca.PAUZA,
                naPokracovanie = pokracuj,
                naUkoncenie = pytajSaNaKoniec,
            )
        }
    }
    }
    }

    if (potvrdenieKonca) {
        AlertDialog(
            onDismissRequest = zavriPotvrdenie,
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
            title = { Text("Ukončiť tréning?") },
            text = {
                Text("Odcvičené série ostanú zapísané a ukážem ti súhrn. Zvyšok dnešného plánu sa vynechá.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        potvrdenieKonca = false
                        model.ukonci()
                    },
                ) { Text("Ukončiť") }
            },
            dismissButton = {
                TextButton(onClick = zavriPotvrdenie) { Text("Pokračovať") }
            },
        )
    }
}

/** Kým je obsah aktuálny, vracia živú hodnotu; počas odchodu (animácia) poslednú živú - zmrazenú. */
@Composable
private fun <T> zmrazMimoAktualneho(aktualny: Boolean, hodnota: T): T {
    val drzak = remember { PrehravacDrzak(hodnota) }
    if (aktualny) drzak.hodnota = hodnota
    return drzak.hodnota
}

// ---------------------------------------------------------------- hlavička

@Composable
private fun Hlavicka(
    kroky: List<KrokTreningu>,
    index: Int,
    obraz: PrehravacObraz,
    zaciatok: Long,
    naPozastavenie: () -> Unit,
    zvuk: RezimZvuku,
    naZvuk: () -> Unit,
    tvRezim: Boolean,
    naTv: () -> Unit,
    zamokLezato: Boolean,
    naZamok: () -> Unit,
) {
    val krok = kroky.getOrNull(index)
    val farbaBloku = krok?.let { FarbyBlokov.pre(it.blok) } ?: MaterialTheme.colorScheme.primary
    val haptika = rememberHaptika()
    val naLezato = LocalNaLezato.current
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = if (naLezato) 4.dp else 8.dp)
    ) {
        PruhTreningu(kroky, index, obraz)
        Spacer(Modifier.height(if (naLezato) 4.dp else 10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Farebná bodka bloku - tréning sa dá čítať podľa farby aj z diaľky.
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(farbaBloku)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Krok ${index + 1} / ${kroky.size}" + (krok?.let { " · ${it.blok.nazov}" } ?: ""),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            UplynulyCas(zaciatok)
            Spacer(Modifier.width(4.dp))
            // Reproduktor: hlas + pípanie → len pípanie → ticho (napr. keď hrá vlastná hudba).
            IconButton(
                onClick = {
                    haptika.klik()
                    naZvuk()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    when (zvuk) {
                        RezimZvuku.HLAS -> Icons.AutoMirrored.Rounded.VolumeUp
                        RezimZvuku.PIPANIE -> Icons.Rounded.NotificationsActive
                        RezimZvuku.TICHO -> Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    contentDescription = "Zvuk: ${zvuk.popis}. Ťuknutím prepneš.",
                )
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Ďalšie možnosti")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Režim TV (väčšie písmo)") },
                        leadingIcon = { Icon(Icons.Rounded.Tv, contentDescription = null) },
                        trailingIcon = { if (tvRezim) Icon(Icons.Rounded.Check, contentDescription = "zapnuté") },
                        onClick = { menu = false; naTv() },
                    )
                    DropdownMenuItem(
                        text = { Text("Zamknúť na ležato") },
                        leadingIcon = { Icon(Icons.Rounded.ScreenLockLandscape, contentDescription = null) },
                        trailingIcon = { if (zamokLezato) Icon(Icons.Rounded.Check, contentDescription = "zapnuté") },
                        onClick = { menu = false; naZamok() },
                    )
                }
            }
            FilledTonalIconButton(
                onClick = {
                    haptika.klik()
                    naPozastavenie()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.Pause, contentDescription = "Pozastaviť tréning")
            }
        }
    }
}

/** Pruh celého tréningu; aktuálny segment sa pri výdrži plní plynulo s odpočtom. */
@Composable
private fun PruhTreningu(kroky: List<KrokTreningu>, index: Int, obraz: PrehravacObraz) {
    val farby = remember(kroky) { kroky.map { FarbyBlokov.pre(it.blok) } }
    val vydrz = obraz.stav == StavPrehravaca.CVICENIE && obraz.koniec > 0L && obraz.celkom > 0L
    val zostatok = rememberZostatokMs(
        koniec = if (vydrz) obraz.koniec else 0L,
        pozastavene = obraz.pozastavene,
        zostatokPozastaveny = if (vydrz) obraz.zostatokPozastaveny else 0L,
    )
    val postup = when {
        vydrz -> 1f - zostatok.toFloat() / obraz.celkom.toFloat()
        obraz.stav == StavPrehravaca.OTAZKA || obraz.stav == StavPrehravaca.DOPLNENIE -> 1f
        else -> 0f
    }
    SegmentovyPruh(
        farby = farby,
        aktualny = index,
        postupAktualneho = postup,
        vyska = 8.dp,
        medzera = if (kroky.size > 28) 2.dp else 3.dp,
    )
}

/** Uplynulý čas tréningu „12:34" - tiká po sekundách od začiatku tréningu. */
@Composable
private fun UplynulyCas(zaciatok: Long) {
    val teraz by produceState(System.currentTimeMillis(), zaciatok) {
        while (true) {
            value = System.currentTimeMillis()
            val uplynulo = if (zaciatok > 0L) value - zaciatok else value
            delay(1000L - (uplynulo % 1000L).coerceIn(0L, 999L))
        }
    }
    Text(
        formatujStopky(if (zaciatok > 0L) teraz - zaciatok else 0L),
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------- odpočet

/**
 * Zostávajúce ms odpočtu, prepočítané na každej snímke - kruh tak tečie, neposkakuje po sekundách.
 * Len na zobrazenie: logika odpočtu (pípanie, posun) beží zvlášť a nezávisí od snímok.
 */
@Composable
private fun rememberZostatokMs(koniec: Long, pozastavene: Boolean, zostatokPozastaveny: Long): Long {
    val zostatok by produceState(
        vypocitajZostatok(koniec, pozastavene, zostatokPozastaveny),
        koniec,
        pozastavene,
        zostatokPozastaveny,
    ) {
        value = vypocitajZostatok(koniec, pozastavene, zostatokPozastaveny)
        if (pozastavene || koniec <= 0L) return@produceState
        while (value > 0L) {
            withFrameMillis { }
            value = vypocitajZostatok(koniec, false, 0L)
        }
    }
    return zostatok
}

private fun vypocitajZostatok(koniec: Long, pozastavene: Boolean, zostatokPozastaveny: Long): Long = when {
    pozastavene -> zostatokPozastaveny
    koniec <= 0L -> 0L
    else -> (koniec - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
}

/** Číslo v strede kruhu. Posledné 3 sekundy zmení farbu - signál „už len chvíľu". */
@Composable
private fun CisloOdpoctu(zostatokMs: Long, pauza: Boolean) {
    val sekundy = ((zostatokMs + 999L) / 1000L).toInt()
    val farbaCisla by animateColorAsState(
        targetValue = if (sekundy in 1..3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "farbaOdpoctu",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (sekundy >= 60) {
            Text(
                formatujStopky(sekundy * 1000L),
                style = StylVelkeCislo.copy(fontSize = 72.sp),
                color = farbaCisla,
            )
        } else {
            AnimovaneCislo(hodnota = sekundy, style = StylVelkeCislo, color = farbaCisla)
        }
        Text(
            when {
                pauza -> "pauza"
                sekundy >= 60 -> "výdrž"
                else -> sklonuj(sekundy, "sekunda", "sekundy", "sekúnd")
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------- cvičenie

/** Stred obrazovky: obsah zvisle vycentrovaný, a keď sa nezmestí (malý displej, veľké písmo), roluje. */
@Composable
private fun RolovatelnyStred(modifier: Modifier = Modifier, obsah: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        val minVyska = if (constraints.hasBoundedHeight) maxHeight else 0.dp
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minVyska),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = obsah,
        )
    }
}

/** Postavička v tónovanom kruhu vo farbe bloku - vizuálna kotva obrazovky. */
@Composable
private fun KruhPostavicky(rebrikId: String, farba: Color, velkost: Dp, pohyb: String? = null) {
    Box(
        modifier = Modifier
            .size(velkost)
            .clip(CircleShape)
            .background(farba.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Postavicka(
            rebrikId = rebrikId,
            pohyb = pohyb,
            modifier = Modifier.size(velkost * 0.8f),
            farba = MaterialTheme.colorScheme.onSurface,
            farbaAkcent = farba,
        )
    }
}

/** Kapsula série + názov cviku (vkladá sa do Column). */
@Composable
private fun HlavaCviku(krok: KrokTreningu, farba: Color) {
    val slovo = if (krok.blok == Blok.KRUH) "Kolo" else "Séria"
    Kapsula(text = "$slovo ${krok.cisloSerie} / ${krok.zoSerii}", farba = farba)
    Spacer(Modifier.height(10.dp))
    Text(
        krok.polozka.cvik.nazov,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Pokyny(krok: KrokTreningu) {
    val pokyny = remember(krok.polozka.cvik.cue) {
        krok.polozka.cvik.cue.split("|").filter { it.isNotBlank() }.take(3)
    }
    if (pokyny.isEmpty()) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        pokyny.forEach {
            Text(
                "• ${it.trim()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Dva stĺpce na ležato (vľavo obraz a čísla, vpravo text a ovládanie), na výšku pod sebou.
 * [spodok] je hlavná akcia - na výšku pod celým obsahom, na ležato pod pravým stĺpcom.
 */
@Composable
private fun Rozlozenie(
    lavy: @Composable ColumnScope.() -> Unit,
    pravy: @Composable ColumnScope.() -> Unit,
    spodok: (@Composable () -> Unit)? = null,
) {
    if (LocalNaLezato.current) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            RolovatelnyStred(Modifier.weight(1f).fillMaxHeight(), obsah = lavy)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                RolovatelnyStred(Modifier.weight(1f).fillMaxWidth(), obsah = pravy)
                if (spodok != null) {
                    Spacer(Modifier.height(8.dp))
                    spodok()
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            RolovatelnyStred(Modifier.weight(1f).fillMaxWidth()) {
                lavy(this)
                pravy(this)
            }
            if (spodok != null) {
                Spacer(Modifier.height(16.dp))
                spodok()
            }
        }
    }
}

/** Opakovania: veľký cieľ v strede, pod ním jedna veľká akcia „Hotovo". */
@Composable
private fun CvicenieOpakovania(krok: KrokTreningu, partner: PartnerStopa?, naHotovo: () -> Unit) {
    val farba = FarbyBlokov.pre(krok.blok)
    val ciel = krok.polozka.ciel
    val naLezato = LocalNaLezato.current
    Rozlozenie(
        lavy = {
            KruhPostavicky(krok.polozka.rebrikId, farba, if (naLezato) 150.dp else 128.dp, pohyb = krok.polozka.cvik.pohyb)
            Spacer(Modifier.height(if (naLezato) 8.dp else 16.dp))
            if (!naLezato) HlavaCviku(krok, farba)
            Spacer(Modifier.height(4.dp))
            Text("$ciel", style = StylVelkeCislo, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (Meranie.z(krok.polozka.cvik.meranie) == Meranie.VZDIALENOST) {
                    sklonuj(ciel, "meter", "metre", "metrov")
                } else {
                    sklonuj(ciel, "opakovanie", "opakovania", "opakovaní")
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        pravy = {
            if (naLezato) HlavaCviku(krok, farba)
            Spacer(Modifier.height(16.dp))
            Pokyny(krok)
            KartaPartnera(krok, partner)
        },
        spodok = { VelkeTlacidlo(text = "Hotovo", onClick = naHotovo, ikona = Icons.Rounded.Check) },
    )
}

/**
 * Na čas (výdrž, alebo opakovania na odpočet): veľký kruh vo farbe bloku, ktorý sa plynulo plní,
 * číslo zostávajúcich sekúnd vo vnútri. Pri opakovaniach na čas je pod ním cieľ počtu.
 */
@Composable
private fun CvicenieVydrz(krok: KrokTreningu, o: PrehravacObraz) {
    val farba = FarbyBlokov.pre(krok.blok)
    val zostatok = rememberZostatokMs(o.koniec, o.pozastavene, o.zostatokPozastaveny)
    val celkom = (if (o.celkom > 0L) o.celkom else krok.sekundOdpoctu() * 1000L).coerceAtLeast(1L)
    val naLezato = LocalNaLezato.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val nizka = maxHeight < 600.dp
        val kruh = if (naLezato) {
            minOf(maxHeight * 0.86f, maxWidth * 0.44f).coerceIn(160.dp, 340.dp)
        } else {
            minOf(maxWidth * 0.84f, maxHeight * 0.56f).coerceIn(200.dp, 320.dp)
        }
        Rozlozenie(
            lavy = {
                if (!naLezato) {
                    if (!nizka) {
                        KruhPostavicky(krok.polozka.rebrikId, farba, 84.dp, pohyb = krok.polozka.cvik.pohyb)
                        Spacer(Modifier.height(12.dp))
                    }
                    HlavaCviku(krok, farba)
                    Spacer(Modifier.height(18.dp))
                }
                KruhPostupu(
                    postup = 1f - zostatok.toFloat() / celkom.toFloat(),
                    modifier = Modifier.size(kruh),
                    farba = farba,
                    hrubka = 18.dp,
                ) {
                    CisloOdpoctu(zostatok, pauza = false)
                }
            },
            pravy = {
                if (naLezato) {
                    KruhPostavicky(krok.polozka.rebrikId, farba, 96.dp, pohyb = krok.polozka.cvik.pohyb)
                    Spacer(Modifier.height(10.dp))
                    HlavaCviku(krok, farba)
                }
                if (krok.polozka.naCasS != null && !krok.jeVydrz()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "cieľ ~${krok.polozka.ciel} ${sklonuj(krok.polozka.ciel, "opakovanie", "opakovania", "opakovaní")}" +
                            if (krok.pytaSaPoSerii()) " · počet zapíšeš potom" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Pokyny(krok)
                KartaPartnera(krok, o.partner)
            },
        )
    }
}

// ---------------------------------------------------------------- otázka po sérii

@Composable
private fun OtazkaRezervy(krok: KrokTreningu, odpoved: (Rir, Int?) -> Unit) {
    val naCas = krok.jeVydrz()
    var hodnota by rememberSaveable(krok.polozka.cvik.id, krok.cisloSerie) {
        mutableIntStateOf(krok.polozka.ciel)
    }
    val krokZmeny = if (naCas) 5 else 1
    val farba = FarbyBlokov.pre(krok.blok)

    Rozlozenie(
        lavy = {
            Text(
                krok.polozka.cvik.nazov,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (naCas) "Koľko sekúnd si vydržal?" else "Koľko si spravil?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TlacidloKroku(Icons.Rounded.Remove, "Menej", farba) {
                    val nova = (hodnota - krokZmeny).coerceAtLeast(0)
                    val zmena = nova != hodnota
                    hodnota = nova
                    zmena
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AnimovaneCislo(
                        hodnota = hodnota,
                        style = StylVelkeCislo,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TlacidloKroku(Icons.Rounded.Add, "Viac", farba) {
                    hodnota += krokZmeny
                    true
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (krok.polozka.naCasS != null && !naCas) {
                    "za ${krok.polozka.naCasS} s · cieľ bol ${formatujHodnotu(krok.polozka.ciel, krok.polozka.cvik.meranie)}"
                } else {
                    "plán bol ${formatujHodnotu(krok.polozka.ciel, krok.polozka.cvik.meranie)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        pravy = {
            Spacer(Modifier.height(16.dp))
            Text("Koľko si mal ešte v sebe?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Rir.NIC, Rir.JEDNO_DVE, Rir.TRI_A_VIAC).forEach { r ->
                    VolbaRezervy(r.popis, farba) { odpoved(r, hodnota) }
                }
            }
        },
    )
}

/**
 * Veľké okrúhle tlačidlo stepperu. Ťuk = jeden krok s tikom; podržanie po chvíli opakuje
 * a zrýchľuje (ako na posilňovacom stroji). [naKrok] vracia, či sa hodnota naozaj zmenila.
 */
@Composable
private fun TlacidloKroku(ikona: ImageVector, popis: String, farba: Color, naKrok: () -> Boolean) {
    val haptika = rememberHaptika()
    val interakcia = remember { MutableInteractionSource() }
    val stlacene by interakcia.collectIsPressedAsState()
    val aktualnyKrok by rememberUpdatedState(naKrok)
    val opakovalo = remember { PrehravacDrzak(false) }

    LaunchedEffect(stlacene) {
        if (!stlacene) {
            // Až po kliknutí (to príde skôr než prekreslenie) - aj pri zrušenom stlačení (posun prstom).
            opakovalo.hodnota = false
            return@LaunchedEffect
        }
        opakovalo.hodnota = false
        delay(420)
        var pauza = 170L
        while (true) {
            opakovalo.hodnota = true
            if (aktualnyKrok()) haptika.tik()
            delay(pauza)
            pauza = (pauza * 0.84f).toLong().coerceAtLeast(45L)
        }
    }

    Surface(
        onClick = {
            // Po podržaní už kroky pribudli - pustenie prsta nesmie pridať ešte jeden.
            if (opakovalo.hodnota) {
                opakovalo.hodnota = false
            } else if (aktualnyKrok()) {
                haptika.tik()
            }
        },
        modifier = Modifier
            .size(80.dp)
            .pruzneStlacenie(interakcia),
        shape = CircleShape,
        color = farba.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interakcia,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(ikona, contentDescription = popis, modifier = Modifier.size(36.dp))
        }
    }
}

/** Odpoveď na „Koľko si mal ešte v sebe?" - veľká plocha, ťuk = zapísané a ďalej. */
@Composable
private fun VolbaRezervy(text: String, farba: Color, onClick: () -> Unit) {
    val haptika = rememberHaptika()
    val interakcia = remember { MutableInteractionSource() }
    Surface(
        onClick = {
            haptika.potvrdenie()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pruzneStlacenie(interakcia),
        shape = RoundedCornerShape(20.dp),
        color = farba.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow),
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interakcia,
    ) {
        Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ---------------------------------------------------------------- pauza

@Composable
private fun Pauza(
    o: PrehravacObraz,
    model: PrehravacModel,
    naUpravu: (Int) -> Unit,
    naPreskocenie: () -> Unit,
) {
    val dalsi = o.krok
    val farba = dalsi?.let { FarbyBlokov.pre(it.blok) } ?: MaterialTheme.colorScheme.secondary
    val zostatok = rememberZostatokMs(o.koniec, o.pozastavene, o.zostatokPozastaveny)
    val celkom = o.celkom.coerceAtLeast(1L)
    val sDoplnenim = o.partnerZapis != null || o.otazkaPriznaku != null
    val haptika = rememberHaptika()
    val naLezato = LocalNaLezato.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val kruh = if (naLezato) {
            minOf(maxHeight * 0.62f, maxWidth * 0.36f).coerceIn(140.dp, 280.dp)
        } else {
            minOf(maxWidth * 0.7f, maxHeight * (if (sDoplnenim) 0.26f else 0.32f)).coerceIn(150.dp, 260.dp)
        }
        Rozlozenie(
            lavy = {
                // Kruh sa vyprázdňuje - oddych ubúda.
                KruhPostupu(
                    postup = zostatok.toFloat() / celkom.toFloat(),
                    modifier = Modifier.size(kruh),
                    farba = farba,
                    hrubka = 16.dp,
                ) {
                    CisloOdpoctu(zostatok, pauza = true)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            haptika.klik()
                            naUpravu(-15)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text("−15 s", style = MaterialTheme.typography.titleMedium, maxLines = 1) }
                    FilledTonalButton(
                        onClick = {
                            haptika.klik()
                            naUpravu(15)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text("+15 s", style = MaterialTheme.typography.titleMedium, maxLines = 1) }
                    Button(
                        onClick = {
                            haptika.potvrdenie()
                            naPreskocenie()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text("Začať", style = MaterialTheme.typography.titleMedium, maxLines = 1) }
                }
            },
            pravy = {
                // Kým oddychuješ, dopíše sa predošlá séria: príznak a zápis druhého dospelého.
                DoplnenieSerie(o, model)
                if (dalsi != null) {
                    Spacer(Modifier.height(14.dp))
                    PredstavenieCviku(dalsi, o.partner)
                }
            },
        )
    }
}

/**
 * Predstavenie ďalšieho cviku v pauze: veľká postavička, názov, dávka, pokyny a najčastejšia chyba.
 * Pole `chyby` z katalógu sa inak v prehrávači nikde neukazuje.
 */
@Composable
private fun PredstavenieCviku(krok: KrokTreningu, partner: PartnerStopa?) {
    val farba = FarbyBlokov.pre(krok.blok)
    val cvik = krok.polozka.cvik
    val pokyny = remember(cvik.cue) { cvik.cue.split("|").map { it.trim() }.filter { it.isNotBlank() }.take(3) }
    val chyba = remember(cvik.chyby) { cvik.chyby.split("|").map { it.trim() }.firstOrNull { it.isNotBlank() } }
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KruhPostavicky(krok.polozka.rebrikId, farba, 104.dp, pohyb = krok.polozka.cvik.pohyb)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Ďalej · ${krok.blok.nazov}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    cvik.nazov,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val davka = if (krok.jeNaCas()) "${krok.sekundOdpoctu()} s" else formatujHodnotu(krok.polozka.ciel, cvik.meranie)
                val seria = if (krok.blok == Blok.KRUH) "kolo" else "séria"
                Text(
                    "$davka · $seria ${krok.cisloSerie} / ${krok.zoSerii}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (pokyny.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            pokyny.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (chyba != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Pozor: $chyba",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val p = krok.partner
        if (partner != null && p != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${partner.meno}: ${p.polozka.cvik.nazov} · ${formatujHodnotu(p.polozka.ciel, p.polozka.cvik.meranie)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------- druhý dospelý

/** „<meno>: cvik · plán" - čo v tomto kroku cvičí druhý dospelý. */
@Composable
private fun KartaPartnera(krok: KrokTreningu, partner: PartnerStopa?) {
    val p = krok.partner
    if (partner == null || p == null) return
    val farba = Color(partner.farba)
    Spacer(Modifier.height(16.dp))
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Inicialka(partner.meno, farba, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${partner.meno}: ${p.polozka.cvik.nazov}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatujHodnotu(p.polozka.ciel, p.polozka.cvik.meranie)} · séria ${p.cisloSerie} / ${p.zoSerii}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Dopísanie predošlej série: otázka príznaku pre teba a kompaktný zápis druhého dospelého. */
@Composable
private fun DoplnenieSerie(o: PrehravacObraz, model: PrehravacModel) {
    val otazka = o.otazkaPriznaku
    if (otazka != null) {
        Spacer(Modifier.height(14.dp))
        TonovanaKarta(farba = MaterialTheme.colorScheme.tertiary, vnutorneOdsadenie = 14.dp) {
            Text(
                otazka.nazovCviku,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OtazkaTela(otazka.otazka, otazka.priznak, MaterialTheme.colorScheme.tertiary) { model.odpovedPriznak(it) }
        }
    }
    val zapis = o.partnerZapis
    val partner = o.partner
    if (zapis != null && partner != null) {
        Spacer(Modifier.height(14.dp))
        ZapisPartnera(zapis, partner) { zmena -> model.upravPartnera(zmena) }
    }
}

/** Kompaktný zápis série druhého dospelého: hodnota (predvolene plán) −/+, rezerva, príznak. */
@Composable
private fun ZapisPartnera(z: PartnerZapis, partner: PartnerStopa, uprav: ((PartnerZapis) -> PartnerZapis) -> Unit) {
    val farba = Color(partner.farba)
    val cvik = z.krok.polozka.cvik
    val krokZmeny = if (Meranie.z(cvik.meranie) == Meranie.VYDRZ) 5 else 1
    val haptika = rememberHaptika()
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Inicialka(partner.meno, farba, 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(partner.meno, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(cvik.nazov, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = {
                    haptika.tik()
                    uprav { it.copy(hodnota = (it.hodnota - krokZmeny).coerceAtLeast(0)) }
                },
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Remove, contentDescription = "Menej") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimovaneCislo(
                    hodnota = z.hodnota,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "plán ${formatujHodnotu(z.krok.polozka.ciel, cvik.meranie)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(
                onClick = {
                    haptika.tik()
                    uprav { it.copy(hodnota = it.hodnota + krokZmeny) }
                },
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "Viac") }
        }
        val rir = z.rir
        if (rir != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Rir.NIC, Rir.JEDNO_DVE, Rir.TRI_A_VIAC).forEach { r ->
                    MalaVolba(r.popis, vybrana = rir == r, farba = farba, modifier = Modifier.weight(1f)) {
                        uprav { it.copy(rir = r) }
                    }
                }
            }
        }
        val otazka = partner.otazka
        if (otazka != null) {
            OtazkaTela(otazka, z.priznak, farba) { ano -> uprav { it.copy(priznak = ano) } }
        }
    }
}

/**
 * Otázka na príznak tela (napr. tlak, ťah, únik). „Áno" nie je zlyhanie - len zastaví postup
 * v tomto rebríku, kým telo nie je pripravené.
 */
@Composable
private fun OtazkaTela(otazka: String, priznak: Boolean, farba: Color, odpoved: (Boolean) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(otazka, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        MalaVolba("Nie", vybrana = !priznak, farba = farba) { odpoved(false) }
        Spacer(Modifier.width(6.dp))
        MalaVolba("Áno", vybrana = priznak, farba = farba) { odpoved(true) }
    }
    AnimatedVisibility(visible = priznak, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            "Zapísané. V tomto rebríku sa zatiaľ nepostupuje — uber záťaž alebo sa vráť o stupeň nižšie.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Malá voľba v kapsule (rezerva, áno/nie) - ťuk prepne, vybraná je plná. */
@Composable
private fun MalaVolba(text: String, vybrana: Boolean, farba: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptika = rememberHaptika()
    val pozadie by animateColorAsState(
        if (vybrana) farba.copy(alpha = 0.32f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "malaVolba",
    )
    Surface(
        onClick = { haptika.klik(); onClick() },
        modifier = modifier.heightIn(min = 44.dp),
        shape = CircleShape,
        color = pozadie,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (vybrana) BorderStroke(1.5.dp, farba) else null,
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, fontWeight = if (vybrana) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

/** Po poslednej sérii: dopísať druhého dospelého (a príznak), až potom súhrn. */
@Composable
private fun Doplnenie(o: PrehravacObraz, model: PrehravacModel, naHotovo: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        RolovatelnyStred(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text("Posledná séria", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Ešte ju zapíš, potom ukážem súhrn.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            DoplnenieSerie(o, model)
        }
        Spacer(Modifier.height(16.dp))
        VelkeTlacidlo(text = "Dokončiť", onClick = naHotovo, ikona = Icons.Rounded.Check)
    }
}

// ---------------------------------------------------------------- detský pruh

/**
 * Detský pruh rodinného režimu. Zámerné rozhodnutia (rešerš motivácie detí):
 * - dieťa si VYBERÁ z dvoch možností (voľba motivuje bez rizika),
 * - labky pribúdajú za pokus, žiadne časovky ani počty,
 * - ukazovateľ = 3 konkrétne labky (2-ročný nechápe percentá),
 * - čo sa odomkne, sa neukazuje vopred.
 * Mechanika je nedotknuteľná; vylepšený je len vzhľad a pohyb.
 */
@Composable
private fun DetskyPruh(d: DetskyStav, model: PrehravacModel) {
    val farba = MaterialTheme.colorScheme.tertiary
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                d.meno,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Labky(d.labky, farba)
        }
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = d,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(animationSpec = tween(260), initialScale = 0.9f)) togetherWith
                    fadeOut(tween(160))
            },
            contentAlignment = Alignment.TopCenter,
            label = "detskyKrok",
        ) { s ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (s.krok) {
                    DetskyKrok.VYBER -> DetskyVyber(s, farba, model)
                    DetskyKrok.CVICI -> DetskeCvicenie(s, farba, model)
                    DetskyKrok.ODHALENIE -> DetskeOdhalenie(s, model)
                    DetskyKrok.HOTOVO -> EsteTlacidlo(farba, model)
                }
            }
        }
    }
}

/** 3 labky - konkrétne obrázky, ktoré sa dopĺňajú; po treťej („Ešte!") pribúda číslo. */
@Composable
private fun Labky(pocet: Int, farba: Color, velkost: Dp = 36.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i -> Labka(naplnena = i < pocet, farba = farba, velkost = velkost) }
        if (pocet > 3) {
            Text("+${pocet - 3}", style = MaterialTheme.typography.titleMedium, color = farba)
        }
    }
}

/** Po zvierati (alebo keď už nie je čo odomknúť) môže dieťa cvičiť ďalej - bez ďalšej odmeny. */
@Composable
private fun EsteTlacidlo(farba: Color, model: PrehravacModel) {
    val haptika = rememberHaptika()
    Button(
        onClick = {
            haptika.potvrdenie()
            model.dietaPokracuje()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = farba, contentColor = MaterialTheme.colorScheme.onTertiary),
    ) { Text("Ešte! 🐾", style = MaterialTheme.typography.titleMedium) }
}

/**
 * Detský pruh na výšku: jeden riadok pod tvojím cvikom, aby nezabral sériu.
 * Rovnaká mechanika ako veľký panel (výber z dvoch, labka za pokus, neohlásené zviera).
 */
@Composable
private fun DetskyPruhKompaktny(d: DetskyStav, model: PrehravacModel) {
    val farba = MaterialTheme.colorScheme.tertiary
    val haptika = rememberHaptika()
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 10.dp, modifier = Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(76.dp)) {
                Text(d.meno, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Labky(d.labky, farba, velkost = 20.dp)
            }
            Spacer(Modifier.width(8.dp))
            when (d.krok) {
                DetskyKrok.VYBER -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.moznosti.forEach { cvik ->
                        val zviera = cvik.zviera?.let { Zvierata.podla(it) }
                        Surface(
                            onClick = { haptika.klik(); model.dietaVybralo(cvik) },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, farba.copy(alpha = 0.45f)),
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(zviera?.emoji ?: "⭐", fontSize = 28.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    zviera?.meno ?: cvik.nazov,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                DetskyKrok.CVICI -> {
                    val cvik = d.aktualny
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (cvik != null) {
                            Postavicka(
                                rebrikId = cvik.rebrikId,
                                pohyb = cvik.pohyb,
                                modifier = Modifier.size(48.dp),
                                farba = MaterialTheme.colorScheme.onSurface,
                                farbaAkcent = farba,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cvik?.nazov.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        DetskyOdpocet(cvik?.id, farba, model, kompaktny = true)
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = { haptika.potvrdenie(); model.dietaSpravilo() },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = farba, contentColor = MaterialTheme.colorScheme.onTertiary),
                        ) { Text("Spravené 🐾", style = MaterialTheme.typography.labelLarge) }
                    }
                }
                DetskyKrok.ODHALENIE -> Box(Modifier.weight(1f)) { DetskeOdhalenieMale(d, model) }
                DetskyKrok.HOTOVO -> Box(Modifier.weight(1f)) { EsteTlacidlo(farba, model) }
            }
        }
    }
}

/**
 * Voliteľný krátky odpočet pre dieťa („Skúsiš to, kým zapípa?"). Deti sa hýbu v krátkych
 * dávkach - 20 s je hra, nie výkon. Labka sa dáva za pokus ďalej, odpočet nič nehodnotí.
 */
@Composable
private fun DetskyOdpocet(cvikId: String?, farba: Color, model: PrehravacModel, kompaktny: Boolean = false) {
    var koniec by remember(cvikId) { mutableStateOf(0L) }
    var zostava by remember(cvikId) { mutableIntStateOf(0) }
    LaunchedEffect(koniec) {
        if (koniec <= 0L) return@LaunchedEffect
        while (true) {
            val ms = koniec - SystemClock.elapsedRealtime()
            zostava = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
            if (ms <= 0) break
            delay(100)
        }
        model.hlas.pipniKoniec(model.pipanieZapnute)
        model.hlas.povedzPotom("Hotovo! Výborne.", model.hlasZapnuty)
        koniec = 0L
    }
    val bezi = koniec > 0L
    OutlinedButton(
        onClick = {
            if (!bezi) {
                koniec = SystemClock.elapsedRealtime() + DETSKY_ODPOCET_S * 1000L
                zostava = DETSKY_ODPOCET_S
            }
        },
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = if (kompaktny) 10.dp else 16.dp),
        border = BorderStroke(1.5.dp, farba),
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (bezi) "$zostava s" else if (kompaktny) "${DETSKY_ODPOCET_S} s" else "Skúsiš ${DETSKY_ODPOCET_S} s?",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Odhalenie v kompaktnom pruhu - emoji a veta, po chvíli späť k pohybu. */
@Composable
private fun DetskeOdhalenieMale(s: DetskyStav, model: PrehravacModel) {
    val z = s.odhalene ?: return
    LaunchedEffect(z.kluc) {
        delay(3500)
        model.odhalenieVidene()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(z.emoji, fontSize = 40.sp)
        Spacer(Modifier.width(8.dp))
        Text(z.veta, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Labka(naplnena: Boolean, farba: Color, velkost: Dp = 36.dp) {
    val mierka by animateFloatAsState(
        targetValue = if (naplnena) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "labka",
    )
    Box(
        Modifier
            .size(velkost)
            .clip(CircleShape)
            .background(if (naplnena) farba.copy(alpha = 0.22f) else Color.Transparent)
            .border(2.dp, farba.copy(alpha = if (naplnena) 0f else 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (naplnena) {
            Text(
                "🐾",
                fontSize = (velkost.value * 0.55f).sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = mierka
                    scaleY = mierka
                },
            )
        }
    }
}

@Composable
private fun DetskyVyber(s: DetskyStav, farba: Color, model: PrehravacModel) {
    Text("Čo ideme teraz?", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        s.moznosti.forEach { cvik ->
            val zviera = cvik.zviera?.let { Zvierata.podla(it) }
            VolbaZvierata(
                emoji = zviera?.emoji ?: "⭐",
                popis = zviera?.meno ?: cvik.nazov,
                farba = farba,
                modifier = Modifier.weight(1f),
            ) { model.dietaVybralo(cvik) }
        }
    }
}

@Composable
private fun VolbaZvierata(
    emoji: String,
    popis: String,
    farba: Color,
    modifier: Modifier,
    naVyber: () -> Unit,
) {
    val haptika = rememberHaptika()
    val interakcia = remember { MutableInteractionSource() }
    Surface(
        onClick = {
            haptika.klik()
            naVyber()
        },
        modifier = modifier
            .heightIn(min = 120.dp)
            .pruzneStlacenie(interakcia),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, farba.copy(alpha = 0.45f)),
        interactionSource = interakcia,
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, fontSize = 52.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                popis,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetskeCvicenie(s: DetskyStav, farba: Color, model: PrehravacModel) {
    val cvik = s.aktualny
    val zviera = cvik?.zviera?.let { Zvierata.podla(it) }
    val haptika = rememberHaptika()
    val skakanie = rememberInfiniteTransition(label = "detskeSkakanie")
    val posun by skakanie.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "detskyPosun",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            zviera?.emoji ?: "⭐",
            fontSize = 56.sp,
            modifier = Modifier.graphicsLayer { translationY = posun.dp.toPx() },
        )
        if (cvik != null) {
            Spacer(Modifier.width(8.dp))
            Postavicka(
                rebrikId = cvik.rebrikId,
                pohyb = cvik.pohyb,
                modifier = Modifier.size(88.dp),
                farba = MaterialTheme.colorScheme.onSurface,
                farbaAkcent = farba,
            )
        }
    }
    Text(
        cvik?.nazov ?: "",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    cvik?.cue?.split("|")?.firstOrNull()?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(10.dp))
    DetskyOdpocet(cvik?.id, farba, model)
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = {
            haptika.potvrdenie()
            model.dietaSpravilo()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = farba,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
    ) { Text("Spravené 🐾", style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun DetskeOdhalenie(s: DetskyStav, model: PrehravacModel) {
    val z = s.odhalene ?: return
    val haptika = rememberHaptika()
    val mierka = remember(z.kluc) { Animatable(0f) }
    val otocenie = remember(z.kluc) { Animatable(-180f) }
    LaunchedEffect(z.kluc) {
        haptika.uspech()
        launch { mierka.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        otocenie.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    // Krátko a späť k pohybu - odmena sa nesmie stať zábavou na obrazovke.
    LaunchedEffect(z.kluc) {
        delay(3500)
        model.odhalenieVidene()
    }
    Text(
        z.emoji,
        fontSize = 88.sp,
        modifier = Modifier.graphicsLayer {
            scaleX = mierka.value
            scaleY = mierka.value
            rotationZ = otocenie.value
        },
    )
    Spacer(Modifier.height(6.dp))
    Text(z.veta, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
        "${z.meno} je v tvojej zbierke.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------- ovládanie a pozastavenie

@Composable
private fun SpodneOvladanie(naVynechanie: () -> Unit, naSkratenie: () -> Unit, naUkoncenie: () -> Unit) {
    val haptika = rememberHaptika()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = {
                haptika.klik()
                naVynechanie()
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Vynechať") }
        OutlinedButton(
            onClick = {
                haptika.klik()
                naSkratenie()
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Nevládzem") }
        TextButton(
            onClick = {
                haptika.klik()
                naUkoncenie()
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Ukončiť") }
    }
}

@Composable
private fun PozastaveneVrstva(
    zostatokMs: Long,
    vPauze: Boolean,
    naPokracovanie: () -> Unit,
    naUkoncenie: () -> Unit,
) {
    // Surface pohltí dotyky - pod vrstvou sa nič nedá omylom stlačiť.
    val naLezato = LocalNaLezato.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Na ležato je výšky málo - obsah sa posúva, tlačidlá ostanú dosiahnuteľné.
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (naLezato) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(if (naLezato) 72.dp else 120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (naLezato) 36.dp else 60.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Pozastavené", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            val sekundy = ((zostatokMs + 999L) / 1000L).toInt()
            Text(
                if (sekundy > 0) {
                    "Odpočet stojí na ${formatujStopky(sekundy * 1000L)} (${if (vPauze) "pauza" else "výdrž"}). " +
                        "Hlas mlčí, kým nepokračuješ."
                } else {
                    "Tréning stojí a hlas mlčí, kým nepokračuješ."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            VelkeTlacidlo(text = "Pokračovať", onClick = naPokracovanie, ikona = Icons.Rounded.PlayArrow)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = naUkoncenie,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ukončiť tréning", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------- záver

/** Celostránkový súhrn: čísla ako hrdinovia, rekordy a postupy, oslava len pri skutočnom úspechu. */
@Composable
private fun Zaver(detsky: DetskyStav?, suhrn: PrehravacSuhrn?, hotovo: () -> Unit) {
    val haptika = rememberHaptika()
    val oslava = suhrn != null && (suhrn.rekordy.isNotEmpty() || suhrn.postupy.any { it.vyssie })
    LaunchedEffect(oslava) {
        if (oslava) haptika.uspech()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                ZnakHotovo()
                Spacer(Modifier.height(16.dp))
                val prazdny = suhrn != null && suhrn.serie == 0
                Text(if (prazdny) "Ukončené." else "Hotovo.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (prazdny) "Nezapísala sa ani jedna séria, takže sa tento tréning do histórie neráta."
                    else "Zapísané. To je celé, čo dnes bolo treba.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = suhrn != null && !prazdny,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                ) {
                    if (suhrn != null) SuhrnTreningu(suhrn)
                }
                if (detsky != null && detsky.labky > 0) {
                    Spacer(Modifier.height(16.dp))
                    TonovanaKarta(farba = MaterialTheme.colorScheme.tertiary) {
                        Text(
                            "${detsky.meno}: ${"🐾".repeat(detsky.labky)} — cvičili ste spolu.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            VelkeTlacidlo(text = "Zavrieť", onClick = hotovo)
        }
        Konfety(spustit = oslava, modifier = Modifier.fillMaxSize(), kluc = "zaver")
    }
}

@Composable
private fun ZnakHotovo() {
    val mierka = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        mierka.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val farba = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(104.dp)
            .graphicsLayer {
                scaleX = mierka.value
                scaleY = mierka.value
            }
            .clip(CircleShape)
            .background(farba.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = farba, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun SuhrnTreningu(s: PrehravacSuhrn) {
    val minuty = rememberPocitadlo((s.trvanieMs / 60_000L).toInt())
    val serie = rememberPocitadlo(s.serie)
    val opakovania = rememberPocitadlo(s.opakovania)
    val sekundy = rememberPocitadlo(s.sekundyVydrzi)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatDlazdica(
                hodnota = if (s.trvanieMs < 60_000L) "${s.trvanieMs / 1000L} s" else formatujTrvanie(minuty * 60_000L),
                popis = "trvanie",
                modifier = Modifier.weight(1f),
                ikona = Icons.Rounded.Timer,
            )
            StatDlazdica(
                hodnota = "$serie",
                popis = sklonuj(s.serie, "séria", "série", "sérií"),
                modifier = Modifier.weight(1f),
                ikona = Icons.Rounded.Repeat,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatDlazdica(
                hodnota = "$opakovania",
                popis = sklonuj(s.opakovania, "opakovanie", "opakovania", "opakovaní") + " spolu",
                modifier = Modifier.weight(1f),
                ikona = Icons.Rounded.FitnessCenter,
            )
            StatDlazdica(
                hodnota = formatujHodnotu(sekundy, Meranie.VYDRZ.kluc),
                popis = "vo výdržiach",
                modifier = Modifier.weight(1f),
                ikona = Icons.Rounded.HourglassBottom,
            )
        }
        if (s.rekordy.isNotEmpty()) {
            NadpisSekcie("Osobné rekordy")
            s.rekordy.forEach { KartaRekordu(it) }
        }
        if (s.postupy.isNotEmpty()) {
            NadpisSekcie("Rebríky")
            s.postupy.forEach { KartaPostupu(it) }
        }
        s.partner?.let { SuhrnDruheho(it) }
    }
}

@Composable
private fun SuhrnDruheho(p: SuhrnPartnera) {
    val farba = Color(p.farba)
    NadpisSekcie(p.meno)
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Inicialka(p.meno, farba, 44.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                "${p.serie} ${sklonuj(p.serie, "séria", "série", "sérií")} zapísaných",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    p.rekordy.forEach { KartaRekordu(it) }
    p.postupy.forEach { KartaPostupu(it) }
}

/** Počítadlo, ktoré pri prvom zobrazení „nabehne" od nuly ku skutočnej hodnote. */
@Composable
private fun rememberPocitadlo(ciel: Int): Int {
    val hodnota = remember { Animatable(0f) }
    LaunchedEffect(ciel) {
        hodnota.animateTo(ciel.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    return hodnota.value.roundToInt()
}

@Composable
private fun KartaRekordu(r: Rekord) {
    val farba = MaterialTheme.colorScheme.primary
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(farba.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = farba, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                r.nazov,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${formatujHodnotu(r.predtym, r.meranie)} → ",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatujHodnotu(r.hodnota, r.meranie),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = farba,
            )
        }
    }
}

@Composable
private fun KartaPostupu(p: PrehravacPostup) {
    val farba = if (p.vyssie) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TonovanaKarta(farba = if (p.vyssie) farba else null, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (p.vyssie) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = farba,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(p.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

// ---------------------------------------------------------------- formátovanie

/** Stopky „4:05" alebo „1:02:03". */
private fun formatujStopky(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    val h = s / 3600L
    val m = (s % 3600L) / 60L
    val sek = s % 60L
    return if (h > 0L) "%d:%02d:%02d".format(h, m, sek) else "%d:%02d".format(m, sek)
}
