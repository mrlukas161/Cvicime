package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
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
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia

/** Čo už má používateľ uložené - obrazovka sa tým predvyplní. */
data class UlozenyVyber(
    val zrucnosti: List<String>,
    val partie: List<Partia>,
    val naradie: List<Naradie>,
    val vahaKg: String,
    val vyskaCm: String,
)

class OnboardingModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    /** Sleduje katalóg tokom - pri prvom spustení sa ešte nemusí byť načítaný. */
    private val _zrucnosti = MutableStateFlow<List<RebrikEntity>>(emptyList())
    val zrucnosti = _zrucnosti.asStateFlow()

    private val _ulozene = MutableStateFlow<UlozenyVyber?>(null)
    val ulozene = _ulozene.asStateFlow()

    init {
        viewModelScope.launch {
            repo.rebrikyTok().collectLatest { rebriky ->
                _zrucnosti.value = rebriky.filter { it.jeZrucnost }
            }
        }
        viewModelScope.launch {
            val profil = repo.profil()
            val ciele = repo.denik.aktivneCiele()
            _ulozene.value = UlozenyVyber(
                zrucnosti = ciele.filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }.map { it.kluc },
                partie = ciele.filter { DruhCiela.z(it.druh) == DruhCiela.PARTIA }
                    .mapNotNull { Partia.z(it.kluc) },
                naradie = profil.naradie.split(",").mapNotNull { Naradie.z(it.trim()) }
                    .filter { it != Naradie.ZIADNE },
                vahaKg = profil.vahaKg.toString().removeSuffix(".0"),
                vyskaCm = profil.vyskaCm.toString(),
            )
        }
    }

    fun uloz(
        vybraneZrucnosti: List<String>,
        vybranePartie: List<Partia>,
        vybraneNaradie: List<Naradie>,
        vahaKg: Double,
        vyskaCm: Int,
        hotovo: () -> Unit,
    ) {
        viewModelScope.launch {
            val nazvy = repo.rebriky().associate { it.id to it.nazov }

            // Ciele sa prepisujú celé - inak by po zmene ostávali visieť staré.
            repo.denik.aktivneCiele().forEach { repo.zmazCiel(it.kluc) }

            vybraneZrucnosti.forEachIndexed { i, id ->
                repo.ulozCiel(
                    CielEntity(
                        druh = DruhCiela.ZRUCNOST.kluc,
                        kluc = id,
                        nazov = nazvy[id] ?: id,
                        priorita = i + 1,
                    )
                )
            }
            vybranePartie.forEach { p ->
                repo.ulozCiel(CielEntity(druh = DruhCiela.PARTIA.kluc, kluc = p.kluc, nazov = p.nazov))
            }

            val profil = repo.profil()
            repo.ulozProfil(
                profil.copy(
                    vahaKg = vahaKg,
                    vyskaCm = vyskaCm,
                    naradie = (vybraneNaradie.map { it.kluc } + Naradie.ZIADNE.kluc)
                        .distinct()
                        .joinToString(","),
                    bielkovinyCielG = repo.odporucaneBielkoviny(vahaKg),
                    zaciatokEpochDen = if (profil.zaciatokEpochDen == 0L) {
                        repo.dnes()
                    } else {
                        profil.zaciatokEpochDen
                    },
                    onboardingHotovy = true,
                )
            )
            hotovo()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingObrazovka(hotovo: () -> Unit, model: OnboardingModel = viewModel()) {
    val zrucnosti by model.zrucnosti.collectAsStateWithLifecycle()
    val ulozene by model.ulozene.collectAsStateWithLifecycle()

    val vybraneZrucnosti = remember { mutableListOf<String>().toMutableStateList() }
    val vybranePartie = remember { mutableListOf<Partia>().toMutableStateList() }
    val vybraneNaradie = remember { mutableListOf<Naradie>().toMutableStateList() }
    var vaha by remember { mutableStateOf("") }
    var vyska by remember { mutableStateOf("") }
    var predvyplnene by remember { mutableStateOf(false) }

    // Predvyplnenie uloženým stavom. Bez tohto sa obrazovka po opätovnom otvorení
    // tvárila prázdna a vyzeralo to, že sa ciele neuložili.
    LaunchedEffect(ulozene) {
        val u = ulozene ?: return@LaunchedEffect
        if (predvyplnene) return@LaunchedEffect
        vybraneZrucnosti.clear(); vybraneZrucnosti.addAll(u.zrucnosti)
        vybranePartie.clear(); vybranePartie.addAll(u.partie)
        vybraneNaradie.clear()
        vybraneNaradie.addAll(
            u.naradie.ifEmpty { listOf(Naradie.HRAZDA, Naradie.JEDNORUCKY, Naradie.PODLOZKA) }
        )
        vaha = u.vahaKg
        vyska = u.vyskaCm
        predvyplnene = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("O čo ti ide", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Podľa tohto sa skladá každý tréning. Dá sa to kedykoľvek zmeniť.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Nadpis("Zručnosti")
        Text(
            "Vyber najviac dve — tretia by ubrala čas obom.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (zrucnosti.isEmpty()) {
            CircularProgressIndicator()
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                zrucnosti.forEach { r ->
                    FilterChip(
                        selected = r.id in vybraneZrucnosti,
                        onClick = { prepni(vybraneZrucnosti, r.id) },
                        label = { Text(r.nazov) },
                    )
                }
            }
        }

        Nadpis("Na čom chceš pribrať")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Partia.entries.forEach { p ->
                FilterChip(
                    selected = p in vybranePartie,
                    onClick = { prepni(vybranePartie, p) },
                    label = { Text(p.nazov) },
                )
            }
        }

        Nadpis("Čo máš doma")
        Text(
            "Bez zapnutého náradia ostanú len cviky na zemi — hrazdu a jednoručky si zapni.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Naradie.entries.filter { it != Naradie.ZIADNE }.forEach { n ->
                FilterChip(
                    selected = n in vybraneNaradie,
                    onClick = { prepni(vybraneNaradie, n) },
                    label = { Text(n.nazov) },
                )
            }
        }

        Nadpis("Pár čísel")
        Text(
            "Váha slúži len na výpočet denného cieľa bielkovín (1,6–2,2 g na kilo).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vaha,
            onValueChange = { vaha = it.filter { z -> z.isDigit() || z == '.' } },
            label = { Text("Váha (kg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vyska,
            onValueChange = { vyska = it.filter { z -> z.isDigit() } },
            label = { Text("Výška (cm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                model.uloz(
                    vybraneZrucnosti = vybraneZrucnosti.take(2),
                    vybranePartie = vybranePartie.toList(),
                    vybraneNaradie = vybraneNaradie.toList(),
                    vahaKg = vaha.toDoubleOrNull() ?: 80.0,
                    vyskaCm = vyska.toIntOrNull() ?: 180,
                    hotovo = hotovo,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Text("Uložiť a pokračovať", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Nadpis(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
}

private fun <T> prepni(zoznam: SnapshotStateList<T>, hodnota: T) {
    if (hodnota in zoznam) zoznam.remove(hodnota) else zoznam.add(hodnota)
}
