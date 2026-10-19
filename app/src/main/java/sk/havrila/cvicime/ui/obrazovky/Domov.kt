package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.DietaEntity
import sk.havrila.cvicime.data.denik.ProfilEntity
import sk.havrila.cvicime.data.denik.SuhrnTreningov
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.domain.PlanBloku
import sk.havrila.cvicime.domain.PlanPolozka
import sk.havrila.cvicime.domain.PlanTreningu
import sk.havrila.cvicime.domain.Pravidla
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.KruhPostupu
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.SegmentovyPruh
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.TyzdenPas
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.formatujHodnotu
import sk.havrila.cvicime.ui.komponenty.formatujTrvanie
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk
import java.time.LocalDate
import java.time.LocalDateTime

/** Cieľ zručnosti na Domove: kde v rebríku stojím a koľko stupňov má. */
data class ZrucnostNaDomove(val kluc: String, val nazov: String, val uroven: Int, val max: Int)

/** Je čas na premeranie - koľko týždňov od testu a prípadné upozornenie na dennú dobu. */
data class RetestNaDomove(val tyzdnov: Long, val dennaDoba: String?)

/**
 * Stav obrazovky "Dnes".
 *
 * Zámerne sa NEČÍTA jednorazovo v init - obrazovka sleduje databázu priebežne.
 * Jednorazové načítanie bola príčina chyby, pri ktorej sa po návrate z onboardingu
 * stále zobrazovala výzva "nastav ciele", hoci ciele už uložené boli: ViewModel
 * prežil navigáciu a držal starý profil. Tokom z Room sa to stať nemôže.
 */
class DomovModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _minuty = MutableStateFlow(repo.prefs.poslednyCasMinut)
    val minuty = _minuty.asStateFlow()

    private val _profil = MutableStateFlow<ProfilEntity?>(null)
    val profil = _profil.asStateFlow()

    private val _ciele = MutableStateFlow<List<CielEntity>>(emptyList())
    val ciele = _ciele.asStateFlow()

    private val _plan = MutableStateFlow<PlanTreningu?>(null)
    val plan = _plan.asStateFlow()

    private val _chyba = MutableStateFlow<String?>(null)
    val chyba = _chyba.asStateFlow()

    private val _pocetCvikov = MutableStateFlow(-1)
    val pocetCvikov = _pocetCvikov.asStateFlow()

    private val _prvyMesiac = MutableStateFlow(true)
    val prvyMesiac = _prvyMesiac.asStateFlow()

    private val _deti = MutableStateFlow<List<DietaEntity>>(emptyList())
    val deti = _deti.asStateFlow()

    private val _bielkoviny = MutableStateFlow(false)
    val bielkoviny = _bielkoviny.asStateFlow()

    /** Koľko dní vypadlo - podnet na otázku o prekážke. null = nepýtať sa. */
    private val _dniPauzy = MutableStateFlow<Long?>(null)
    val dniPauzy = _dniPauzy.asStateFlow()

    /** null = na retest ešte nie je čas (alebo test ešte nebol). */
    private val _retest = MutableStateFlow<RetestNaDomove?>(null)
    val retest = _retest.asStateFlow()

    private val pondelok = repo.zaciatokTyzdna(repo.dnes())

    /** Dni tohto týždňa s dokončeným tréningom - zrkadlo týždňa, nie séria. */
    val dniTyzdna: StateFlow<Set<Long>> = repo.dniTreningovTok(pondelok)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val suhrnTyzdna: StateFlow<SuhrnTreningov> = repo.suhrnTreningovTok(pondelok)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SuhrnTreningov(0, 0))

    /**
     * Ciele zručností s aktuálnym stupňom a maximom rebríka. Katalóg sa pri štarte
     * môže ešte prepisovať, preto sa sleduje aj tok rebríkov - maximá sa dopočítajú znova.
     */
    val zrucnosti: StateFlow<List<ZrucnostNaDomove>> =
        combine(repo.cieleTok(), repo.denik.urovneTok(), repo.rebrikyTok()) { ciele, urovne, _ ->
            val maxy = repo.katalog.maxUrovne().associate { it.rebrikId to it.maxUroven }
            val aktualne = urovne.associate { it.rebrikId to it.uroven }
            ciele.filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }
                .sortedBy { it.priorita }
                .mapNotNull { c ->
                    val max = maxy[c.kluc] ?: return@mapNotNull null
                    ZrucnostNaDomove(
                        kluc = c.kluc,
                        nazov = c.nazov,
                        uroven = (aktualne[c.kluc] ?: 1).coerceIn(1, max),
                        max = max,
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repo.detiTok().collectLatest { _deti.value = it }
        }
        viewModelScope.launch {
            _bielkoviny.value = repo.bielkovinyDnes()
            // Otázka na prekážku: po 4+ dňoch pauzy, najviac raz denne.
            val dni = repo.dniOdTreningu()
            if (dni != null && dni >= 4 && repo.prefs.prekazkaDen != repo.dnes()) {
                _dniPauzy.value = dni
            }
        }
        viewModelScope.launch {
            // Po každom novom teste sa výzva na premeranie prehodnotí (inak by visela aj po teste).
            repo.denik.vsetkyTestyTok().collectLatest {
                _retest.value = if (repo.jeCasNaRetest()) {
                    val posledny = repo.denik.poslednyTestDen() ?: repo.dnes()
                    RetestNaDomove(
                        tyzdnov = ((repo.dnes() - posledny) / 7).coerceAtLeast(Pravidla.RETEST_PO_DNOCH / 7L),
                        dennaDoba = repo.upozornenieNaDennuDobu(),
                    )
                } else {
                    null
                }
            }
        }
        viewModelScope.launch {
            combine(repo.profilTok(), repo.cieleTok(), _minuty) { p, c, m -> Triple(p, c, m) }
                .collectLatest { (p, c, m) ->
                    val profil = p ?: repo.profil()
                    _profil.value = profil
                    _ciele.value = c
                    _pocetCvikov.value = repo.katalog.pocetCvikov()
                    _prvyMesiac.value =
                        profil.zaciatokEpochDen == 0L || (repo.dnes() - profil.zaciatokEpochDen) < 28
                    _plan.value = runCatching { repo.zostavPlan(m) }
                        .onFailure { _chyba.value = it.message ?: it.javaClass.simpleName }
                        .getOrNull()
                    if (_plan.value != null) _chyba.value = null
                }
        }
    }

    fun nastavCas(m: Int) {
        repo.prefs.poslednyCasMinut = m
        _minuty.value = m
    }

    fun prepniBielkoviny() {
        viewModelScope.launch { _bielkoviny.value = repo.prepniBielkoviny() }
    }

    /** Odpoveď na prekážku. Pri „nemal som čas" sa rovno prepne na 15 minút. */
    fun zapisPrekazku(druh: String) {
        repo.prefs.prekazkaDen = repo.dnes()
        _dniPauzy.value = null
        viewModelScope.launch {
            repo.zapisPrekazku(druh)
            if (druh == "cas") nastavCas(15)
        }
    }

    fun zavriPrekazku() {
        repo.prefs.prekazkaDen = repo.dnes()
        _dniPauzy.value = null
    }
}

/** Ponúkané dĺžky tréningu v minútach. */
private val DLZKY = listOf(15, 30, 45, 60)

