package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sk.havrila.cvicime.BuildConfig
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.ProfilEntity

class NastaveniaModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _profil = MutableStateFlow<ProfilEntity?>(null)
    val profil = _profil.asStateFlow()

    private val _ciele = MutableStateFlow<List<CielEntity>>(emptyList())
    val ciele = _ciele.asStateFlow()

    private val _pocetCvikov = MutableStateFlow(0)
    val pocetCvikov = _pocetCvikov.asStateFlow()

    // Tokom, nie jednorazovo - inak by po zmene cieľov obrazovka ukazovala staré hodnoty.
    init {
        viewModelScope.launch {
            repo.profilTok().collectLatest { _profil.value = it ?: repo.profil() }
        }
        viewModelScope.launch {
            repo.cieleTok().collectLatest {
                _ciele.value = it
                _pocetCvikov.value = repo.katalog.pocetCvikov()
            }
        }
    }

    fun zopakujTest() {
        viewModelScope.launch {
            val p = repo.profil()
            repo.ulozProfil(p.copy(vstupnyTestHotovy = false))
        }
    }

    val prefs get() = repo.prefs
}

@Composable
fun NastaveniaObrazovka(naOnboarding: () -> Unit, model: NastaveniaModel = viewModel()) {
    val profil by model.profil.collectAsStateWithLifecycle()
    val ciele by model.ciele.collectAsStateWithLifecycle()
    val pocetCvikov by model.pocetCvikov.collectAsStateWithLifecycle()
    @Suppress("UNUSED_VARIABLE")
    val kontext = LocalContext.current

    var vibracie by remember { mutableStateOf(model.prefs.vibracieZapnute) }
    var obrazovka by remember { mutableStateOf(model.prefs.obrazovkaStaleZapnuta) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Nastavenia", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(20.dp))
        Text("Ciele", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        if (ciele.isEmpty()) {
            Text(
                "Zatiaľ žiadne. Bez cieľov appka skladá len všeobecný tréning.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ciele.forEach { Text("• ${it.nazov}", style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = naOnboarding) { Text("Zmeniť ciele a náradie") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Prehrávač", style = MaterialTheme.typography.titleLarge)
        Prepinac("Vibrácie na konci série", vibracie) {
            vibracie = it
            model.prefs.vibracieZapnute = it
        }
        Prepinac("Nechať obrazovku zapnutú", obrazovka) {
            obrazovka = it
            model.prefs.obrazovkaStaleZapnuta = it
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Vstupný test", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Retest sa oplatí po štyroch týždňoch — a v podobnú dennú dobu ako prvýkrát. " +
                "Sila cez deň kolíše, inak by graf ukázal progres, ktorý žiadny nie je.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = model::zopakujTest) { Text("Spraviť test znova") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("O appke", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Verzia ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})\n" +
                "Knižnica: $pocetCvikov cvikov\n" +
                "Váha ${profil?.vahaKg ?: 0.0} kg · cieľ bielkovín ${profil?.bielkovinyCielG ?: 0} g/deň",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Časť pravidiel v appke (nácvik zručností, prahy postupu, deload) vychádza " +
                "z praxe trénerov, nie z dôkazov — vedecké štúdie na ne zatiaľ neexistujú. " +
                "Kde to tak je, appka to hovorí priamo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Prepinac(popis: String, hodnota: Boolean, zmena: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(popis, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = hodnota, onCheckedChange = zmena)
    }
}
