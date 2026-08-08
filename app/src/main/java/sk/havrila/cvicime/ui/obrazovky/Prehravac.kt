package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.theme.StylVelkeCislo

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

    private val _detsky = MutableStateFlow<DetskyStav?>(null)
    val detsky = _detsky.asStateFlow()

    private val _postupy = MutableStateFlow<List<String>>(emptyList())
    val postupy = _postupy.asStateFlow()

    val hlasZapnuty get() = repo.prefs.hlasZapnuty
    val vibracieZapnute get() = repo.prefs.vibracieZapnute
    val obrazovkaStaleZapnuta get() = repo.prefs.obrazovkaStaleZapnuta

    private var treningId: Long = -1
    private var poradieSerie = 0
    private var detskeCviky: List<CvikEntity> = emptyList()

    fun spusti(minuty: Int, dietaId: Long) {
        if (treningId > 0) return
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
        _koniecOdpoctu.value = if (Meranie.z(krok.polozka.cvik.meranie) == Meranie.VYDRZ) {
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
        viewModelScope.launch {
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

    fun skrat() {
        val zvysok = _kroky.value.drop(_index.value)
        val skratene = zvysok.groupBy { it.polozka.cvik.id }.values.mapNotNull { it.firstOrNull() }
        _kroky.value = _kroky.value.take(_index.value) + skratene
        pripravKrok(_index.value)
    }

    fun ukonci() {
        val id = treningId
        _stav.value = StavPrehravaca.HOTOVO
        if (id <= 0) return
        val d = _detsky.value
        viewModelScope.launch {
            repo.ukonciTrening(id, pocitPo = null)
            if (d != null) {
                repo.denik.ulozUcast(
                    UcastEntity(treningId = id, dietaId = d.dietaId, splneneKroky = d.labky)
                )
            }
            _postupy.value = repo.vyhodnotPostupy(id).mapNotNull { (rebrikId, uroven) ->
                val r = repo.katalog.rebrik(rebrikId) ?: return@mapNotNull null
                val cvik = repo.katalog.cvik(rebrikId, uroven)
                "${r.nazov}: postupuješ na stupeň $uroven" + (cvik?.let { " — ${it.nazov}" } ?: "")
            }
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
    val detsky by model.detsky.collectAsStateWithLifecycle()
    val postupy by model.postupy.collectAsStateWithLifecycle()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (kroky.isNotEmpty() && stav != StavPrehravaca.HOTOVO) {
            LinearProgressIndicator(
                progress = { (index.toFloat() / kroky.size).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${krok?.blok?.nazov ?: ""} · séria ${krok?.cisloSerie ?: 0}/${krok?.zoSerii ?: 0}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (stav) {
                StavPrehravaca.NACITAVA -> CircularProgressIndicator()

                StavPrehravaca.PRAZDNY -> Text(
                    "Zatiaľ ti neviem poskladať tréning. Skontroluj ciele a náradie v nastaveniach.",
                    textAlign = TextAlign.Center,
                )

                StavPrehravaca.CVICENIE -> krok?.let {
                    Cvicenie(it, koniec, hlas, model.hlasZapnuty, model.vibracieZapnute, model::hotovoCvicenie)
                }

                StavPrehravaca.OTAZKA -> krok?.let { OtazkaRezervy(it, model::odpovedRir) }

                StavPrehravaca.PAUZA -> Pauza(
                    koniec = koniec,
                    dalsi = krok,
                    detsky = detsky,
                    model = model,
                    hlas = hlas,
                    hotovoPauza = model::pauzaSkoncila,
                )

                StavPrehravaca.HOTOVO -> Zaver(detsky, postupy, hotovo)
            }
        }

        if (stav == StavPrehravaca.CVICENIE || stav == StavPrehravaca.PAUZA) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = model::preskoc) { Text("Preskočiť") }
                OutlinedButton(onClick = model::skrat) { Text("Nevládzem") }
                TextButton(onClick = { model.ukonci() }) { Text("Ukončiť") }
            }
        }
    }
}

@Composable
private fun Cvicenie(
    krok: KrokTreningu,
    koniec: Long,
    hlas: Hlas,
    hlasZapnuty: Boolean,
    vibracie: Boolean,
    hotovo: () -> Unit,
) {
    val naCas = Meranie.z(krok.polozka.cvik.meranie) == Meranie.VYDRZ
    var zostava by remember(koniec) { mutableIntStateOf(krok.polozka.ciel) }

    if (naCas && koniec > 0) {
        LaunchedEffect(koniec) {
            var poslednePipnutie = -1
            while (true) {
                val ms = koniec - SystemClock.elapsedRealtime()
                val s = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
                zostava = s
                if (s in 1..3 && s != poslednePipnutie) {
                    poslednePipnutie = s
                    hlas.pipni(hlasZapnuty)
                }
                if (ms <= 0) break
                delay(150)
            }
            hlas.pipniKoniec(hlasZapnuty)
            hlas.zavibruj(vibracie)
            hotovo()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Postavicka(
            rebrikId = krok.polozka.rebrikId,
            modifier = Modifier.size(120.dp),
            farba = MaterialTheme.colorScheme.onSurface,
            farbaAkcent = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            krok.polozka.cvik.nazov,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (naCas) "$zostava" else "${krok.polozka.ciel}",
            style = StylVelkeCislo,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            if (naCas) "sekúnd" else "opakovaní",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))
        krok.polozka.cvik.cue.split("|").filter { it.isNotBlank() }.take(3).forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        if (!naCas) {
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    hlas.zavibruj(true)
                    hotovo()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                Text("Hotovo", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun OtazkaRezervy(krok: KrokTreningu, odpoved: (Rir, Int?) -> Unit) {
    val naCas = Meranie.z(krok.polozka.cvik.meranie) == Meranie.VYDRZ
    var hodnota by remember(krok.polozka.cvik.id, krok.cisloSerie) {
        mutableIntStateOf(krok.polozka.ciel)
    }
    val krokZmeny = if (naCas) 5 else 1

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            krok.polozka.cvik.nazov,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (naCas) "Koľko sekúnd si vydržal?" else "Koľko si spravil?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { hodnota = (hodnota - krokZmeny).coerceAtLeast(0) },
                modifier = Modifier.size(64.dp),
            ) { Text("−", style = MaterialTheme.typography.headlineMedium) }
            Text(
                "$hodnota",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            OutlinedButton(
                onClick = { hodnota += krokZmeny },
                modifier = Modifier.size(64.dp),
            ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        }
        Text(
            "plán bol ${krok.polozka.ciel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Text("Koľko si mal ešte v sebe?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        listOf(Rir.NIC, Rir.JEDNO_DVE, Rir.TRI_A_VIAC).forEach { r ->
            Button(
                onClick = { odpoved(r, hodnota) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(vertical = 3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) { Text(r.popis, style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun Pauza(
    koniec: Long,
    dalsi: KrokTreningu?,
    detsky: DetskyStav?,
    model: PrehravacModel,
    hlas: Hlas,
    hotovoPauza: () -> Unit,
) {
    var zostava by remember(koniec) { mutableIntStateOf(0) }
    LaunchedEffect(koniec) {
        var poslednePipnutie = -1
        while (true) {
            val ms = koniec - SystemClock.elapsedRealtime()
            val s = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
            zostava = s
            if (s in 1..3 && s != poslednePipnutie) {
                poslednePipnutie = s
                hlas.pipni(model.hlasZapnuty)
            }
            if (ms <= 0) break
            delay(150)
        }
        hlas.pipniKoniec(model.hlasZapnuty)
        hlas.zavibruj(model.vibracieZapnute)
        hotovoPauza()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Pauza", style = MaterialTheme.typography.titleLarge)
        Text(
            "$zostava s",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        dalsi?.let {
            Text(
                "Ďalej: ${it.polozka.cvik.nazov}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = hotovoPauza) { Text("Preskočiť pauzu") }

        // Detský pruh: deti cvičia v tvojich pauzách. Ty oddychuješ, oni sa hrajú.
        if (detsky != null && detsky.krok != DetskyKrok.HOTOVO) {
            Spacer(Modifier.height(14.dp))
            DetskyPruh(detsky, model)
        }
    }
}

/**
 * Detský pruh rodinného režimu. Zámerné rozhodnutia (rešerš motivácie detí):
 * - dieťa si VYBERÁ z dvoch možností (voľba motivuje bez rizika),
 * - labky pribúdajú za pokus, žiadne časovky ani počty,
 * - ukazovateľ = 3 konkrétne labky (2-ročný nechápe percentá),
 * - čo sa odomkne, sa neukazuje vopred.
 */
@Composable
private fun DetskyPruh(d: DetskyStav, model: PrehravacModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.meno,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                // 3 labky - konkrétne obrázky, ktoré sa dopĺňajú
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i ->
                        Text(if (i < d.labky) "🐾" else "⚪", fontSize = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            when (d.krok) {
                DetskyKrok.VYBER -> {
                    Text("Čo ideme teraz?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        d.moznosti.forEach { cvik ->
                            val zviera = cvik.zviera?.let { Zvierata.podla(it) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { model.dietaVybralo(cvik) }
                                    .padding(10.dp),
                            ) {
                                Text(zviera?.emoji ?: "⭐", fontSize = 34.sp)
                                Text(
                                    zviera?.meno ?: cvik.nazov,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                DetskyKrok.CVICI -> {
                    val cvik = d.aktualny
                    val zviera = cvik?.zviera?.let { Zvierata.podla(it) }
                    Text(zviera?.emoji ?: "⭐", fontSize = 40.sp)
                    Text(cvik?.nazov ?: "", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    cvik?.cue?.split("|")?.firstOrNull()?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = model::dietaSpravilo) { Text("Spravené 🐾") }
                }

                DetskyKrok.ODHALENIE -> {
                    val z = d.odhalene
                    if (z != null) {
                        Text(z.emoji, fontSize = 56.sp)
                        Text(z.veta, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text(
                            "${z.meno} je v tvojej zbierke.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Krátko a späť k pohybu - odmena sa nesmie stať zábavou na obrazovke.
                        LaunchedEffect(z.kluc) {
                            delay(3500)
                            model.odhalenieVidene()
                        }
                    }
                }

                DetskyKrok.HOTOVO -> {}
            }
        }
    }
}

@Composable
private fun Zaver(detsky: DetskyStav?, postupy: List<String>, hotovo: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hotovo.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Zapísané. To je celé, čo dnes bolo treba.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        postupy.forEach {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            ) {
                Text(
                    "⬆ $it",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (detsky != null && detsky.labky > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${detsky.meno}: ${"🐾".repeat(detsky.labky)} — cvičili ste spolu.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = hotovo) { Text("Zavrieť") }
    }
}