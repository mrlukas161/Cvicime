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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.ProfilEntity
import sk.havrila.cvicime.domain.PlanTreningu

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

    private val _pocetTyzdna = MutableStateFlow(0)
    val pocetTyzdna = _pocetTyzdna.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.profilTok(), repo.cieleTok(), _minuty) { p, c, m -> Triple(p, c, m) }
                .collectLatest { (p, c, m) ->
                    val profil = p ?: repo.profil()
                    _profil.value = profil
                    _ciele.value = c
                    _pocetCvikov.value = repo.katalog.pocetCvikov()
                    _prvyMesiac.value =
                        profil.zaciatokEpochDen == 0L || (repo.dnes() - profil.zaciatokEpochDen) < 28
                    _pocetTyzdna.value =
                        repo.denik.pocetTreningovOd(repo.zaciatokTyzdna(repo.dnes()))
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
    val chyba by model.chyba.collectAsStateWithLifecycle()
    val pocetCvikov by model.pocetCvikov.collectAsStateWithLifecycle()
    val prvyMesiac by model.prvyMesiac.collectAsStateWithLifecycle()
    val pocetTyzdna by model.pocetTyzdna.collectAsStateWithLifecycle()
    val minuty by model.minuty.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Dnes", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            when (pocetTyzdna) {
                0 -> "Tento týždeň zatiaľ nič."
                1 -> "Tento týždeň jeden tréning."
                else -> "Tento týždeň $pocetTyzdna tréningy."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Prázdna knižnica je jediná chyba, ktorú používateľ nevie obísť - povedať to nahlas.
        if (pocetCvikov == 0) {
            Karta(
                titulok = "Knižnica cvikov sa nenačítala",
                text = "V appke je 0 cvikov, takže sa nedá poskladať nič. Toto je chyba appky, " +
                    "nie tvoja. Pomôže preinštalovanie; ak nie, treba to nahlásiť.",
            )
            Spacer(Modifier.height(16.dp))
        }

        val p = profil
        when {
            p == null -> Text("Načítavam…")

            !p.onboardingHotovy -> KartaKroku(
                titulok = "Najprv mi povedz, o čo ti ide",
                text = "Vyber si ciele a povedz, aké máš náradie. Bez toho ti neviem poskladať " +
                    "tréning, ktorý dáva zmysel.",
                tlacidlo = "Nastaviť ciele",
                akcia = naOnboarding,
            )

            !p.vstupnyTestHotovy -> KartaKroku(
                titulok = "Vstupný test",
                text = "Pri každom cviku vyberieš najťažší stupeň, ktorý zvládneš. Trvá to pár " +
                    "minút a bez toho by prvý mesiac bol buď trápne ľahký, alebo nesplniteľný.",
                tlacidlo = "Spraviť test",
                akcia = naTest,
            )

            else -> {
                Text("Koľko máš dnes času?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45).forEach { m ->
                        FilterChip(
                            selected = minuty == m,
                            onClick = { model.nastavCas(m) },
                            label = { Text("$m min") },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                val bloky = plan?.bloky.orEmpty()
                if (bloky.isEmpty()) {
                    Karta(
                        titulok = "Z tvojho nastavenia sa nedá nič poskladať",
                        text = buildString {
                            append("Najčastejšia príčina: v Nastaveniach nie je zapnuté žiadne ")
                            append("náradie, alebo si nevybral ani jeden cieľ. ")
                            chyba?.let { append("\n\nChyba: $it") }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = naOnboarding, modifier = Modifier.fillMaxWidth()) {
                        Text("Otvoriť ciele a náradie")
                    }
                } else {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Odhad ${plan?.odhadMinut ?: 0} min",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(10.dp))
                            bloky.forEach { blok ->
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
                            plan?.zdovodnenie?.forEach {
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
                    enabled = bloky.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Text("Poďme", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        if (prvyMesiac) {
            Spacer(Modifier.height(24.dp))
            PrvyMesiacKarta()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Karta(titulok: String, text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulok, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
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
            Button(onClick = akcia, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(tlacidlo, style = MaterialTheme.typography.titleLarge)
            }
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
