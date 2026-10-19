package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.SeriaEntity
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.katalog.rozparsujPartie
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Skupina
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.komponenty.Pozy
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.formatujHodnotu
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk
import java.text.Normalizer
import java.time.LocalDate

// ================================================================== knižnica

class KniznicaModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebriky = MutableStateFlow<List<RebrikEntity>>(emptyList())
    val rebriky = _rebriky.asStateFlow()

    private val _urovne = MutableStateFlow<Map<String, Int>>(emptyMap())
    val urovne = _urovne.asStateFlow()

    /** rebrikId -> stupne zoradené od najľahšieho (dospelé aj detské). */
    private val _cviky = MutableStateFlow<Map<String, List<CvikEntity>>>(emptyMap())
    val cviky = _cviky.asStateFlow()

    /** rebrikId -> text na hľadanie bez diakritiky (názov rebríka, skupina, názvy stupňov). */
    private val _hladaciText = MutableStateFlow<Map<String, String>>(emptyMap())
    val hladaciText = _hladaciText.asStateFlow()

    /** Kľúče aktívnych cieľov: zručnosti (= id rebríka) v poradí priority a partie. */
    private val _cieleZrucnosti = MutableStateFlow<List<String>>(emptyList())
    val cieleZrucnosti = _cieleZrucnosti.asStateFlow()
    private val _cielePartie = MutableStateFlow<Set<String>>(emptySet())
    val cielePartie = _cielePartie.asStateFlow()

    private val _naradie = MutableStateFlow<Set<Naradie>>(emptySet())
    val naradie = _naradie.asStateFlow()

    // Tokom, nie jednorazovo: katalóg sa načítava na pozadí a úrovne sa menia po každom tréningu.
    init {
        viewModelScope.launch {
            repo.rebrikyTok().collectLatest { zoznam ->
                val vsetky = (repo.katalog.vsetkyDospeleCviky() + repo.katalog.detskeCviky())
                    .groupBy { it.rebrikId }
                    .mapValues { (_, c) -> c.sortedBy { it.uroven } }
                _cviky.value = vsetky
                _hladaciText.value = zoznam.associate { r ->
                    r.id to bezDiakritiky(
                        buildString {
                            append(r.nazov).append(' ')
                            append(Skupina.z(r.skupina).nazov).append(' ')
                            vsetky[r.id].orEmpty().forEach { append(it.nazov).append(' ') }
                        }
                    )
                }
                _rebriky.value = zoznam
            }
        }
        viewModelScope.launch {
            repo.denik.urovneTok().collectLatest { zoznam ->
                _urovne.value = zoznam.associate { it.rebrikId to it.uroven }
            }
        }
        viewModelScope.launch {
            repo.cieleTok().collectLatest { ciele ->
                _cieleZrucnosti.value = ciele
                    .filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }
                    .sortedBy { it.priorita }
                    .map { it.kluc }
                _cielePartie.value = ciele
                    .filter { DruhCiela.z(it.druh) == DruhCiela.PARTIA }
                    .map { it.kluc }
                    .toSet()
            }
        }
        viewModelScope.launch {
            repo.profilTok().collectLatest { _naradie.value = repo.naradie() }
        }
    }
}

