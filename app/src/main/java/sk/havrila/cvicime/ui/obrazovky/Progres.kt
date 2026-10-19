package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Rekord
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.BodGrafu
import sk.havrila.cvicime.data.denik.MieraEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Partia
import sk.havrila.cvicime.domain.Objem
import sk.havrila.cvicime.domain.Pokrok
import sk.havrila.cvicime.domain.StavCiela
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.KruhPostupu
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.SKRATKY_DNI
import sk.havrila.cvicime.ui.komponenty.StatDlazdica
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.formatujHodnotu
import sk.havrila.cvicime.ui.komponenty.formatujTrvanie
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import java.time.LocalDate
import kotlin.math.abs

/** Kalendár aktivity ukazuje posledných 16 týždňov (aktuálny týždeň je posledný stĺpec). */
private const val TYZDNOV_KALENDARA = 16

data class ObjemRiadok(val partia: Partia, val odcvicene: Double, val ciel: Double, val prioritna: Boolean)

/** Graf sily jedného rebríka - najlepšia séria po dňoch. [meranie] určuje formát hodnôt ("12×", "45 s"). */
data class GrafRebrika(val rebrikId: String, val nazov: String, val body: List<BodGrafu>, val meranie: String)

/** Cieľ zručnosti: stav brán z domény + názvy stupňov rebríka z katalógu. */
data class ProgresZrucnost(
    val stav: StavCiela,
    val stupen: Int,
    val maxStupen: Int,
    val nazovStupna: String?,
    val dalsiStupen: String?,
)

/** Súhrnné čísla v hlavičke Pokroku. */
data class ProgresStatistiky(
    val treningov: Int,
    val trvanieMs: Long,
    val serii: Int,
    val treningovTentoMesiac: Int,
)

class ProgresModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    val dnes: Long = repo.dnes()

    /** Prvý deň kalendára aktivity - pondelok pred 15 týždňami. */
    val zaciatokKalendara: Long = repo.zaciatokTyzdna(dnes) - (TYZDNOV_KALENDARA - 1) * 7L

    /** null = ešte sa načítava (aby neprebleskol prázdny stav). */
    val statistiky: StateFlow<ProgresStatistiky?> = combine(
        repo.suhrnTreningovTok(0),
        repo.suhrnTreningovTok(LocalDate.ofEpochDay(dnes).withDayOfMonth(1).toEpochDay()),
        repo.pocetSeriiTok(),
    ) { spolu, mesiac, serie ->
        ProgresStatistiky(
            treningov = spolu.pocet,
            trvanieMs = spolu.trvanieMs,
            serii = serie,
            treningovTentoMesiac = mesiac.pocet,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dniTreningov: StateFlow<Set<Long>> = repo.dniTreningovTok(zaciatokKalendara)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _zrucnosti = MutableStateFlow<List<ProgresZrucnost>>(emptyList())
    val zrucnosti = _zrucnosti.asStateFlow()

    private val _objem = MutableStateFlow<List<ObjemRiadok>>(emptyList())
    val objem = _objem.asStateFlow()

    private val _grafy = MutableStateFlow<List<GrafRebrika>>(emptyList())
    val grafy = _grafy.asStateFlow()

    private val _miery = MutableStateFlow<Map<String, List<MieraEntity>>>(emptyMap())
    val miery = _miery.asStateFlow()

    private val _rekordy = MutableStateFlow<List<Rekord>?>(null)
    val rekordy = _rekordy.asStateFlow()

    private val _retest = MutableStateFlow<String?>(null)
    val retest = _retest.asStateFlow()

    private val _nacitane = MutableStateFlow(false)
    val nacitane = _nacitane.asStateFlow()

    init {
        obnov()
    }

    fun obnov() {
        viewModelScope.launch {
            val urovne = repo.denik.urovne().associate { it.rebrikId to it.uroven }
            val maxy = repo.katalog.maxUrovne().associate { it.rebrikId to it.maxUroven }
            val aktivne = repo.denik.aktivneCiele()
            val cieleZrucnosti = aktivne.filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }

            _zrucnosti.value = cieleZrucnosti.map { c ->
                val stav = Pokrok.vyhodnotZrucnost(c.kluc, c.nazov, urovne, maxy)
                val cviky = repo.cvikyRebrika(c.kluc).filter { !it.jeDetsky }
                val stupen = (urovne[c.kluc] ?: 1).coerceAtLeast(1)
                ProgresZrucnost(
                    stav = stav,
                    stupen = stupen,
                    maxStupen = maxy[c.kluc] ?: cviky.maxOfOrNull { it.uroven } ?: stupen,
                    nazovStupna = cviky.firstOrNull { it.uroven == stupen }?.nazov,
                    dalsiStupen = cviky.firstOrNull { it.uroven == stupen + 1 }?.nazov,
                )
            }

            val prioritne = aktivne.filter { DruhCiela.z(it.druh) == DruhCiela.PARTIA }
                .mapNotNull { Partia.z(it.kluc) }.toSet()
            val dnesDen = repo.dnes()
            val tyzdenObjem = repo.denik.objemPodlaPartii(repo.zaciatokTyzdna(dnesDen), dnesDen)
                .mapNotNull { o -> Partia.z(o.partia)?.let { it to o.serie } }.toMap()
            val profil = repo.profil()
            val tyzdnov = if (profil.zaciatokEpochDen > 0) ((dnesDen - profil.zaciatokEpochDen) / 7).toInt() else 0
            _objem.value = Partia.entries
                .map { p ->
                    ObjemRiadok(
                        partia = p,
                        odcvicene = tyzdenObjem[p] ?: 0.0,
                        ciel = Objem.cielSerii(p, p in prioritne, tyzdnov),
                        prioritna = p in prioritne,
                    )
                }
                .filter { it.prioritna || it.odcvicene > 0 }
                .sortedByDescending { it.prioritna }

            // Grafy: rebríky cieľov + tie, kde je aspoň 2× zapísaná séria.
            val rebriky = repo.rebriky().associateBy { it.id }
            val kandidati = (
                cieleZrucnosti.map { it.kluc } + listOf("kliky", "pritahy", "drep")
                ).distinct()
            _grafy.value = kandidati.mapNotNull { id ->
                val body = repo.denik.grafRebrika(id)
                if (body.size < 2) return@mapNotNull null
                val r = rebriky[id] ?: return@mapNotNull null
                val meranie = repo.cvikyRebrika(id).firstOrNull()?.meranie ?: "opakovania"
                GrafRebrika(id, r.nazov, body, meranie)
            }

            _miery.value = repo.denik.vsetkyMiery().groupBy { it.typ }
            _rekordy.value = repo.posledneRekordy(8)
            _retest.value = if (repo.jeCasNaRetest()) repo.upozornenieNaDennuDobu()
                ?: "Od posledného testu prešli 4 týždne — oplatí sa premerať."
            else null
            _nacitane.value = true
        }
    }

    fun pridajMieru(typ: String, hodnota: Double) {
        viewModelScope.launch {
            repo.denik.vlozMieru(MieraEntity(epochDen = repo.dnes(), typ = typ, hodnota = hodnota))
            obnov()
        }
    }
}

