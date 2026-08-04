package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.ProfilEntity
import sk.havrila.cvicime.domain.PlanTreningu

class DomovModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _profil = MutableStateFlow<ProfilEntity?>(null)
    val profil = _profil.asStateFlow()

    private val _plan = MutableStateFlow<PlanTreningu?>(null)
    val plan = _plan.asStateFlow()

    private val _prvyMesiac = MutableStateFlow(true)
    val prvyMesiac = _prvyMesiac.asStateFlow()

    private val _pocetTreningov = MutableStateFlow(0)
    val pocetTreningov = _pocetTreningov.asStateFlow()

    init {
        obnov(repo.prefs.poslednyCasMinut)
    }

    fun obnov(minuty: Int) {
        viewModelScope.launch {
            val p = repo.profil()
            _profil.value = p
            _plan.value = runCatching { repo.zostavPlan(minuty) }.getOrNull()
            _prvyMesiac.value = p.zaciatokEpochDen == 0L || (repo.dnes() - p.zaciatokEpochDen) < 28
            _pocetTreningov.value = repo.denik.pocetTreningovOd(repo.zaciatokTyzdna(repo.dnes()))
        }
    }

    fun zapamatajCas(minuty: Int) {
        repo.prefs.poslednyCasMinut = minuty
    }
}

@Composable
fun DomovObrazovka(
    naOnboarding: () -> Unit,
    naTest: () -> Unit,
    naPrehravac: (Int) -> Unit,
    model: DomovModel = viewModel(),
) {
    val profil by model.profil.collectAsStateWithLifecycle()
    val plan by model.plan.collectAsStateWithLifecycle()
    val prvyMesiac by model.prvyMesiac.collectAsStateWithLifecycle()
    val pocetTyzdna by model.pocetTreningov.collectAsStateWithLifecycle()

    var minuty by remember { mutableIntStateOf(30) }
    var zvolene by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Dnes", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            if (pocetTyzdna == 0) "Tento týždeň zatiaľ nič." else "Tento týždeň: $pocetTyzdna tréningy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        when {
            profil == null -> Text("Načítavam…")

            !profil!!.onboardingHotovy -> KartaKroku(
                titulok = "Najprv mi povedz, o čo ti ide",
                text = "Vyber si ciele a povedz, aké máš náradie. Bez toho ti neviem poskladať " +
                    "tréning, ktorý dáva zmysel.",
                tlacidlo = "Nastaviť ciele",
                akcia = naOnboarding,
            )

            !profil!!.vstupnyTestHotovy -> KartaKroku(
                titulok = "Vstupný test",
                text = "Osem krátkych meraní, asi 15 minút. Z nich sa nastaví, na ktorom stupni " +
                    "každého cviku začínaš — inak by prvý mesiac bol buď trápne ľahký, alebo nesplniteľný.",
                tlacidlo = "Spraviť test",
                akcia = naTest,
            )

            else -> {
                Text("Koľko máš dnes času?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45).forEach { m ->
                        FilterChip(
                            selected = zvolene && minuty == m,
                            onClick = {
                                minuty = m
                                zvolene = true
                                model.zapamatajCas(m)
                                model.obnov(m)
                            },
                            label = { Text("$m min") },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                plan?.let { p ->
                    Card(colors = CardDefaults.cardColors()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Odhad ${p.odhadMinut} min",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(10.dp))
                            p.bloky.forEach { blok ->
                                Text(
                                    blok.blok.nazov,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                blok.polozky.forEach { polozka ->
                                    val jednotka = if (polozka.cvik.meranie == "vydrz") "s" else "×"
                                    Text(
                                        "• ${polozka.cvik.nazov} — ${polozka.serie}× ${polozka.ciel}$jednotka",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            if (p.zdovodnenie.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                p.zdovodnenie.forEach {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { naPrehravac(minuty) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    ) {
                        Text("Poďme", style = MaterialTheme.typography.titleLarge)
                    }
                } ?: Text(
                    "Zatiaľ ti neviem poskladať tréning — chýba knižnica cvikov alebo ciele.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (prvyMesiac) {
            Spacer(Modifier.height(24.dp))
            PrvyMesiacKarta()
        }
    }
}

@Composable
private fun KartaKroku(titulok: String, text: String, tlacidlo: String, akcia: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(titulok, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Button(onClick = akcia) { Text(tlacidlo) }
        }
    }
}

/**
 * Najdôležitejší text v celej appke. Bez neho ľudia v treťom týždni prestanú,
 * lebo v zrkadle nič nevidia - a nevedia, že to tak má byť.
 */
@Composable
private fun PrvyMesiacKarta() {
    var rozbalene by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Prvý mesiac", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Teraz rastie hlavne nervový systém, nie sval. Čísla pôjdu hore rýchlo, " +
                    "zrkadlo príde okolo šiesteho týždňa. Nie je to výhovorka — je to poradie, " +
                    "v akom to funguje.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (rozbalene) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "V prvých dvoch až štyroch týždňoch je prakticky celý nárast sily neurálny: " +
                        "mozog sa učí zapájať viac svalových vlákien naraz a rýchlejšie. Merateľné " +
                        "zväčšenie svalu sa v štúdiách objavuje až okolo šiesteho týždňa. Preto ti " +
                        "appka v tomto období ukazuje opakovania a výdrže, nie obvody.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { rozbalene = !rozbalene }) {
                Text(if (rozbalene) "Skryť" else "Prečo to tak je")
            }
        }
    }
}
