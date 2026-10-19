package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
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
import sk.havrila.cvicime.trening.Hlas
import sk.havrila.cvicime.trening.TreningService
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.Konfety
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
)

enum class StavPrehravaca { NACITAVA, CVICENIE, OTAZKA, PAUZA, HOTOVO, PRAZDNY }

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
)

class PrehravacModel(app: Application) : AndroidViewModel(app) {

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

    val hlasZapnuty get() = repo.prefs.hlasZapnuty
    val vibracieZapnute get() = repo.prefs.vibracieZapnute
    val obrazovkaStaleZapnuta get() = repo.prefs.obrazovkaStaleZapnuta

    private var treningId: Long = -1
    private var spusteny = false
    private var poradieSerie = 0
    private var detskeCviky: List<CvikEntity> = emptyList()

    /** Rozbehnuté zápisy sérií - pred ukončením sa na ne počká, aby vyhodnotenie videlo každú sériu. */
    private val zapisy = mutableListOf<Job>()

    fun spusti(minuty: Int, dietaId: Long) {
        // Poistka: po otočení obrazovky sa LaunchedEffect spustí znova. Tréning sa nesmie založiť
        // druhýkrát - ani počas načítania, ani na súhrne (vtedy je treningId už -1).
        if (spusteny || treningId > 0) return
        spusteny = true
        viewModelScope.launch {
            val plan = repo.zostavPlan(minuty)
            val zoznam = plan.bloky.flatMap { blok ->
                blok.polozky.flatMap { polozka ->
                    (1..polozka.serie).map { i -> KrokTreningu(blok.blok, polozka, i, polozka.serie) }
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
            pripravKrok(0)
        }
    }

    // ------------------------------------------------------------ dospelá vetva

    private fun pripravKrok(i: Int) {
        _index.value = i
        val krok = _kroky.value.getOrNull(i) ?: run {
            _stav.value = StavPrehravaca.HOTOVO
            return
        }
        _stav.value = StavPrehravaca.CVICENIE
        val naCas = Meranie.z(krok.polozka.cvik.meranie) == Meranie.VYDRZ
        _celkomOdpoctu.value = if (naCas) krok.polozka.ciel * 1000L else 0L
        _koniecOdpoctu.value = if (naCas) {
            SystemClock.elapsedRealtime() + krok.polozka.ciel * 1000L
        } else {
            0L
        }
    }

    fun hotovoCvicenie() {
        val krok = _kroky.value.getOrNull(_index.value) ?: return
        _koniecOdpoctu.value = 0L
        if (krok.blok == Blok.SILA || krok.blok == Blok.KONDICIA) {
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
        dalej()
    }

    private fun zapis(krok: KrokTreningu, hodnota: Int?, rir: Int?, preskocena: Boolean = false) {
        val id = treningId
        if (id <= 0) return
        poradieSerie += 1
        val poradie = poradieSerie
        zapisy += viewModelScope.launch {
            repo.zapisSeriu(
                treningId = id,
                blok = krok.blok.kluc,
                poradie = poradie,
                cvik = krok.polozka.cvik,
                planHodnota = krok.polozka.ciel,
                skutocnaHodnota = hodnota,
                rir = rir,
                preskocena = preskocena,
            )
        }
    }

    private fun dalej() {
        val dalsiIndex = _index.value + 1
        val krok = _kroky.value.getOrNull(_index.value)
        val dalsi = _kroky.value.getOrNull(dalsiIndex)
        if (dalsi == null) {
            ukonci()
            return
        }
        val pauza = krok?.polozka?.pauzaS ?: 0
        if (pauza > 0) {
            _stav.value = StavPrehravaca.PAUZA
            _celkomOdpoctu.value = pauza * 1000L
            _koniecOdpoctu.value = SystemClock.elapsedRealtime() + pauza * 1000L
            _index.value = dalsiIndex
        } else {
            pripravKrok(dalsiIndex)
        }
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
        if (s != StavPrehravaca.CVICENIE && s != StavPrehravaca.OTAZKA && s != StavPrehravaca.PAUZA) return
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
        _pozastavene.value = false
        _zostatokPozastaveny.value = 0L
        _stav.value = StavPrehravaca.HOTOVO
        if (id <= 0) return
        val d = _detsky.value
        val cakajuceZapisy = zapisy.toList()
        viewModelScope.launch {
            // Posledná séria sa mohla ešte zapisovať - vyhodnotenie postupov ju musí vidieť.
            cakajuceZapisy.joinAll()
            // Len čítanie: stupne PRED vyhodnotením, aby súhrn vedel povedať smer (vyššie / nižšie).
            val urovnePred = repo.denik.urovne().associate { it.rebrikId to it.uroven }
            repo.ukonciTrening(id, pocitPo = null)
            if (d != null) {
                repo.denik.ulozUcast(
                    UcastEntity(treningId = id, dietaId = d.dietaId, splneneKroky = d.labky)
                )
            }
            val postupy = repo.vyhodnotPostupy(id).mapNotNull { (rebrikId, uroven) ->
                val r = repo.katalog.rebrik(rebrikId) ?: return@mapNotNull null
                val cvik = repo.katalog.cvik(rebrikId, uroven)
                val vyssie = uroven > (urovnePred[rebrikId] ?: 1)
                val veta = if (vyssie) "postupuješ na stupeň $uroven" else "vraciaš sa na stupeň $uroven"
                PrehravacPostup(
                    text = "${r.nazov}: $veta" + (cvik?.let { " — ${it.nazov}" } ?: ""),
                    vyssie = vyssie,
                )
            }

            // Série hlavného cvičiaceho (ucastnikId 0) - rovnako ako postupy a rekordy.
            val odcvicene = repo.denik.serieTreningu(id)
                .filter { !it.preskocena && it.skutocnaHodnota != null && it.ucastnikId == 0L }
            val trening = repo.denik.trening(id)
            val trvanie = trening?.let { (it.koniec ?: System.currentTimeMillis()) - it.zaciatok } ?: 0L
            // Rekordy sú len ozdoba súhrnu - chyba pri ich výpočte nesmie zhodiť koniec tréningu.
            val rekordy = runCatching { repo.rekordyTreningu(id) }.getOrDefault(emptyList())
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
        val labky = (d.labky + 1).coerceAtMost(3)

        if (labky >= 3) {
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
)

private class PrehravacDrzak<T>(var hodnota: T)

private fun KrokTreningu.jeVydrz() = Meranie.z(polozka.cvik.meranie) == Meranie.VYDRZ

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

    val hlas = remember { Hlas(kontext) }

    LaunchedEffect(minuty, dietaId) { model.spusti(minuty, dietaId) }

    DisposableEffect(Unit) {
        TreningService.spusti(kontext, "Prebieha tréning")
        if (model.obrazovkaStaleZapnuta) pohlad.keepScreenOn = true
        onDispose {
            TreningService.zastav(kontext)
            pohlad.keepScreenOn = false
            hlas.zavri()
        }
    }

    val krok = kroky.getOrNull(index)
    val vTreningu = kroky.isNotEmpty() &&
        (stav == StavPrehravaca.CVICENIE || stav == StavPrehravaca.OTAZKA || stav == StavPrehravaca.PAUZA)

    // Hlas pri zmene cviku - názov + dávka. Bez pozerania na displej.
    LaunchedEffect(stav, index) {
        val k = kroky.getOrNull(index) ?: return@LaunchedEffect
        when (stav) {
            StavPrehravaca.CVICENIE -> {
                val naCas = Meranie.z(k.polozka.cvik.meranie) == Meranie.VYDRZ
                val davka = if (naCas) "${k.polozka.ciel} sekúnd" else "${k.polozka.ciel} opakovaní"
                hlas.povedz("${k.polozka.cvik.nazov}. $davka.", model.hlasZapnuty)
            }
            StavPrehravaca.PAUZA -> {
                val dalsi = kroky.getOrNull(index)
                hlas.povedz(
                    "Pauza. Ďalej ${dalsi?.polozka?.cvik?.nazov ?: ""}.",
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
        val vydrz = stavOdpoctu == StavPrehravaca.CVICENIE && k.jeVydrz()
        if (!vydrz && stavOdpoctu != StavPrehravaca.PAUZA) return@LaunchedEffect
        var poslednePipnutie = -1
        while (true) {
            // Pozastavenie zastaví odpočet hneď, nie až pri ďalšom prekreslení.
            if (model.pozastavene.value) return@LaunchedEffect
            val ms = koniecOdpoctu - SystemClock.elapsedRealtime()
            val s = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
            if (s in 1..3 && s != poslednePipnutie) {
                poslednePipnutie = s
                hlas.pipni(model.hlasZapnuty)
            }
            if (ms <= 0) break
            delay(50)
        }
        hlas.pipniKoniec(model.hlasZapnuty)
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
    )
    val kluc = PrehravacKluc(stav, index, krok?.polozka?.cvik?.id, krok?.cisloSerie ?: 0)

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                                if (kr.jeVydrz()) {
                                    CvicenieVydrz(kr, o)
                                } else {
                                    CvicenieOpakovania(
                                        krok = kr,
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

                        StavPrehravaca.HOTOVO -> Zaver(o.detsky, o.suhrn, hotovo)
                    }
                }
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
            spustit = stav == StavPrehravaca.PAUZA && detsky?.krok == DetskyKrok.ODHALENIE && odhalene != null,
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
) {
    val krok = kroky.getOrNull(index)
    val farbaBloku = krok?.let { FarbyBlokov.pre(it.blok) } ?: MaterialTheme.colorScheme.primary
    val haptika = rememberHaptika()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        PruhTreningu(kroky, index, obraz)
        Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.width(8.dp))
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
        obraz.stav == StavPrehravaca.OTAZKA -> 1f
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
private fun KruhPostavicky(rebrikId: String, farba: Color, velkost: Dp) {
    Box(
        modifier = Modifier
            .size(velkost)
            .clip(CircleShape)
            .background(farba.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Postavicka(
            rebrikId = rebrikId,
            modifier = Modifier.size(velkost * 0.8f),
            farba = MaterialTheme.colorScheme.onSurface,
            farbaAkcent = farba,
        )
    }
}

/** Kapsula série + názov cviku (vkladá sa do Column). */
@Composable
private fun HlavaCviku(krok: KrokTreningu, farba: Color) {
    Kapsula(text = "Séria ${krok.cisloSerie} / ${krok.zoSerii}", farba = farba)
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

/** Opakovania: veľký cieľ v strede, pod ním jedna veľká akcia „Hotovo". */
@Composable
private fun CvicenieOpakovania(krok: KrokTreningu, naHotovo: () -> Unit) {
    val farba = FarbyBlokov.pre(krok.blok)
    val ciel = krok.polozka.ciel
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        RolovatelnyStred(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            KruhPostavicky(krok.polozka.rebrikId, farba, 128.dp)
            Spacer(Modifier.height(16.dp))
            HlavaCviku(krok, farba)
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
            Spacer(Modifier.height(16.dp))
            Pokyny(krok)
        }
        Spacer(Modifier.height(16.dp))
        VelkeTlacidlo(text = "Hotovo", onClick = naHotovo, ikona = Icons.Rounded.Check)
    }
}

/** Výdrž: veľký kruh vo farbe bloku, ktorý sa plynulo plní, číslo zostávajúcich sekúnd vo vnútri. */
@Composable
private fun CvicenieVydrz(krok: KrokTreningu, o: PrehravacObraz) {
    val farba = FarbyBlokov.pre(krok.blok)
    val zostatok = rememberZostatokMs(o.koniec, o.pozastavene, o.zostatokPozastaveny)
    val celkom = (if (o.celkom > 0L) o.celkom else krok.polozka.ciel * 1000L).coerceAtLeast(1L)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val nizka = maxHeight < 600.dp
        val kruh = minOf(maxWidth * 0.84f, maxHeight * 0.56f).coerceIn(220.dp, 320.dp)
        RolovatelnyStred(Modifier.fillMaxSize()) {
            if (!nizka) {
                KruhPostavicky(krok.polozka.rebrikId, farba, 84.dp)
                Spacer(Modifier.height(12.dp))
            }
            HlavaCviku(krok, farba)
            Spacer(Modifier.height(18.dp))
            KruhPostupu(
                postup = 1f - zostatok.toFloat() / celkom.toFloat(),
                modifier = Modifier.size(kruh),
                farba = farba,
                hrubka = 18.dp,
            ) {
                CisloOdpoctu(zostatok, pauza = false)
            }
            Spacer(Modifier.height(18.dp))
            Pokyny(krok)
        }
    }
}

// ---------------------------------------------------------------- otázka po sérii

@Composable
private fun OtazkaRezervy(krok: KrokTreningu, odpoved: (Rir, Int?) -> Unit) {
    val naCas = krok.jeVydrz()
    var hodnota by remember(krok.polozka.cvik.id, krok.cisloSerie) {
        mutableIntStateOf(krok.polozka.ciel)
    }
    val krokZmeny = if (naCas) 5 else 1
    val farba = FarbyBlokov.pre(krok.blok)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        RolovatelnyStred(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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
                "plán bol ${formatujHodnotu(krok.polozka.ciel, krok.polozka.cvik.meranie)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Koľko si mal ešte v sebe?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Rir.NIC, Rir.JEDNO_DVE, Rir.TRI_A_VIAC).forEach { r ->
                VolbaRezervy(r.popis, farba) { odpoved(r, hodnota) }
            }
        }
    }
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
    val detsky = o.detsky
    val sDetmi = detsky != null && detsky.krok != DetskyKrok.HOTOVO
    val haptika = rememberHaptika()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val kruh = minOf(maxWidth * 0.78f, maxHeight * (if (sDetmi) 0.34f else 0.48f)).coerceIn(196.dp, 300.dp)
        RolovatelnyStred(Modifier.fillMaxSize()) {
            // Kruh sa vyprázdňuje - oddych ubúda.
            KruhPostupu(
                postup = zostatok.toFloat() / celkom.toFloat(),
                modifier = Modifier.size(kruh),
                farba = farba,
                hrubka = 18.dp,
            ) {
                CisloOdpoctu(zostatok, pauza = true)
            }
            Spacer(Modifier.height(16.dp))
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
                ) { Text("Preskočiť", style = MaterialTheme.typography.titleMedium, maxLines = 1) }
            }
            if (dalsi != null) {
                Spacer(Modifier.height(16.dp))
                KartaDalej(dalsi)
            }
            // Detský pruh: deti cvičia v tvojich pauzách. Ty oddychuješ, oni sa hrajú.
            if (detsky != null && sDetmi) {
                Spacer(Modifier.height(14.dp))
                DetskyPruh(detsky, model)
            }
        }
    }
}

/** „Ďalej: cvik · plán" s malou postavičkou ďalšieho kroku. */
@Composable
private fun KartaDalej(krok: KrokTreningu) {
    val farba = FarbyBlokov.pre(krok.blok)
    TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KruhPostavicky(krok.polozka.rebrikId, farba, 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Ďalej · ${krok.blok.nazov}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    krok.polozka.cvik.nazov,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatujHodnotu(krok.polozka.ciel, krok.polozka.cvik.meranie)} · séria ${krok.cisloSerie} / ${krok.zoSerii}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            // 3 labky - konkrétne obrázky, ktoré sa dopĺňajú
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i -> Labka(naplnena = i < d.labky, farba = farba) }
            }
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
                    DetskyKrok.HOTOVO -> {}
                }
            }
        }
    }
}

@Composable
private fun Labka(naplnena: Boolean, farba: Color) {
    val mierka by animateFloatAsState(
        targetValue = if (naplnena) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "labka",
    )
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (naplnena) farba.copy(alpha = 0.22f) else Color.Transparent)
            .border(2.dp, farba.copy(alpha = if (naplnena) 0f else 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (naplnena) {
            Text(
                "🐾",
                fontSize = 20.sp,
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
    Text(
        zviera?.emoji ?: "⭐",
        fontSize = 64.sp,
        modifier = Modifier.graphicsLayer { translationY = posun.dp.toPx() },
    )
    Text(
        cvik?.nazov ?: "",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    cvik?.cue?.split("|")?.firstOrNull()?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp),
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
            Spacer(Modifier.weight(1f))
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
                Text("Hotovo.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Zapísané. To je celé, čo dnes bolo treba.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = suhrn != null,
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
    }
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