@Composable
fun KniznicaObrazovka(naRebrik: (String) -> Unit, model: KniznicaModel = viewModel()) {
    val rebriky by model.rebriky.collectAsStateWithLifecycle()
    val urovne by model.urovne.collectAsStateWithLifecycle()
    val cviky by model.cviky.collectAsStateWithLifecycle()
    val hladaciText by model.hladaciText.collectAsStateWithLifecycle()
    val cieleZrucnosti by model.cieleZrucnosti.collectAsStateWithLifecycle()
    val cielePartie by model.cielePartie.collectAsStateWithLifecycle()
    val naradie by model.naradie.collectAsStateWithLifecycle()

    var hladanie by rememberSaveable { mutableStateOf("") }
    var vybranaSkupina by rememberSaveable { mutableStateOf<String?>(null) }

    // Skupiny v poradí, v akom idú rebríky v katalógu (rozohriatie, zručnosti, tlak, ťah…).
    val skupiny = remember(rebriky) { rebriky.map { it.skupina }.distinct() }
    val dopyt = remember(hladanie) { bezDiakritiky(hladanie.trim()) }

    val viditelne = rebriky.filter { r ->
        (vybranaSkupina == null || r.skupina == vybranaSkupina) &&
            (dopyt.isEmpty() || hladaciText[r.id].orEmpty().contains(dopyt))
    }

    // Ciele hore: najprv cieľové zručnosti podľa priority, potom rebríky, ktoré priamo
    // zaťažujú partiu zvolenú ako cieľ (aspoň jeden stupeň s váhou 1).
    val cieloveId = buildList {
        cieleZrucnosti.forEach { if (it !in this) add(it) }
        if (cielePartie.isNotEmpty()) {
            rebriky.forEach { r ->
                if (r.id in this || Skupina.z(r.skupina) == Skupina.DETSKY) return@forEach
                val zasahuje = cviky[r.id].orEmpty().any { c ->
                    rozparsujPartie(c.partie).any { (kluc, vaha) -> kluc in cielePartie && vaha >= 1.0 }
                }
                if (zasahuje) add(r.id)
            }
        }
    }
    val cielove = cieloveId.mapNotNull { id -> viditelne.firstOrNull { it.id == id } }
    val ostatne = viditelne.filter { it.id !in cieloveId }
    val zoskupit = vybranaSkupina == null && dopyt.isEmpty()

    @Composable
    fun KartaZoznamu(r: RebrikEntity, modifier: Modifier) {
        val stupne = cviky[r.id].orEmpty()
        val u = urovne[r.id]
        val aktualny = stupne.firstOrNull { it.uroven == (u ?: 1) }
        // Zhoda hľadania v stupni (nie v názve rebríka) - ukáže sa, ktorý stupeň to je.
        val zhoda = if (dopyt.isNotEmpty() && !bezDiakritiky(r.nazov).contains(dopyt)) {
            stupne.firstOrNull { bezDiakritiky(it.nazov).contains(dopyt) }
        } else {
            null
        }
        KartaRebrika(
            rebrik = r,
            uroven = u,
            maxUroven = stupne.maxOfOrNull { it.uroven } ?: 0,
            pocetStupnov = stupne.size,
            zamknutyAktualny = aktualny != null && chybajuceNaradie(aktualny, naradie).isNotEmpty(),
            zhoda = zhoda,
            onClick = { naRebrik(r.id) },
            modifier = modifier,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "hlavicka") {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Text("Cviky", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Každý cvik je rebrík stupňov. Postupuje sa na ťažšiu variáciu, nie na viac " +
                        "opakovaní donekonečna — to je celý princíp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "hladanie") {
            PoleHladania(
                text = hladanie,
                zmena = { hladanie = it },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item(key = "filtre") {
            LazyRow(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "vsetko") {
                    CipFiltra(
                        text = "Všetko",
                        vybrany = vybranaSkupina == null,
                        farba = MaterialTheme.colorScheme.primary,
                        onClick = { vybranaSkupina = null },
                    )
                }
                items(skupiny, key = { it }) { kluc ->
                    val skupina = Skupina.z(kluc)
                    CipFiltra(
                        text = skupina.nazov,
                        vybrany = vybranaSkupina == kluc,
                        farba = citatelnaFarba(farbaSkupiny(skupina)),
                        onClick = { vybranaSkupina = if (vybranaSkupina == kluc) null else kluc },
                    )
                }
            }
        }

        when {
            rebriky.isEmpty() -> item(key = "nacitavam") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            viditelne.isEmpty() -> item(key = "prazdne") {
                PrazdnyStav(
                    titulok = "Nič také tu nie je",
                    text = "Skús iné slovo — hľadá sa v názvoch rebríkov aj jednotlivých stupňov.",
                    rebrikPostavicky = "drep",
                    tlacidlo = "Zrušiť hľadanie",
                    akcia = { hladanie = ""; vybranaSkupina = null },
                    modifier = Modifier.animateItem(),
                )
            }

            else -> {
                if (cielove.isNotEmpty()) {
                    item(key = "h-ciele") {
                        NadpisSekcie("Tvoje ciele", Modifier.padding(horizontal = 20.dp).animateItem())
                    }
                    items(cielove, key = { "c-" + it.id }) { r ->
                        KartaZoznamu(r, Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem())
                    }
                }
                if (zoskupit) {
                    skupiny.forEach { kluc ->
                        val vSkupine = ostatne.filter { it.skupina == kluc }
                        if (vSkupine.isNotEmpty()) {
                            item(key = "h-$kluc") {
                                NadpisSekcie(
                                    Skupina.z(kluc).nazov,
                                    Modifier.padding(horizontal = 20.dp).animateItem(),
                                )
                            }
                            items(vSkupine, key = { "r-" + it.id }) { r ->
                                KartaZoznamu(r, Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem())
                            }
                        }
                    }
                } else if (ostatne.isNotEmpty()) {
                    item(key = "h-vysledky") {
                        val nadpis = if (dopyt.isNotEmpty()) {
                            "${ostatne.size + cielove.size} " +
                                sklonuj(ostatne.size + cielove.size, "výsledok", "výsledky", "výsledkov")
                        } else {
                            Skupina.z(vybranaSkupina.orEmpty()).nazov
                        }
                        NadpisSekcie(nadpis, Modifier.padding(horizontal = 20.dp).animateItem())
                    }
                    items(ostatne, key = { "r-" + it.id }) { r ->
                        KartaZoznamu(r, Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun PoleHladania(text: String, zmena: (String) -> Unit, modifier: Modifier = Modifier) {
    val klavesnica = LocalSoftwareKeyboardController.current
    TextField(
        value = text,
        onValueChange = zmena,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Hľadať cvik alebo stupeň") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (text.isNotEmpty()) {
            {
                IconButton(onClick = { zmena("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Vymazať hľadanie")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { klavesnica?.hide() }),
    )
}

@Composable
private fun CipFiltra(text: String, vybrany: Boolean, farba: Color, onClick: () -> Unit) {
    val haptika = rememberHaptika()
    FilterChip(
        selected = vybrany,
        onClick = { haptika.klik(); onClick() },
        label = { Text(text) },
        leadingIcon = if (vybrany) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = farba.copy(alpha = 0.18f),
            selectedLabelColor = farba,
            selectedLeadingIconColor = farba,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = vybrany,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = farba.copy(alpha = 0.45f),
            selectedBorderWidth = 1.dp,
        ),
    )
}

@Composable
private fun KartaRebrika(
    rebrik: RebrikEntity,
    uroven: Int?,
    maxUroven: Int,
    pocetStupnov: Int,
    zamknutyAktualny: Boolean,
    zhoda: CvikEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skupina = Skupina.z(rebrik.skupina)
    val zakladna = farbaSkupiny(skupina)
    val akcent = citatelnaFarba(zakladna)
    val detsky = skupina == Skupina.DETSKY

    TonovanaKarta(modifier = modifier, onClick = onClick, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KruhPostavicky(rebrik.id, zakladna, akcent, velkost = 64)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rebrik.nazov,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (zamknutyAktualny) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Aktuálny stupeň potrebuje náradie",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                val popis = buildAnnotatedString {
                    append(skupina.nazov)
                    append("  ·  ")
                    when {
                        detsky -> append("$pocetStupnov ${sklonuj(pocetStupnov, "cvik", "cviky", "cvikov")} pre deti")
                        uroven != null && maxUroven > 0 -> {
                            append("stupeň ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = Grotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            ) { append("${uroven.coerceAtMost(maxUroven)} / $maxUroven") }
                        }
                        else -> append("$maxUroven ${sklonuj(maxUroven, "stupeň", "stupne", "stupňov")} · nezačaté")
                    }
                }
                Text(
                    popis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (zhoda != null) {
                    Text(
                        "Stupeň ${zhoda.uroven}: ${zhoda.nazov}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = akcent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!detsky && maxUroven > 0) {
                    Spacer(Modifier.height(10.dp))
                    PasPlnenia(
                        postup = (uroven ?: 0).toFloat() / maxUroven,
                        farba = akcent,
                        vyska = 6.dp,
                    )
                }
            }
        }
    }
}

/** Postavička v tónovanom kruhu; ak rebrík pózu nemá, náhradná ikona. */
@Composable
private fun KruhPostavicky(rebrikId: String, zakladna: Color, akcent: Color, velkost: Int, trvanieMs: Int = 1400) {
    Box(
        Modifier
            .size(velkost.dp)
            .clip(CircleShape)
            .background(tonovanie(zakladna)),
        contentAlignment = Alignment.Center,
    ) {
        if (Pozy.pre(rebrikId) != null) {
            Postavicka(
                rebrikId,
                Modifier.size((velkost * 0.82f).dp),
                farba = MaterialTheme.colorScheme.onSurface,
                farbaAkcent = akcent,
                trvanieMs = trvanieMs,
            )
        } else {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = akcent,
                modifier = Modifier.size((velkost * 0.4f).dp),
            )
        }
    }
}

// ================================================================== detail rebríka

class RebrikModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebrik = MutableStateFlow<RebrikEntity?>(null)
    val rebrik = _rebrik.asStateFlow()

    private val _cviky = MutableStateFlow<List<CvikEntity>>(emptyList())
    val cviky = _cviky.asStateFlow()

    private val _uroven = MutableStateFlow(1)
    val uroven = _uroven.asStateFlow()

    /** false = používateľ rebrík ešte necvičil (stupeň 1 je len predvolený). */
    private val _zacaty = MutableStateFlow(false)
    val zacaty = _zacaty.asStateFlow()

    private val _naradie = MutableStateFlow<Set<Naradie>>(emptySet())
    val naradie = _naradie.asStateFlow()

    /** Posledné odcvičené série v rebríku, najnovšie prvé. */
    private val _posledne = MutableStateFlow<List<SeriaEntity>>(emptyList())
    val posledne = _posledne.asStateFlow()

    fun nacitaj(rebrikId: String) {
        viewModelScope.launch {
            // Najprv všetko načítať, rebrík nastaviť ako posledný - obrazovka sa ukáže naraz,
            // bez bliknutia "všetko zamknuté", kým sa nedočíta náradie.
            val r = repo.katalog.rebrik(rebrikId)
            val c = repo.cvikyRebrika(rebrikId)
            val u = repo.urovenVRebriku(rebrikId)
            val z = repo.denik.uroven(rebrikId) != null
            val n = repo.naradie()
            val p = repo.denik.poslednéSerieRebrika(rebrikId, 30).filter { !it.preskocena }
            _cviky.value = c
            _uroven.value = u
            _zacaty.value = z
            _naradie.value = n
            _posledne.value = p
            _rebrik.value = r
        }
    }
}

private enum class StavStupna { HOTOVY, AKTUALNY, BUDUCI, NEUTRALNY }

@Composable
fun RebrikObrazovka(rebrikId: String, spat: () -> Unit, model: RebrikModel = viewModel()) {
    val rebrik by model.rebrik.collectAsStateWithLifecycle()
    val cviky by model.cviky.collectAsStateWithLifecycle()
    val uroven by model.uroven.collectAsStateWithLifecycle()
    val zacaty by model.zacaty.collectAsStateWithLifecycle()
    val naradie by model.naradie.collectAsStateWithLifecycle()
    val posledne by model.posledne.collectAsStateWithLifecycle()

    LaunchedEffect(rebrikId) { model.nacitaj(rebrikId) }

    val r = rebrik
    if (r == null || r.id != rebrikId) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            IconButton(onClick = spat) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
            }
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val skupina = Skupina.z(r.skupina)
    val detsky = skupina == Skupina.DETSKY
    val zakladna = farbaSkupiny(skupina)
    val akcent = citatelnaFarba(zakladna)
    val maxUroven = cviky.maxOfOrNull { it.uroven } ?: 0

    // Cviky vyžadujúce náradie, ktoré nemá zapnuté, sa nezamlčia - označia sa zámkom.
    val chybajuceVRebriku = cviky.flatMap { chybajuceNaradie(it, naradie) }.distinct()

    var rozbalene by remember(rebrikId, uroven) { mutableStateOf(if (detsky) emptySet() else setOf(uroven)) }
    val haptika = rememberHaptika()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "hlavicka") {
            HlavickaRebrika(
                rebrik = r,
                skupina = skupina,
                zakladna = zakladna,
                akcent = akcent,
                uroven = uroven,
                maxUroven = maxUroven,
                zacaty = zacaty,
                detsky = detsky,
                spat = spat,
            )
        }

        if (chybajuceVRebriku.isNotEmpty()) {
            item(key = "naradie") {
                TonovanaKarta(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    vnutorneOdsadenie = 16.dp,
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp).size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Stupne so zámkom potrebujú: ${chybajuceVRebriku.joinToString { it.nazov }}. " +
                                "Zapína sa v Nastaveniach → ciele a náradie.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val dni = posledne
            .groupBy { it.epochDen to it.uroven }
            .entries
            .take(3)
        if (dni.isNotEmpty() && !detsky) {
            item(key = "vykony") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    NadpisSekcie("Posledné výkony")
                    Spacer(Modifier.height(8.dp))
                    TonovanaKarta(vnutorneOdsadenie = 16.dp) {
                        dni.forEachIndexed { i, (kluc, serie) ->
                            if (i > 0) {
                                HorizontalDivider(
                                    Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                            RiadokVykonu(den = kluc.first, uroven = kluc.second, serie = serie.sortedBy { it.id }, akcent = akcent)
                        }
                    }
                }
            }
        }

        item(key = "h-stupne") {
            NadpisSekcie(
                if (detsky) "Cviky" else "Stupne",
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            )
        }

        itemsIndexed(cviky, key = { _, c -> c.id }) { i, c ->
            val stav = when {
                detsky -> StavStupna.NEUTRALNY
                c.uroven < uroven -> StavStupna.HOTOVY
                c.uroven == uroven -> StavStupna.AKTUALNY
                else -> StavStupna.BUDUCI
            }
            RiadokStupna(
                cvik = c,
                stav = stav,
                chybajuce = chybajuceNaradie(c, naradie),
                prvy = i == 0,
                posledny = i == cviky.lastIndex,
                rozbaleny = c.uroven in rozbalene,
                vykony = posledne.filter { it.uroven == c.uroven && it.epochDen == posledne.firstOrNull { p -> p.uroven == c.uroven }?.epochDen }
                    .sortedBy { it.id },
                zakladna = zakladna,
                akcent = akcent,
                naTuk = {
                    haptika.klik()
                    rozbalene = if (c.uroven in rozbalene) rozbalene - c.uroven else rozbalene + c.uroven
                },
            )
        }
    }
}

@Composable
private fun HlavickaRebrika(
    rebrik: RebrikEntity,
    skupina: Skupina,
    zakladna: Color,
    akcent: Color,
    uroven: Int,
    maxUroven: Int,
    zacaty: Boolean,
    detsky: Boolean,
    spat: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        IconButton(onClick = spat, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KruhPostavicky(rebrik.id, zakladna, akcent, velkost = 176, trvanieMs = 1600)
            Spacer(Modifier.height(20.dp))
            Text(
                rebrik.nazov,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kapsula(skupina.nazov, akcent)
                if (rebrik.jeZrucnost) {
                    Kapsula("Cieľová zručnosť", akcent, ikona = Icons.Outlined.TrackChanges)
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            if (!detsky && maxUroven > 0) {
                Spacer(Modifier.height(20.dp))
                TonovanaKarta(farba = zakladna) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimovaneCislo(
                            uroven.coerceAtMost(maxUroven),
                            style = MaterialTheme.typography.headlineLarge,
                            color = akcent,
                        )
                        Text(
                            " / $maxUroven",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        val zostava = maxUroven - uroven
                        Text(
                            when {
                                zostava <= 0 -> "Vrchol rebríka"
                                else -> "$zostava ${sklonuj(zostava, "stupeň", "stupne", "stupňov")} do vrcholu"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(
                        if (zacaty) "tvoj stupeň" else "zatiaľ necvičené — začína sa prvým stupňom",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    PasPlnenia(
                        postup = if (zacaty) uroven.toFloat() / maxUroven else 0f,
                        farba = akcent,
                    )
                }
            }
            rebrik.popis.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun RiadokVykonu(den: Long, uroven: Int, serie: List<SeriaEntity>, akcent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(96.dp)) {
            Text(popisDna(den), style = MaterialTheme.typography.labelLarge)
            Text(
                "stupeň $uroven",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            serie.joinToString("  ") { formatujHodnotu(it.skutocnaHodnota ?: 0, it.meranie) },
            style = MaterialTheme.typography.titleLarge,
            color = akcent,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
    }
}

// Rozmery časovej osi - bodka sedí na osi prvého riadku karty stupňa.
private val OKRAJ = 20.dp
private val KOLAJ = 40.dp
private val BODKA = 28.dp
private val BODKA_HORE = 14.dp

@Composable
private fun RiadokStupna(
    cvik: CvikEntity,
    stav: StavStupna,
    chybajuce: List<Naradie>,
    prvy: Boolean,
    posledny: Boolean,
    rozbaleny: Boolean,
    vykony: List<SeriaEntity>,
    zakladna: Color,
    akcent: Color,
    naTuk: () -> Unit,
) {
    val zamknuty = chybajuce.isNotEmpty()
    val stopa = MaterialTheme.colorScheme.outlineVariant
    val farbaHore = if (stav == StavStupna.HOTOVY || stav == StavStupna.AKTUALNY) akcent else stopa
    val farbaDole = if (stav == StavStupna.HOTOVY) akcent else stopa

    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = (OKRAJ + KOLAJ / 2).toPx()
                val stred = (BODKA_HORE + BODKA / 2).toPx()
                val polomer = (BODKA / 2).toPx() + 3.dp.toPx()
                val hrubka = 3.dp.toPx()
                if (!prvy) {
                    drawLine(farbaHore, Offset(x, 0f), Offset(x, stred - polomer), hrubka, StrokeCap.Round)
                }
                if (!posledny) {
                    drawLine(farbaDole, Offset(x, stred + polomer), Offset(x, size.height), hrubka, StrokeCap.Round)
                }
            }
            .padding(start = OKRAJ, end = OKRAJ, bottom = 10.dp),
    ) {
        Box(Modifier.width(KOLAJ).padding(top = BODKA_HORE), contentAlignment = Alignment.TopCenter) {
            BodkaStupna(cislo = cvik.uroven, stav = stav, zamknuty = zamknuty, akcent = akcent)
        }
        Spacer(Modifier.width(8.dp))
        TonovanaKarta(
            modifier = Modifier
                .weight(1f)
                .alpha(if (stav == StavStupna.BUDUCI && zamknuty) 0.72f else 1f),
            farba = if (stav == StavStupna.AKTUALNY) zakladna else null,
            onClick = naTuk,
            vnutorneOdsadenie = 16.dp,
        ) {
            Column(Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cvik.nazov,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (stav == StavStupna.AKTUALNY) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (zamknuty) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Potrebuje náradie",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp),
                        )
                    }
                    if (stav == StavStupna.AKTUALNY) {
                        Spacer(Modifier.width(8.dp))
                        Kapsula("Tu si", akcent)
                    }
                    val otocenie by animateFloatAsState(if (rozbaleny) 180f else 0f, label = "sipka")
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (rozbaleny) "Zbaliť" else "Rozbaliť pokyny",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp).rotate(otocenie),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Stupeň ${cvik.uroven} · cieľ ${cvik.cielMin}–${cvik.cielMax} ${jednotkaCiela(cvik.meranie)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (zamknuty) {
                    Text(
                        "Potrebuje: ${chybajuce.joinToString { it.nazov }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (rozbaleny) {
                    val pokyny = cvik.cue.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    val chyby = cvik.chyby.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    if (pokyny.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Ako na to", style = MaterialTheme.typography.labelLarge, color = akcent)
                        pokyny.forEach { Odrazka(it, akcent) }
                    }
                    if (chyby.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Na čo si dať pozor",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        chyby.forEach { Odrazka(it, MaterialTheme.colorScheme.error) }
                    }
                    if (vykony.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Naposledy · ${popisDna(vykony.first().epochDen)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            vykony.joinToString("  ") { formatujHodnotu(it.skutocnaHodnota ?: 0, it.meranie) },
                            style = MaterialTheme.typography.titleLarge,
                            color = akcent,
                        )
                    }
                    if (pokyny.isEmpty() && chyby.isEmpty() && vykony.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "K tomuto stupňu nie sú ďalšie pokyny.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BodkaStupna(cislo: Int, stav: StavStupna, zamknuty: Boolean, akcent: Color) {
    val naAkcente = if (akcent.luminance() > 0.45f) Color(0xFF1C1917) else Color.White
    Box(Modifier.size(BODKA), contentAlignment = Alignment.Center) {
        if (stav == StavStupna.AKTUALNY) {
            // Jemný "dych" okolo aktuálneho stupňa - jediný pohyb na osi, ukazuje kde si.
            val pulz = rememberInfiniteTransition(label = "pulz")
            val t by pulz.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart),
                label = "pulzT",
            )
            Box(
                Modifier
                    .size(BODKA)
                    .graphicsLayer {
                        val s = 1f + 0.55f * t
                        scaleX = s
                        scaleY = s
                        alpha = 0.35f * (1f - t)
                    }
                    .clip(CircleShape)
                    .background(akcent)
            )
        }
        when {
            stav == StavStupna.HOTOVY -> Box(
                Modifier.size(BODKA).clip(CircleShape).background(akcent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Hotové", tint = naAkcente, modifier = Modifier.size(16.dp))
            }

            stav == StavStupna.AKTUALNY -> Box(
                Modifier.size(BODKA).clip(CircleShape).background(akcent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$cislo",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk),
                    color = naAkcente,
                )
            }

            zamknuty -> Box(
                Modifier
                    .size(BODKA)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Zamknuté",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }

            else -> Box(
                Modifier
                    .size(BODKA)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$cislo",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Grotesk),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Odrazka(text: String, farba: Color) {
    Row(Modifier.padding(top = 6.dp)) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(farba)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ================================================================== pomocníci

/** Náradie, ktoré stupeň potrebuje a používateľ ho nemá zapnuté. */
private fun chybajuceNaradie(cvik: CvikEntity, mam: Set<Naradie>): List<Naradie> =
    cvik.naradie.split(",")
        .mapNotNull { Naradie.z(it.trim()) }
        .filter { it != Naradie.ZIADNE && it !in mam }

private fun jednotkaCiela(meranie: String): String = when (meranie) {
    "vydrz" -> "s"
    "vzdialenost" -> "m"
    else -> "opakovaní"
}

private fun popisDna(den: Long): String {
    val dnes = LocalDate.now().toEpochDay()
    return when (den) {
        dnes -> "Dnes"
        dnes - 1 -> "Včera"
        else -> LocalDate.ofEpochDay(den).let { "${it.dayOfMonth}. ${it.monthValue}." }
    }
}

private fun bezDiakritiky(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/**
 * Farba rebríka podľa skupiny - rovnaký jazyk ako bloky tréningu na Domove:
 * zručnosti = plameň, silové skupiny = zelenomodrá, mobilita = listová,
 * rozohriatie = jantár, detské = akcent detí.
 */
@Composable
private fun farbaSkupiny(skupina: Skupina): Color = when (skupina) {
    Skupina.ZRUCNOST -> FarbyBlokov.pre(Blok.SKILL)
    Skupina.MOBILITA -> FarbyBlokov.pre(Blok.MOBILITA)
    Skupina.ROZOHRIATIE -> FarbyBlokov.pre(Blok.ROZOHRIATIE)
    Skupina.DETSKY -> MaterialTheme.colorScheme.tertiary
    else -> FarbyBlokov.pre(Blok.SILA)
}

/** Variant farby čitateľný ako text/ikona na pozadí témy (svetlá tmavne, tmavá svetlie). */
@Composable
private fun citatelnaFarba(farba: Color): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) lerp(farba, Color.White, 0.30f)
    else lerp(farba, Color.Black, 0.28f)

/** Tónované pozadie kruhu - v tmavom režime o niečo sýtejšie, inak by tmavé farby zanikli. */
@Composable
private fun tonovanie(farba: Color): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) farba.copy(alpha = 0.26f)
    else farba.copy(alpha = 0.16f)
