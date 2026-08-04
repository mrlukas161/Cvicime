package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import android.os.SystemClock
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Meranie
import sk.havrila.cvicime.data.model.Rir
import sk.havrila.cvicime.domain.PlanPolozka
import sk.havrila.cvicime.trening.TreningService
import sk.havrila.cvicime.ui.theme.StylVelkeCislo

/** Jedna séria v poradí, ako sa reálne odcvičí. */
data class KrokTreningu(
    val blok: Blok,
    val polozka: PlanPolozka,
    val cisloSerie: Int,
    val zoSerii: Int,
)

enum class StavPrehravaca { NACITAVA, CVICENIE, OTAZKA, PAUZA, HOTOVO, PRAZDNY }

class PrehravacModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _kroky = MutableStateFlow<List<KrokTreningu>>(emptyList())
    val kroky = _kroky.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()

    private val _stav = MutableStateFlow(StavPrehravaca.NACITAVA)
    val stav = _stav.asStateFlow()

    /** Koniec aktuálneho odpočtu v elapsedRealtime; 0 = nebeží. */
    private val _koniecOdpoctu = MutableStateFlow(0L)
    val koniecOdpoctu = _koniecOdpoctu.asStateFlow()

    private var treningId: Long = -1
    private var poradieSerie = 0

    fun spusti(minuty: Int) {
        if (treningId > 0) return
        viewModelScope.launch {
            val plan = repo.zostavPlan(minuty)
            val zoznam = plan.bloky.flatMap { blok ->
                blok.polozky.flatMap { polozka ->
                    (1..polozka.serie).map { i ->
                        KrokTreningu(blok.blok, polozka, i, polozka.serie)
                    }
                }
            }
            _kroky.value = zoznam
            if (zoznam.isEmpty()) {
                _stav.value = StavPrehravaca.PRAZDNY
                return@launch
            }
            treningId = repo.zacniTrening(minuty, rodinny = false)
            pripravKrok(0)
        }
    }

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

    /** Cvik dokončený. Pri sériách, ktoré sa počítajú do objemu, sa pýtame na rezervu. */
    fun hotovoCvicenie() {
        val krok = _kroky.value.getOrNull(_index.value) ?: return
        _koniecOdpoctu.value = 0L
        _stav.value = if (krok.blok == Blok.SILA || krok.blok == Blok.KONDICIA) {
            StavPrehravaca.OTAZKA
        } else {
            zapis(krok, krok.polozka.ciel, null)
            dalej()
            return
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

    /**
     * Únik z tréningu, ktorý ho neopustí. Zoberie zvyšné kroky a nechá z každého cviku
     * už len jednu sériu - podľa literatúry o zmene správania je práve riešenie prekážky
     * najúčinnejšia zložka, nie pripomínanie.
     */
    fun skrat() {
        val zvysok = _kroky.value.drop(_index.value)
        val skratene = zvysok.groupBy { it.polozka.cvik.id }
            .values
            .mapNotNull { it.firstOrNull() }
        _kroky.value = _kroky.value.take(_index.value) + skratene
        pripravKrok(_index.value)
    }

    fun ukonci() {
        val id = treningId
        _stav.value = StavPrehravaca.HOTOVO
        if (id <= 0) return
        viewModelScope.launch { repo.ukonciTrening(id, pocitPo = null) }
        treningId = -1
    }
}

@Composable
fun PrehravacObrazovka(minuty: Int, hotovo: () -> Unit, model: PrehravacModel = viewModel()) {
    val kontext = LocalContext.current
    val stav by model.stav.collectAsStateWithLifecycle()
    val kroky by model.kroky.collectAsStateWithLifecycle()
    val index by model.index.collectAsStateWithLifecycle()
    val koniec by model.koniecOdpoctu.collectAsStateWithLifecycle()

    LaunchedEffect(minuty) { model.spusti(minuty) }

    DisposableEffect(Unit) {
        TreningService.spusti(kontext, "Prebieha tréning")
        onDispose { TreningService.zastav(kontext) }
    }

    val krok = kroky.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (kroky.isNotEmpty()) {
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

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (stav) {
                StavPrehravaca.NACITAVA -> CircularProgressIndicator()

                StavPrehravaca.PRAZDNY -> Text(
                    "Zatiaľ ti neviem poskladať tréning. Skontroluj ciele a náradie v nastaveniach.",
                    textAlign = TextAlign.Center,
                )

                StavPrehravaca.CVICENIE -> krok?.let {
                    Cvicenie(it, koniec, model::hotovoCvicenie)
                }

                StavPrehravaca.OTAZKA -> krok?.let {
                    OtazkaRezervy(it, model::odpovedRir)
                }

                StavPrehravaca.PAUZA -> Pauza(
                    koniec = koniec,
                    dalsi = krok,
                    hotovo = model::pauzaSkoncila,
                )

                StavPrehravaca.HOTOVO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hotovo.", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Zapísané. To je celé, čo dnes bolo treba.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = hotovo) { Text("Zavrieť") }
                }
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
private fun Cvicenie(krok: KrokTreningu, koniec: Long, hotovo: () -> Unit) {
    val naCas = Meranie.z(krok.polozka.cvik.meranie) == Meranie.VYDRZ
    var zostava by remember(koniec) { mutableIntStateOf(krok.polozka.ciel) }

    if (naCas && koniec > 0) {
        LaunchedEffect(koniec) {
            while (true) {
                val ms = koniec - SystemClock.elapsedRealtime()
                zostava = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
                if (ms <= 0) break
                delay(200)
            }
            hotovo()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            krok.polozka.cvik.nazov,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
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

        Spacer(Modifier.height(20.dp))
        krok.polozka.cvik.cue.split("|").filter { it.isNotBlank() }.take(3).forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        if (!naCas) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = hotovo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                Text("Hotovo", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/**
 * Rezerva sa pýta troma tlačidlami, nie posuvníkom.
 * Jemnejšia škála by u začiatočníka aj tak merala šum - a udýchaný človek
 * nemá trafiť posuvník. Odpoveď je len korekcia, hlavný signál sú opakovania.
 */
@Composable
private fun OtazkaRezervy(krok: KrokTreningu, odpoved: (Rir, Int?) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Koľko si mal ešte v sebe?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            krok.polozka.cvik.nazov,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        listOf(Rir.NIC, Rir.JEDNO_DVE, Rir.TRI_A_VIAC).forEach { r ->
            Button(
                onClick = { odpoved(r, krok.polozka.ciel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(r.popis, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun Pauza(koniec: Long, dalsi: KrokTreningu?, hotovo: () -> Unit) {
    var zostava by remember(koniec) { mutableIntStateOf(0) }
    LaunchedEffect(koniec) {
        while (true) {
            val ms = koniec - SystemClock.elapsedRealtime()
            zostava = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
            if (ms <= 0) break
            delay(200)
        }
        hotovo()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Pauza", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("$zostava", style = StylVelkeCislo, color = MaterialTheme.colorScheme.secondary)
        Text("sekúnd", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        dalsi?.let {
            Text(
                "Ďalej: ${it.polozka.cvik.nazov}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = hotovo) { Text("Preskočiť pauzu") }
        Spacer(Modifier.height(6.dp))
        Text(
            "Kratšia pauza znie tvrdšie, ale znamená menší prírastok.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
