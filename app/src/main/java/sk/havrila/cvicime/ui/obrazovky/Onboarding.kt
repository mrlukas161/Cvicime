package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia

class OnboardingModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _zrucnosti = MutableStateFlow<List<RebrikEntity>>(emptyList())
    val zrucnosti = _zrucnosti.asStateFlow()

    init {
        viewModelScope.launch {
            _zrucnosti.value = repo.rebriky().filter { it.jeZrucnost }
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
            val zrucnostiMapa = repo.rebriky().associateBy { it.id }

            // Ciele sa vždy prepíšu celé - inak by po zmene ostávali visieť staré.
            repo.denik.aktivneCiele().forEach { repo.zmazCiel(it.kluc) }

            vybraneZrucnosti.forEachIndexed { i, id ->
                repo.ulozCiel(
                    CielEntity(
                        druh = DruhCiela.ZRUCNOST.kluc,
                        kluc = id,
                        nazov = zrucnostiMapa[id]?.nazov ?: id,
                        priorita = i + 1,
                    )
                )
            }
            vybranePartie.forEach { p ->
                repo.ulozCiel(
                    CielEntity(druh = DruhCiela.PARTIA.kluc, kluc = p.kluc, nazov = p.nazov)
                )
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
                    zaciatokEpochDen = if (profil.zaciatokEpochDen == 0L) repo.dnes() else profil.zaciatokEpochDen,
                    onboardingHotovy = true,
                )
            )
            hotovo()
        }
    }
}

@Composable
fun OnboardingObrazovka(hotovo: () -> Unit, model: OnboardingModel = viewModel()) {
    val zrucnosti by model.zrucnosti.collectAsStateWithLifecycle()

    val vybraneZrucnosti = remember { mutableListOf<String>().toMutableStateList() }
    val vybranePartie = remember { mutableListOf<Partia>().toMutableStateList() }
    val vybraneNaradie = remember {
        mutableListOf(Naradie.HRAZDA, Naradie.JEDNORUCKY, Naradie.PODLOZKA).toMutableStateList()
    }
    var vaha by remember { mutableStateOf("80") }
    var vyska by remember { mutableStateOf("180") }

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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            zrucnosti.forEach { r ->
                FilterChip(
                    selected = r.id in vybraneZrucnosti,
                    onClick = { prepni(vybraneZrucnosti, r.id) },
                    label = { Text(r.nazov) },
                )
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
            Text("Uložiť", style = MaterialTheme.typography.titleLarge)
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