@Composable
fun ProgresObrazovka(naTest: () -> Unit, model: ProgresModel = viewModel()) {
    val statistiky by model.statistiky.collectAsStateWithLifecycle()
    val dni by model.dniTreningov.collectAsStateWithLifecycle()
    val zrucnosti by model.zrucnosti.collectAsStateWithLifecycle()
    val objem by model.objem.collectAsStateWithLifecycle()
    val grafy by model.grafy.collectAsStateWithLifecycle()
    val miery by model.miery.collectAsStateWithLifecycle()
    val rekordy by model.rekordy.collectAsStateWithLifecycle()
    val retest by model.retest.collectAsStateWithLifecycle()
    val nacitane by model.nacitane.collectAsStateWithLifecycle()

    var dialogMiera by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { model.obnov() }

    val stat = statistiky
    val maTreningy = stat != null && stat.treningov > 0
    val prazdne = nacitane && stat != null && !maTreningy &&
        zrucnosti.isEmpty() && objem.isEmpty() && grafy.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "titulok") {
            Text("Pokrok", style = MaterialTheme.typography.headlineLarge)
        }

        retest?.let { text ->
            item(key = "retest") { KartaRetestu(text, naTest, Modifier.animateItem()) }
        }

        if (prazdne) {
            item(key = "prazdne") {
                PrazdnyStav(
                    titulok = "Zatiaľ tu nie je čo ukázať",
                    text = "Pokrok sa objaví po prvých tréningoch. Ciele si nastavíš v Nastaveniach.",
                    rebrikPostavicky = "kliky",
                )
            }
        }

        // ---------------- súhrn a kalendár ----------------
        if (stat != null && maTreningy) {
            item(key = "statistiky") { MriezkaStatistik(stat, Modifier.animateItem()) }
            item(key = "kalendar") {
                KartaKalendara(dni, model.zaciatokKalendara, model.dnes, Modifier.animateItem())
            }
        }

        // ---------------- ciele so zručnosťami ----------------
        if (zrucnosti.isNotEmpty()) {
            item(key = "nadpis-ciele") { NadpisSekcie("Ciele") }
            items(zrucnosti, key = { "ciel-${it.stav.kluc}" }) { z ->
                KartaZrucnosti(z, Modifier.animateItem())
            }
        }

        // ---------------- osobné rekordy ----------------
        val zoznamRekordov = rekordy
        if (maTreningy && zoznamRekordov != null) {
            item(key = "nadpis-rekordy") { NadpisSekcie("Osobné rekordy") }
            item(key = "rekordy") { KartaRekordov(zoznamRekordov, Modifier.animateItem()) }
        }

        // ---------------- grafy sily ----------------
        if (grafy.isNotEmpty()) {
            item(key = "nadpis-grafy") {
                Column {
                    NadpisSekcie("Sila v čase")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Najlepšia séria dňa. Zmena o 1 opakovanie je bežné kolísanie, nie pokrok. " +
                            "Ťukni alebo prejdi prstom po grafe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(grafy, key = { "graf-${it.rebrikId}" }) { g ->
                KartaGrafu(g, Modifier.animateItem())
            }
        }

        // ---------------- týždenný objem ----------------
        if (objem.isNotEmpty()) {
            item(key = "nadpis-objem") {
                Column {
                    NadpisSekcie("Tento týždeň")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Série na partiu oproti týždennému cieľu. Hviezdička = tvoja prioritná partia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "objem") { KartaObjemu(objem, Modifier.animateItem()) }
        }

        // ---------------- miery ----------------
        item(key = "nadpis-miery") {
            Column {
                NadpisSekcie("Miery")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pri lýtkach a predlaktiach je obvod jediné poctivé meradlo. Raz mesačne stačí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "miery") { KartaMier(miery, naZapis = { dialogMiera = true }) }
    }

    if (dialogMiera) {
        DialogMiery(
            zavri = { dialogMiera = false },
            uloz = { typ, hodnota -> model.pridajMieru(typ, hodnota); dialogMiera = false },
        )
    }
}

// ------------------------------------------------------------------ pomocné

/**
 * false pri prvom zobrazení, hneď nato true - ukazovatele sa pri vstupe na obrazovku naplnia z nuly.
 * Uložené cez rememberSaveable, aby sa animácia neopakovala pri každom odrolovaní v zozname.
 */
@Composable
private fun rememberOdhalenie(): Boolean {
    var odhalene by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { odhalene = true }
    return odhalene
}

private val NAZVY_DNI = listOf("pondelok", "utorok", "streda", "štvrtok", "piatok", "sobota", "nedeľa")
private val SKRATKY_MESIACOV = listOf("jan", "feb", "mar", "apr", "máj", "jún", "júl", "aug", "sep", "okt", "nov", "dec")

/** "3. 10." (v inom roku "3. 10. 2025"), voliteľne s názvom dňa "streda 3. 10.". */
private fun pokrokDatum(epochDen: Long, sDnom: Boolean = false): String {
    val d = LocalDate.ofEpochDay(epochDen)
    val zaklad = if (d.year == LocalDate.now().year) "${d.dayOfMonth}. ${d.monthValue}."
    else "${d.dayOfMonth}. ${d.monthValue}. ${d.year}"
    return if (sDnom) "${NAZVY_DNI[d.dayOfWeek.value - 1]} $zaklad" else zaklad
}

// ------------------------------------------------------------------ retest

@Composable
private fun KartaRetestu(text: String, naTest: () -> Unit, modifier: Modifier = Modifier) {
    val farba = MaterialTheme.colorScheme.secondary
    TonovanaKarta(modifier, farba = farba) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Straighten, contentDescription = null, tint = farba, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Čas na retest", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        VelkeTlacidlo(
            text = "Premerať sa",
            onClick = naTest,
            farba = farba,
            farbaTextu = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

// ------------------------------------------------------------------ štatistiky

@Composable
private fun MriezkaStatistik(s: ProgresStatistiky, modifier: Modifier = Modifier) {
    val odhalene = rememberOdhalenie()
    val spec = tween<Int>(durationMillis = 900, easing = FastOutSlowInEasing)
    val treningov by animateIntAsState(if (odhalene) s.treningov else 0, spec, label = "treningov")
    val minuty by animateIntAsState(if (odhalene) (s.trvanieMs / 60_000L).toInt() else 0, spec, label = "minuty")
    val serii by animateIntAsState(if (odhalene) s.serii else 0, spec, label = "serii")
    val mesiac by animateIntAsState(if (odhalene) s.treningovTentoMesiac else 0, spec, label = "mesiac")

    // Veľké číslo musí ostať krátke (dlaždica je polovica šírky) - celé hodiny,
    // presný čas cez formatujTrvanie ide do popisu.
    val cas = if (minuty < 60) formatujTrvanie(minuty * 60_000L) else "${minuty / 60} h"
    val popisCasu = if (s.trvanieMs >= 3_600_000L) "čas spolu · ${formatujTrvanie(s.trvanieMs)}" else "čas spolu"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatDlazdica(
                hodnota = "$treningov",
                popis = sklonuj(treningov, "tréning spolu", "tréningy spolu", "tréningov spolu"),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                farba = MaterialTheme.colorScheme.primary,
                ikona = Icons.Filled.FitnessCenter,
            )
            StatDlazdica(
                hodnota = cas,
                popis = popisCasu,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                farba = MaterialTheme.colorScheme.secondary,
                ikona = Icons.Filled.Timer,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatDlazdica(
                hodnota = "$serii",
                popis = sklonuj(serii, "séria spolu", "série spolu", "sérií spolu"),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                ikona = Icons.Filled.Repeat,
            )
            StatDlazdica(
                hodnota = "$mesiac",
                popis = sklonuj(mesiac, "tréning tento mesiac", "tréningy tento mesiac", "tréningov tento mesiac"),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                farba = MaterialTheme.colorScheme.primary,
                ikona = Icons.Filled.CalendarMonth,
            )
        }
    }
}

// ------------------------------------------------------------------ kalendár aktivity

@Composable
private fun KartaKalendara(dni: Set<Long>, zaciatok: Long, dnes: Long, modifier: Modifier = Modifier) {
    val pocet = dni.count { it in zaciatok..dnes }
    val plna = MaterialTheme.colorScheme.primary
    TonovanaKarta(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Aktivita", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "posledných $TYZDNOV_KALENDARA týždňov",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        KalendarAktivity(dni, zaciatok, dnes)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$pocet ${sklonuj(pocet, "deň", "dni", "dní")} s tréningom",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(plna))
            Spacer(Modifier.width(6.dp))
            Text("tréning", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(12.dp).border(1.5.dp, plna, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
            Text("dnes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Mriežka 7 riadkov (Po–Ne) × 16 stĺpcov (týždne). Odcvičený deň je plný, dnes má obrys.
 * Zámerne žiadne počítadlo série dní - je to len zrkadlo, nie trest za pauzu.
 */
@Composable
private fun KalendarAktivity(dni: Set<Long>, zaciatok: Long, dnes: Long) {
    val plna = MaterialTheme.colorScheme.primary
    val prazdna = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val obrysOdcviceneho = MaterialTheme.colorScheme.onSurface
    val tlmena = MaterialTheme.colorScheme.onSurfaceVariant
    val odhalene = rememberOdhalenie()
    val vlna by animateFloatAsState(
        targetValue = if (odhalene) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "kalendar",
    )

    // Popisok mesiaca patrí stĺpcu, v ktorom mesiac začína (týždeň s jeho 1. dňom).
    val mesiace = remember(zaciatok) {
        val zoznam = mutableListOf<Pair<Int, String>>()
        var predosly = -1
        for (w in 0 until TYZDNOV_KALENDARA) {
            val nedela = LocalDate.ofEpochDay(zaciatok + w * 7L + 6)
            if (nedela.monthValue != predosly) {
                zoznam += w to SKRATKY_MESIACOV[nedela.monthValue - 1]
                predosly = nedela.monthValue
            }
        }
        // Prvý stĺpec je zvyčajne chvost mesiaca - jeho popisok by sa prekrýval s nasledujúcim.
        if (zoznam.size > 1 && zoznam[1].first <= 2) zoznam.removeAt(0)
        zoznam.filter { it.first <= TYZDNOV_KALENDARA - 2 }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sirkaPopisu = 26.dp
        val medzera = 3.dp
        val bunka = minOf(
            (maxWidth - sirkaPopisu - medzera * (TYZDNOV_KALENDARA - 1)) / TYZDNOV_KALENDARA,
            24.dp,
        )
        val sirkaMriezky = bunka * TYZDNOV_KALENDARA + medzera * (TYZDNOV_KALENDARA - 1)
        val vyskaMriezky = bunka * 7 + medzera * 6

        Column {
            Box(Modifier.fillMaxWidth().height(18.dp)) {
                mesiace.forEach { (stlpec, nazov) ->
                    Text(
                        nazov,
                        style = MaterialTheme.typography.labelSmall,
                        color = tlmena,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(x = sirkaPopisu + (bunka + medzera) * stlpec),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Column(Modifier.width(sirkaPopisu), verticalArrangement = Arrangement.spacedBy(medzera)) {
                    for (r in 0 until 7) {
                        Box(Modifier.height(bunka), contentAlignment = Alignment.CenterStart) {
                            if (r == 0 || r == 2 || r == 4) {
                                Text(SKRATKY_DNI[r], style = MaterialTheme.typography.labelSmall, color = tlmena, maxLines = 1)
                            }
                        }
                    }
                }
                Canvas(Modifier.width(sirkaMriezky).height(vyskaMriezky)) {
                    val c = bunka.toPx()
                    val g = medzera.toPx()
                    val zaoblenie = CornerRadius(c * 0.3f)
                    val hrubka = 1.5.dp.toPx()
                    // Vlna zľava doprava: história sa "dopíše" až po dnešok.
                    val postup = vlna * (TYZDNOV_KALENDARA + 3)
                    for (w in 0 until TYZDNOV_KALENDARA) {
                        val alfa = ((postup - w) / 3f).coerceIn(0f, 1f)
                        if (alfa <= 0f) continue
                        for (d in 0 until 7) {
                            val den = zaciatok + w * 7L + d
                            if (den > dnes) continue
                            val poloha = Offset(w * (c + g), d * (c + g))
                            val odcviceny = den in dni
                            drawRoundRect(
                                color = if (odcviceny) plna else prazdna,
                                topLeft = poloha,
                                size = Size(c, c),
                                cornerRadius = zaoblenie,
                                alpha = alfa,
                            )
                            if (den == dnes) {
                                drawRoundRect(
                                    color = if (odcviceny) obrysOdcviceneho else plna,
                                    topLeft = poloha + Offset(hrubka / 2f, hrubka / 2f),
                                    size = Size(c - hrubka, c - hrubka),
                                    cornerRadius = zaoblenie,
                                    style = Stroke(hrubka),
                                    alpha = alfa,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ ciele zručností

@Composable
private fun KartaZrucnosti(z: ProgresZrucnost, modifier: Modifier = Modifier) {
    val c = z.stav
    val akcent = MaterialTheme.colorScheme.primary
    val splnenaFarba = MaterialTheme.colorScheme.secondary
    val odhalene = rememberOdhalenie()
    val percent = (c.pokrok * 100).toInt()

    TonovanaKarta(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KruhPostupu(
                postup = if (odhalene) c.pokrok.toFloat() else 0f,
                modifier = Modifier.size(96.dp),
                farba = akcent,
                hrubka = 10.dp,
                plynulo = false,
            ) {
                Text("$percent %", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(c.nazov, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Stupeň ${z.stupen} z ${z.maxStupen}",
                    style = MaterialTheme.typography.labelLarge,
                    color = akcent,
                )
                z.nazovStupna?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val dalsi = when {
                    z.dalsiStupen != null -> "Ďalší stupeň: ${z.dalsiStupen}"
                    z.stupen >= z.maxStupen -> "Vrchol rebríka - ďalší stupeň už nie je."
                    else -> null
                }
                dalsi?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Brány (prerekvizity). Pri cieli s jedinou bránou by len opakovali kruh.
        if (c.brany.size > 1) {
            Spacer(Modifier.height(18.dp))
            c.brany.forEachIndexed { i, b ->
                val brzda = b == c.brzda
                val splnena = b.pokrok >= 1.0
                if (i > 0) Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        b.brana.nazov,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (brzda) {
                        Kapsula("brzda", akcent)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (splnena) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Splnené",
                            tint = splnenaFarba,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "${b.aktualnaUroven}/${b.potrebnaUroven}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (brzda) akcent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                PasPlnenia(
                    postup = if (odhalene) b.pokrok.toFloat() else 0f,
                    farba = if (brzda) akcent else splnenaFarba,
                    vyska = 8.dp,
                )
            }
        }

        Pokrok.vetaBrzdy(c)?.let { veta ->
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp).size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(veta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------------ osobné rekordy

@Composable
private fun KartaRekordov(rekordy: List<Rekord>, modifier: Modifier = Modifier) {
    if (rekordy.isEmpty()) {
        TonovanaKarta(modifier, vnutorneOdsadenie = 0.dp) {
            PrazdnyStav(
                titulok = "Rekordy ešte prídu",
                text = "Keď na niektorom cviku prekonáš svoje doterajšie maximum, objaví sa tu.",
                rebrikPostavicky = null,
            )
        }
        return
    }
    TonovanaKarta(modifier, vnutorneOdsadenie = 16.dp) {
        rekordy.forEachIndexed { i, r ->
            if (i > 0) {
                HorizontalDivider(
                    Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
            RiadokRekordu(r)
        }
    }
}

@Composable
private fun RiadokRekordu(r: Rekord) {
    val farba = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(farba.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = farba, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                r.nazov,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "predtým ${formatujHodnotu(r.predtym, r.meranie)} · ${pokrokDatum(r.epochDen)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatujHodnotu(r.hodnota, r.meranie), style = MaterialTheme.typography.titleLarge, color = farba)
            Text(
                "+" + formatujHodnotu(r.hodnota - r.predtym, r.meranie),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

// ------------------------------------------------------------------ grafy sily

@Composable
private fun KartaGrafu(g: GrafRebrika, modifier: Modifier = Modifier) {
    var vybrany by remember(g.rebrikId, g.body.size) { mutableStateOf<Int?>(null) }
    val body = g.body
    val bod = vybrany?.let { body.getOrNull(it) }
    val zobrazeny = bod ?: body.last()
    val rozdiel = zobrazeny.hodnota - body.first().hodnota

    TonovanaKarta(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(g.nazov, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (bod != null) pokrokDatum(bod.den, sDnom = true) else "naposledy ${pokrokDatum(zobrazeny.den)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatujHodnotu(zobrazeny.hodnota, g.meranie),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (zobrazeny !== body.first()) {
                    Text(
                        (if (rozdiel >= 0) "+" else "−") + formatujHodnotu(abs(rozdiel), g.meranie) + " od začiatku",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rozdiel > 0) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GrafSily(body, g.meranie, vybrany, naVyber = { vybrany = it })
    }
}

/** Index bodu, ktorého x je najbližšie k [x] (body sú rozložené podľa dní, nie rovnomerne). */
private fun najblizsiBod(body: List<BodGrafu>, x: Float, sirka: Float, okraj: Float): Int {
    val minD = body.first().den
    val rozsah = maxOf(body.last().den - minD, 1L).toFloat()
    val vnutro = sirka - 2 * okraj
    return body.indices.minBy { i -> abs(okraj + (body[i].den - minD) / rozsah * vnutro - x) }
}

/** Plynulá krivka cez body: kubické segmenty s vodorovnými dotyčnicami - neprestreľuje nad skutočné hodnoty. */
private fun Path.pridajKrivku(body: List<Offset>) {
    for (i in 1 until body.size) {
        val a = body[i - 1]
        val b = body[i]
        val stred = (a.x + b.x) / 2f
        cubicTo(stred, a.y, stred, b.y, b.x, b.y)
    }
}

/**
 * Graf sily bez knižnice: krivka s gradientom pod ňou, os X s dátumami, os Y s krajnými hodnotami.
 * Ťuknutie vyberie bod (opätovné ťuknutie výber zruší), ťahanie prstom prechádza po bodoch.
 */
@Composable
private fun GrafSily(body: List<BodGrafu>, meranie: String, vybrany: Int?, naVyber: (Int?) -> Unit) {
    val farba = MaterialTheme.colorScheme.primary
    val mriezka = MaterialTheme.colorScheme.outlineVariant
    val ukazovatel = MaterialTheme.colorScheme.onSurfaceVariant
    val pozadieBodu = MaterialTheme.colorScheme.surfaceContainerLow
    val tlmena = MaterialTheme.colorScheme.onSurfaceVariant
    val haptika = rememberHaptika()
    val aktualnyVyber by rememberUpdatedState(vybrany)
    val vyber by rememberUpdatedState(naVyber)

    val odhalene = rememberOdhalenie()
    val kreslenie by animateFloatAsState(
        targetValue = if (odhalene) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "graf",
    )

    val minD = body.first().den
    val maxD = maxOf(body.last().den, minD + 1)
    val minH = body.minOf { it.hodnota }
    val maxH = body.maxOf { it.hodnota }
    val dolna = if (minH == maxH) (minH - 1).coerceAtLeast(0) else minH
    val horna = if (minH == maxH) maxH + 1 else maxH
    val vyskaGrafu = 150.dp
    val okraj = 8.dp

    Row {
        Column(Modifier.weight(1f)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(vyskaGrafu)
                    .pointerInput(body) {
                        detectTapGestures { poloha ->
                            val i = najblizsiBod(body, poloha.x, size.width.toFloat(), okraj.toPx())
                            haptika.tik()
                            vyber(if (i == aktualnyVyber) null else i)
                        }
                    }
                    .pointerInput(body) {
                        fun zvol(x: Float) {
                            val i = najblizsiBod(body, x, size.width.toFloat(), okraj.toPx())
                            if (i != aktualnyVyber) {
                                haptika.tik()
                                vyber(i)
                            }
                        }
                        detectHorizontalDragGestures(
                            onDragStart = { poloha -> zvol(poloha.x) },
                            onHorizontalDrag = { zmena, _ ->
                                zmena.consume()
                                zvol(zmena.position.x)
                            },
                        )
                    }
            ) {
                val ok = okraj.toPx()
                val sirka = size.width - 2 * ok
                val vyska = size.height - 2 * ok
                fun x(den: Long) = ok + (den - minD).toFloat() / (maxD - minD).toFloat() * sirka
                fun y(h: Int) = ok + (1f - (h - dolna).toFloat() / (horna - dolna).toFloat()) * vyska

                val hrubkaMriezky = 1.dp.toPx()
                val prerusovana = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                drawLine(mriezka, Offset(0f, ok), Offset(size.width, ok), hrubkaMriezky, pathEffect = prerusovana)
                drawLine(
                    mriezka, Offset(0f, ok + vyska / 2f), Offset(size.width, ok + vyska / 2f),
                    hrubkaMriezky, pathEffect = prerusovana,
                )
                drawLine(mriezka, Offset(0f, ok + vyska), Offset(size.width, ok + vyska), hrubkaMriezky)

                val body2 = body.map { Offset(x(it.den), y(it.hodnota)) }
                val ciara = Path().apply {
                    moveTo(body2.first().x, body2.first().y)
                    pridajKrivku(body2)
                }
                val vypln = Path().apply {
                    moveTo(body2.first().x, ok + vyska)
                    lineTo(body2.first().x, body2.first().y)
                    pridajKrivku(body2)
                    lineTo(body2.last().x, ok + vyska)
                    close()
                }

                // Krivka sa pri vstupe "dokreslí" zľava doprava.
                clipRect(right = ok + sirka * kreslenie + ok) {
                    drawPath(
                        vypln,
                        Brush.verticalGradient(
                            listOf(farba.copy(alpha = 0.32f), farba.copy(alpha = 0f)),
                            startY = ok,
                            endY = ok + vyska,
                        ),
                    )
                    drawPath(
                        ciara, farba,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    if (body2.size <= 24) {
                        body2.forEach { p ->
                            drawCircle(pozadieBodu, 5.dp.toPx(), p)
                            drawCircle(farba, 3.dp.toPx(), p)
                        }
                    }
                }

                val i = vybrany
                val zvyrazneny = if (i != null && i in body2.indices) body2[i] else null
                if (zvyrazneny != null) {
                    drawLine(
                        ukazovatel.copy(alpha = 0.6f),
                        Offset(zvyrazneny.x, 0f), Offset(zvyrazneny.x, size.height),
                        1.5.dp.toPx(), pathEffect = prerusovana,
                    )
                    drawCircle(farba.copy(alpha = 0.22f), 12.dp.toPx(), zvyrazneny)
                    drawCircle(pozadieBodu, 6.dp.toPx(), zvyrazneny)
                    drawCircle(farba, 4.5.dp.toPx(), zvyrazneny)
                } else if (kreslenie >= 1f) {
                    val posledny = body2.last()
                    drawCircle(farba.copy(alpha = 0.22f), 9.dp.toPx(), posledny)
                    drawCircle(farba, 4.5.dp.toPx(), posledny)
                }
            }
            Spacer(Modifier.height(6.dp))
            // Os X: prvý deň, stred obdobia, posledný deň.
            Box(Modifier.fillMaxWidth()) {
                Text(
                    pokrokDatum(minD),
                    style = MaterialTheme.typography.labelSmall,
                    color = tlmena,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                if (maxD - minD >= 6) {
                    Text(
                        pokrokDatum((minD + maxD) / 2),
                        style = MaterialTheme.typography.labelSmall,
                        color = tlmena,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Text(
                    pokrokDatum(body.last().den),
                    style = MaterialTheme.typography.labelSmall,
                    color = tlmena,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // Os Y: horná a dolná hodnota, zarovnané s krajnými čiarami mriežky.
        Box(Modifier.height(vyskaGrafu)) {
            Text(
                formatujHodnotu(horna, meranie),
                style = MaterialTheme.typography.labelSmall,
                color = tlmena,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Text(
                formatujHodnotu(dolna, meranie),
                style = MaterialTheme.typography.labelSmall,
                color = tlmena,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

// ------------------------------------------------------------------ týždenný objem

@Composable
private fun KartaObjemu(objem: List<ObjemRiadok>, modifier: Modifier = Modifier) {
    val odhalene = rememberOdhalenie()
    val prioritnaFarba = MaterialTheme.colorScheme.primary
    val beznaFarba = FarbyBlokov.pre(Blok.SILA)
    TonovanaKarta(modifier) {
        objem.forEachIndexed { i, r ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            val postup = if (r.ciel > 0) (r.odcvicene / r.ciel).toFloat().coerceIn(0f, 1f) else 1f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.partia.nazov, style = MaterialTheme.typography.bodyLarge)
                if (r.prioritna) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Prioritná partia",
                        tint = prioritnaFarba,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${"%.1f".format(r.odcvicene).removeSuffix(".0").removeSuffix(",0")} / ${r.ciel.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (postup >= 1f) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            PasPlnenia(
                postup = if (odhalene) postup else 0f,
                farba = if (r.prioritna) prioritnaFarba else beznaFarba,
                vyska = 8.dp,
            )
        }
    }
}

// ------------------------------------------------------------------ miery

@Composable
private fun KartaMier(miery: Map<String, List<MieraEntity>>, naZapis: () -> Unit) {
    TonovanaKarta {
        if (miery.isEmpty()) {
            Text(
                "Zatiaľ žiadne.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            miery.entries.forEachIndexed { i, (typ, zoznam) ->
                if (i > 0) {
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                }
                val posledna = zoznam.last()
                val delta = posledna.hodnota - zoznam.first().hodnota
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nazovMiery(typ), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            pokrokDatum(posledna.epochDen),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (zoznam.size > 1) {
                        Kapsula(
                            "%+.1f".format(delta).replace(".", ","),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        "%.1f cm".format(posledna.hodnota).replace(".", ","),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = naZapis) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Zapísať mieru")
        }
    }
}

private fun nazovMiery(typ: String) = when (typ) {
    "lytko" -> "Lýtko"
    "predlaktie" -> "Predlaktie"
    "hrudnik" -> "Hrudník"
    "pas" -> "Pás"
    "rameno" -> "Rameno (biceps)"
    else -> typ
}

@Composable
private fun DialogMiery(zavri: () -> Unit, uloz: (String, Double) -> Unit) {
    var typ by remember { mutableStateOf("lytko") }
    var hodnota by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = zavri,
        title = { Text("Nová miera") },
        text = {
            Column {
                listOf("lytko", "predlaktie", "rameno", "hrudnik", "pas").forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { typ = t }
                            .padding(vertical = 2.dp),
                    ) {
                        RadioButton(selected = typ == t, onClick = { typ = t })
                        Text(nazovMiery(t))
                    }
                }
                OutlinedTextField(
                    value = hodnota,
                    onValueChange = { hodnota = it.filter { z -> z.isDigit() || z == '.' || z == ',' } },
                    label = { Text("Obvod (cm)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                hodnota.replace(",", ".").toDoubleOrNull()?.let { uloz(typ, it) }
            }) { Text("Uložiť") }
        },
        dismissButton = { TextButton(onClick = zavri) { Text("Zrušiť") } },
    )
}