@Composable
fun DomovObrazovka(
    naOnboarding: () -> Unit,
    naTest: () -> Unit,
    naPrehravac: (Int, Long) -> Unit,
    model: DomovModel = viewModel(),
) {
    val profil by model.profil.collectAsStateWithLifecycle()
    val plan by model.plan.collectAsStateWithLifecycle()
    val chyba by model.chyba.collectAsStateWithLifecycle()
    val pocetCvikov by model.pocetCvikov.collectAsStateWithLifecycle()
    val prvyMesiac by model.prvyMesiac.collectAsStateWithLifecycle()
    val minuty by model.minuty.collectAsStateWithLifecycle()
    val deti by model.deti.collectAsStateWithLifecycle()
    val bielkoviny by model.bielkoviny.collectAsStateWithLifecycle()
    val dniPauzy by model.dniPauzy.collectAsStateWithLifecycle()
    val retest by model.retest.collectAsStateWithLifecycle()
    val dniTyzdna by model.dniTyzdna.collectAsStateWithLifecycle()
    val suhrnTyzdna by model.suhrnTyzdna.collectAsStateWithLifecycle()
    val zrucnosti by model.zrucnosti.collectAsStateWithLifecycle()

    // Rodinný režim: max jedno aktívne dieťa na tréning (deti sa nestriedajú o displej).
    var sDietatom by rememberSaveable { mutableStateOf<Long?>(null) }
    // Dieťa medzičasom odstránené v Nastaveniach sa nesmie poslať do prehrávača.
    val vybraneDieta = sDietatom?.takeIf { id -> deti.any { it.id == id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Hlavicka()

        // Prázdna knižnica je jediná chyba, ktorú používateľ nevie obísť - povedať to nahlas.
        if (pocetCvikov == 0) {
            UpozornenieKarta(
                titulok = "Knižnica cvikov sa nenačítala",
                text = "V appke je 0 cvikov, takže sa nedá poskladať nič. Je to chyba appky, " +
                    "nie tvoja — pomôže preinštalovanie, inak ju treba nahlásiť.",
            )
        }

        // Otázka na prekážku po výpadku. Nie „prečo si zlyhal", ale „čo prekážalo" -
        // riešenie prekážky je podľa meta-regresie najúčinnejšia zložka udržania návyku.
        // Karta po odpovedi zbalí - počas odchodu sa ukazuje posledný známy počet dní.
        var zobrazenaPauza by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(dniPauzy) { if (dniPauzy != null) zobrazenaPauza = dniPauzy }
        AnimatedVisibility(
            visible = dniPauzy != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            PrekazkaKarta(
                dni = dniPauzy ?: zobrazenaPauza ?: 0L,
                naOdpoved = model::zapisPrekazku,
                naZavri = model::zavriPrekazku,
            )
        }

        val p = profil
        when {
            p == null -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !p.onboardingHotovy -> KrokKarta(
                krok = 1,
                titulok = "Najprv mi povedz, o čo ti ide",
                text = "Ciele a náradie. Bez nich ti neviem poskladať tréning, ktorý dáva zmysel.",
                rebrikPostavicky = "stojka",
                tlacidlo = "Nastaviť ciele",
                akcia = naOnboarding,
            )

            !p.vstupnyTestHotovy -> KrokKarta(
                krok = 2,
                titulok = "Vstupný test",
                text = "Pri každom cviku vyberieš najťažší stupeň, ktorý zvládneš. Pár minút — " +
                    "inak by prvý mesiac bol buď trápne ľahký, alebo nesplniteľný.",
                rebrikPostavicky = "kliky",
                tlacidlo = "Spraviť test",
                akcia = naTest,
            )

            else -> {
                DnesnyTrening(
                    plan = plan,
                    chyba = chyba,
                    minuty = minuty,
                    naMinuty = model::nastavCas,
                    deti = deti,
                    sDietatom = vybraneDieta,
                    naDieta = { sDietatom = it },
                    naStart = { naPrehravac(minuty, vybraneDieta ?: -1L) },
                    naOnboarding = naOnboarding,
                )

                TyzdenKarta(suhrn = suhrnTyzdna, dni = dniTyzdna)

                if (zrucnosti.isNotEmpty()) {
                    ZrucnostiSekcia(zrucnosti)
                }

                retest?.let { r -> RetestKarta(retest = r, naTest = naTest) }

                val bloky = plan?.bloky.orEmpty()
                if (bloky.isNotEmpty()) {
                    PlanKarta(bloky = bloky, zdovodnenie = plan?.zdovodnenie.orEmpty())
                }

                // Bielkoviny: jedno číslo, jedno ťuknutie. Žiadny jedálniček.
                BielkovinyKarta(
                    splnene = bielkoviny,
                    cielG = p.bielkovinyCielG,
                    naPrepni = model::prepniBielkoviny,
                )
            }
        }

        if (prvyMesiac) {
            val zaciatok = p?.zaciatokEpochDen ?: 0L
            PrvyMesiacKarta(
                denOdZaciatku = if (zaciatok > 0L) LocalDate.now().toEpochDay() - zaciatok else null,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ------------------------------------------------------------------ hlavička

private val DNI_TYZDNA = listOf("Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota", "Nedeľa")
private val MESIACE_GENITIV = listOf(
    "januára", "februára", "marca", "apríla", "mája", "júna",
    "júla", "augusta", "septembra", "októbra", "novembra", "decembra",
)

private fun datumSlovom(d: LocalDate): String =
    "${DNI_TYZDNA[d.dayOfWeek.value - 1]} ${d.dayOfMonth}. ${MESIACE_GENITIV[d.monthValue - 1]}"

private fun pozdrav(hodina: Int): String = when (hodina) {
    in 4..9 -> "Dobré ráno"
    in 10..17 -> "Dobrý deň"
    else -> "Dobrý večer"
}

@Composable
private fun Hlavicka() {
    val teraz = LocalDateTime.now()
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            datumSlovom(teraz.toLocalDate()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(pozdrav(teraz.hour), style = MaterialTheme.typography.headlineLarge)
    }
}

// ------------------------------------------------------------------ dnešný tréning (hero)

private enum class StavPlanu { NACITAVA, PRAZDNY, PLAN }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DnesnyTrening(
    plan: PlanTreningu?,
    chyba: String?,
    minuty: Int,
    naMinuty: (Int) -> Unit,
    deti: List<DietaEntity>,
    sDietatom: Long?,
    naDieta: (Long?) -> Unit,
    naStart: () -> Unit,
    naOnboarding: () -> Unit,
) {
    val stav = when {
        plan == null && chyba == null -> StavPlanu.NACITAVA
        plan == null || plan.bloky.isEmpty() -> StavPlanu.PRAZDNY
        else -> StavPlanu.PLAN
    }
    TonovanaKarta(modifier = Modifier.animateContentSize()) {
        NadpisSekcie("Dnešný tréning", modifier = Modifier.padding(bottom = 4.dp))

        AnimatedContent(
            targetState = stav,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "stavPlanu",
        ) { s ->
            when (s) {
                StavPlanu.NACITAVA -> Box(
                    Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                StavPlanu.PRAZDNY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Z tvojho nastavenia sa nedá nič poskladať", style = stylNadpisuKarty())
                    Text(
                        buildString {
                            append("Najčastejšia príčina: nie je zapnuté žiadne náradie ")
                            append("alebo nie je vybraný ani jeden cieľ.")
                            chyba?.let { append("\n\nChyba: $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = naOnboarding,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Otvoriť ciele a náradie")
                    }
                }

                StavPlanu.PLAN -> {
                    val aktualny = plan ?: return@AnimatedContent
                    SuhrnPlanu(aktualny)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PrepinacDlzky(moznosti = DLZKY, vybrana = minuty, naVyber = naMinuty)

        // Rodinný režim - deti cvičia v tvojich pauzách, ten istý tréning.
        if (deti.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            NadpisSekcie("Poďme spolu")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AvatarCip(
                    meno = "Sám",
                    farba = MaterialTheme.colorScheme.primary,
                    vybrany = sDietatom == null,
                    ikona = Icons.Filled.Person,
                    onClick = { naDieta(null) },
                )
                deti.forEach { d ->
                    AvatarCip(
                        meno = d.meno,
                        farba = farbaDietata(d),
                        vybrany = sDietatom == d.id,
                        onClick = { naDieta(d.id) },
                    )
                }
            }
        }

        if (stav == StavPlanu.PLAN) {
            Spacer(Modifier.height(20.dp))
            VelkeTlacidlo(
                text = if (sDietatom != null) "Začať spolu" else "Začať tréning",
                onClick = naStart,
                ikona = Icons.Filled.PlayArrow,
            )
        }
    }
}

/** Čísla tréningu ako hrdinovia + farebné kapsuly blokov + "čiarový kód" cvikov. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuhrnPlanu(plan: PlanTreningu) {
    val polozky = plan.vsetkyPolozky
    val serie = polozky.sumOf { it.second.serie }
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "≈",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp, bottom = 8.dp),
            )
            AnimovaneCislo(
                hodnota = plan.odhadMinut,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 68.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "min",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MaleCislo(polozky.size, sklonuj(polozky.size, "cvik", "cviky", "cvikov"))
            MaleCislo(serie, sklonuj(serie, "séria", "série", "sérií"))
            MaleCislo(plan.bloky.size, sklonuj(plan.bloky.size, "blok", "bloky", "blokov"))
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            plan.bloky.forEach { b ->
                Kapsula(text = b.blok.nazov, farba = citatelna(FarbyBlokov.pre(b.blok)))
            }
        }
        Spacer(Modifier.height(14.dp))
        // Každý cvik jeden segment vo farbe svojho bloku - tvar tréningu na prvý pohľad.
        val farby = polozky.map { FarbyBlokov.pre(it.first) }
        SegmentovyPruh(farby = farby, aktualny = farby.size, vyska = 8.dp)
    }
}

@Composable
private fun MaleCislo(n: Int, slovo: String) {
    Text(
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = Grotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            ) { append("$n") }
            append(" $slovo")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Segmentový prepínač dĺžky tréningu - posuvný indikátor, haptický tik pri zmene. */
@Composable
private fun PrepinacDlzky(moznosti: List<Int>, vybrana: Int, naVyber: (Int) -> Unit) {
    val haptika = rememberHaptika()
    val index = moznosti.indexOf(vybrana)
    val farba = MaterialTheme.colorScheme.primary
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        val sirka = maxWidth / moznosti.size
        val posun by animateDpAsState(
            targetValue = sirka * index.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "prepinacDlzky",
        )
        if (index >= 0) {
            Box(
                Modifier
                    .offset(x = posun)
                    .width(sirka)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(farba)
            )
        }
        Row(Modifier.fillMaxSize()) {
            moznosti.forEachIndexed { i, m ->
                val vybrany = i == index
                val farbaTextu by animateColorAsState(
                    if (vybrany) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "textDlzky",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = vybrany,
                            role = Role.Tab,
                            onClick = {
                                if (!vybrany) {
                                    haptika.tik()
                                    naVyber(m)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 19.sp)) {
                                append("$m")
                            }
                            append(" min")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = farbaTextu,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ rodinný režim

/** Farby avatarov detí. DietaEntity.farba zatiaľ ostáva 0 - vtedy sa farba odvodí od id. */
private val FARBY_DETI = listOf(
    Color(0xFFF2B138), // slnečná žltá - detský akcent appky
    Color(0xFF4FA3C7), // nebeská
    Color(0xFF9B7BD1), // levanduľová
    Color(0xFFE07A9B), // malinová
    Color(0xFF6BAF5B), // trávová
)

private fun farbaDietata(d: DietaEntity): Color =
    if (d.farba != 0) Color(d.farba) else FARBY_DETI[Math.floorMod(d.id, FARBY_DETI.size.toLong()).toInt()]

/** Písmo na farebnom podklade - tmavé na svetlej farbe, biele na tmavej. */
private fun textNa(farba: Color): Color = if (farba.luminance() > 0.18f) Color(0xFF1C1917) else Color.White

@Composable
private fun AvatarCip(
    meno: String,
    farba: Color,
    vybrany: Boolean,
    onClick: () -> Unit,
    ikona: ImageVector? = null,
) {
    val haptika = rememberHaptika()
    val zaklad = MaterialTheme.colorScheme.surfaceContainerHigh
    val pozadie by animateColorAsState(
        if (vybrany) farba.copy(alpha = 0.22f).compositeOver(zaklad) else zaklad,
        label = "avatarPozadie",
    )
    val okraj by animateColorAsState(if (vybrany) farba else Color.Transparent, label = "avatarOkraj")
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(pozadie)
            .border(2.dp, okraj, CircleShape)
            .selectable(selected = vybrany, role = Role.RadioButton, onClick = { haptika.klik(); onClick() })
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(farba),
            contentAlignment = Alignment.Center,
        ) {
            when {
                vybrany -> Icon(Icons.Filled.Check, contentDescription = null, tint = textNa(farba), modifier = Modifier.size(20.dp))
                ikona != null -> Icon(ikona, contentDescription = null, tint = textNa(farba), modifier = Modifier.size(20.dp))
                else -> Text(
                    meno.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
                    color = textNa(farba),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(meno, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ------------------------------------------------------------------ týždeň a ciele

@Composable
private fun TyzdenKarta(suhrn: SuhrnTreningov, dni: Set<Long>) {
    TonovanaKarta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimovaneCislo(
                hodnota = suhrn.pocet,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${sklonuj(suhrn.pocet, "tréning", "tréningy", "tréningov")} tento týždeň",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (suhrn.trvanieMs > 0) {
                Text(
                    formatujTrvanie(suhrn.trvanieMs),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TyzdenPas(dniSTreningom = dni)
    }
}

@Composable
private fun ZrucnostiSekcia(zrucnosti: List<ZrucnostNaDomove>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NadpisSekcie("Ciele zručností")
        zrucnosti.chunked(2).forEach { riadok ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                riadok.forEach { z -> ZrucnostDlazdica(z, Modifier.weight(1f)) }
                if (riadok.size == 1 && zrucnosti.size > 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ZrucnostDlazdica(z: ZrucnostNaDomove, modifier: Modifier = Modifier) {
    val farba = FarbyBlokov.pre(Blok.SKILL)
    TonovanaKarta(modifier = modifier, vnutorneOdsadenie = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KruhPostupu(
                postup = z.uroven.toFloat() / z.max.coerceAtLeast(1),
                modifier = Modifier.size(56.dp),
                farba = farba,
                hrubka = 6.dp,
                plynulo = false,
            ) {
                Text(
                    "${z.uroven}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    z.nazov,
                    style = stylNadpisuKarty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "stupeň ${z.uroven} z ${z.max}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ plán

@Composable
private fun PlanKarta(bloky: List<PlanBloku>, zdovodnenie: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NadpisSekcie("Plán")
        TonovanaKarta(modifier = Modifier.animateContentSize()) {
            bloky.forEachIndexed { i, blok ->
                RiadokBloku(blok = blok, posledny = i == bloky.lastIndex)
            }
            if (zdovodnenie.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                zdovodnenie.forEach { veta ->
                    Row(Modifier.padding(top = 6.dp)) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            veta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Jeden blok plánu na farebnej časovej osi: bodka + zvislá linka + obsah. */
@Composable
private fun RiadokBloku(blok: PlanBloku, posledny: Boolean) {
    val farba = FarbyBlokov.pre(blok.blok)
    // IntrinsicSize dá riadku pevnú výšku, aby zvislá linka vedela vyplniť zvyšok.
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.width(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(farba)
            )
            if (!posledny) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(3.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(farba.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = if (posledny) 0.dp else 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    blok.blok.nazov,
                    style = MaterialTheme.typography.titleLarge,
                    color = citatelna(farba),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${blok.polozky.size} ${sklonuj(blok.polozky.size, "cvik", "cviky", "cvikov")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            blok.polozky.forEach { polozka ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        polozka.cvik.nazov,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        davka(polozka),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "3 × 8", "3 × 30 s", "2 × 20 m" - série × cieľ jednej série. */
private fun davka(p: PlanPolozka): String = when (p.cvik.meranie) {
    "vydrz", "vzdialenost" -> "${p.serie} × ${formatujHodnotu(p.ciel, p.cvik.meranie)}"
    else -> "${p.serie} × ${p.ciel}"
}

// ------------------------------------------------------------------ menšie karty

@Composable
private fun KrokKarta(
    krok: Int,
    titulok: String,
    text: String,
    rebrikPostavicky: String,
    tlacidlo: String,
    akcia: () -> Unit,
) {
    TonovanaKarta(vnutorneOdsadenie = 0.dp) {
        Column(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Kapsula(text = "Krok $krok z 2", farba = MaterialTheme.colorScheme.primary)
            PrazdnyStav(
                titulok = titulok,
                text = text,
                rebrikPostavicky = rebrikPostavicky,
                tlacidlo = tlacidlo,
                akcia = akcia,
            )
        }
    }
}

@Composable
private fun UpozornenieKarta(titulok: String, text: String) {
    val farba = MaterialTheme.colorScheme.error
    TonovanaKarta(farba = farba) {
        Row {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = farba, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulok, style = stylNadpisuKarty())
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrekazkaKarta(dni: Long, naOdpoved: (String) -> Unit, naZavri: () -> Unit) {
    val moznosti = listOf(
        "cas" to "Nemal som čas",
        "bolest" to "Niečo ma bolelo",
        "nechcelo_sa" to "Nechcelo sa mi",
        "prec" to "Boli sme preč",
    )
    val pocet = dni.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    TonovanaKarta(farba = MaterialTheme.colorScheme.tertiary) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(
                    "$pocet ${sklonuj(pocet, "deň", "dni", "dní")} pauza — čo prekážalo?",
                    style = stylNadpisuKarty(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nie je to výčitka. Podľa odpovede upravím, čo ti nabudúce ponúknem.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = naZavri) {
                Icon(Icons.Filled.Close, contentDescription = "Zavrieť")
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moznosti.chunked(2).forEach { riadok ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    riadok.forEach { (druh, popis) ->
                        OutlinedButton(
                            onClick = { naOdpoved(druh) },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(popis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetestKarta(retest: RetestNaDomove, naTest: () -> Unit) {
    val farba = MaterialTheme.colorScheme.primary
    TonovanaKarta(farba = farba) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonaVKruhu(Icons.Filled.Replay, farba)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Čas na premeranie", style = stylNadpisuKarty())
                Text(
                    "Posledný test bol pred ${retest.tyzdnov} týždňami.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        retest.dennaDoba?.let { upozornenie ->
            Spacer(Modifier.height(12.dp))
            Row {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp).size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    upozornenie,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = naTest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Premerať sa")
        }
    }
}

@Composable
private fun BielkovinyKarta(splnene: Boolean, cielG: Int, naPrepni: () -> Unit) {
    val farba = MaterialTheme.colorScheme.secondary
    TonovanaKarta(
        modifier = Modifier.semantics { stateDescription = if (splnene) "splnené" else "nesplnené" },
        farba = if (splnene) farba else null,
        onClick = naPrepni,
        vnutorneOdsadenie = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonaVKruhu(Icons.Filled.Egg, farba)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Bielkoviny dnes", style = stylNadpisuKarty())
                Text(
                    "cieľ ~$cielG g (1,6–2,2 g na kilo)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ZnackaSplnenia(splnene = splnene, farba = farba)
        }
    }
}

/** Kruh, ktorý sa po splnení vyplní a ukáže fajku. */
@Composable
private fun ZnackaSplnenia(splnene: Boolean, farba: Color) {
    val pozadie by animateColorAsState(if (splnene) farba else Color.Transparent, label = "znacka")
    val okraj by animateColorAsState(
        if (splnene) farba else MaterialTheme.colorScheme.outline,
        label = "znackaOkraj",
    )
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(pozadie)
            .border(2.dp, okraj, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = splnene, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Najdôležitejší text v celej appke. Bez neho ľudia v treťom týždni prestanú,
 * lebo v zrkadle nič nevidia - a nevedia, že to tak má byť.
 */
@Composable
private fun PrvyMesiacKarta(denOdZaciatku: Long?) {
    var rozbalene by rememberSaveable { mutableStateOf(false) }
    val farba = MaterialTheme.colorScheme.secondary
    TonovanaKarta(farba = farba, modifier = Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonaVKruhu(Icons.Filled.Psychology, farba)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Prvý mesiac", style = stylNadpisuKarty())
                if (denOdZaciatku != null) {
                    Text(
                        "${(denOdZaciatku / 7 + 1).coerceIn(1, 4)}. týždeň zo 4",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (denOdZaciatku != null) {
            Spacer(Modifier.height(12.dp))
            PasPlnenia(postup = (denOdZaciatku + 1) / 28f, farba = farba, vyska = 6.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Teraz rastie hlavne nervový systém, nie sval. Čísla pôjdu hore rýchlo, " +
                "zrkadlo príde okolo šiesteho týždňa. Nie je to výhovorka — je to poradie, " +
                "v akom to funguje.",
            style = MaterialTheme.typography.bodyMedium,
        )
        AnimatedVisibility(visible = rozbalene) {
            Text(
                "V prvých dvoch až štyroch týždňoch je prakticky celý nárast sily neurálny: " +
                    "mozog sa učí zapájať viac svalových vlákien naraz a rýchlejšie. Merateľné " +
                    "zväčšenie svalu sa v štúdiách objavuje až okolo šiesteho týždňa. Preto ti " +
                    "appka v tomto období ukazuje opakovania a výdrže, nie obvody.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        TextButton(
            onClick = { rozbalene = !rozbalene },
            modifier = Modifier.padding(top = 4.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        ) {
            Text(if (rozbalene) "Skryť" else "Prečo to tak je")
        }
    }
}

// ------------------------------------------------------------------ drobnosti

@Composable
private fun IkonaVKruhu(ikona: ImageVector, farba: Color) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(farba.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ikona, contentDescription = null, tint = citatelna(farba), modifier = Modifier.size(24.dp))
    }
}

/** Nadpis malej karty - Grotesk, o stupeň menší než titleLarge. */
@Composable
private fun stylNadpisuKarty(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

/**
 * Farby blokov sú stredne svetlé - na tmavom podklade čitateľné, na svetlom papieri
 * nie. Vo svetlom režime sa preto text vo farbe bloku stmaví.
 */
@Composable
private fun citatelna(farba: Color): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) lerp(farba, Color.Black, 0.3f) else farba
