package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import sk.havrila.cvicime.aktualizacia.AppUpdater
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

    private val _deti = MutableStateFlow<List<sk.havrila.cvicime.data.denik.DietaEntity>>(emptyList())
    val deti = _deti.asStateFlow()

    init {
        viewModelScope.launch {
            repo.detiTok().collectLatest { _deti.value = it }
        }
    }

    fun pridajDieta(meno: String, vek: Int) {
        viewModelScope.launch {
            repo.ulozDieta(
                sk.havrila.cvicime.data.denik.DietaEntity(meno = meno, vek = vek, farba = 0)
            )
        }
    }

    fun ulozKotvu(kontext: android.content.Context, text: String, minuty: Int) {
        viewModelScope.launch { repo.ulozKotvu(kontext, text, minuty) }
    }

    /** Export denníka do Downloads cez MediaStore - bez povolení, bez SAF dialógov. */
    fun exportuj(kontext: android.content.Context, vysledok: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repo.exportujJson()
                val nazov = "cvicime-zaloha-${java.time.LocalDate.now()}.json"
                val hodnoty = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, nazov)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val uri = kontext.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, hodnoty,
                )
                if (uri == null) {
                    vysledok("Nepodarilo sa vytvoriť súbor.")
                    return@launch
                }
                kontext.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                }
                vysledok("Uložené do Downloads ako $nazov")
            } catch (t: Throwable) {
                vysledok("Export zlyhal: ${t.message}")
            }
        }
    }

    val prefs get() = repo.prefs
}

@Composable
fun NastaveniaObrazovka(
    naOnboarding: () -> Unit,
    naTeoria: () -> Unit,
    naZbierka: () -> Unit,
    model: NastaveniaModel = viewModel(),
) {
    val profil by model.profil.collectAsStateWithLifecycle()
    val ciele by model.ciele.collectAsStateWithLifecycle()
    val pocetCvikov by model.pocetCvikov.collectAsStateWithLifecycle()
    val deti by model.deti.collectAsStateWithLifecycle()
    val kontext = LocalContext.current
    val rozsah = rememberCoroutineScope()

    var vibracie by remember { mutableStateOf(model.prefs.vibracieZapnute) }
    var obrazovka by remember { mutableStateOf(model.prefs.obrazovkaStaleZapnuta) }
    var hlas by remember { mutableStateOf(model.prefs.hlasZapnuty) }
    var hladam by remember { mutableStateOf(false) }
    var stavAktualizacie by remember { mutableStateOf<String?>(null) }
    var dialogDieta by remember { mutableStateOf(false) }
    var stavExportu by remember { mutableStateOf<String?>(null) }
    var kotvaText by remember(profil?.kotvaText) { mutableStateOf(profil?.kotvaText ?: "") }
    var kotvaCas by remember(profil?.kotvaMinuty) {
        mutableStateOf(
            profil?.kotvaMinuty?.takeIf { it >= 0 }
                ?.let { "%02d:%02d".format(it / 60, it % 60) } ?: ""
        )
    }

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
        Prepinac("Hlas a pípanie", hlas) {
            hlas = it
            model.prefs.hlasZapnuty = it
        }
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

        Text("Pripomienka", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Naviaž ju na niečo, čo sa deje aj tak — napríklad „keď uložím deti\". " +
                "Kotva funguje lepšie než hodina.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kotvaText,
            onValueChange = { kotvaText = it },
            label = { Text("Kotva (napr. keď uložím deti)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kotvaCas,
            onValueChange = { kotvaCas = it.filter { z -> z.isDigit() || z == ':' } },
            label = { Text("Čas kontroly (HH:MM), prázdne = vypnuté") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            val casti = kotvaCas.split(":")
            val minuty = if (casti.size == 2) {
                val h = casti[0].toIntOrNull()
                val m = casti[1].toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) h * 60 + m else -1
            } else {
                -1
            }
            model.ulozKotvu(kontext, kotvaText, minuty)
        }) { Text("Uložiť pripomienku") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Deti", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pridané deti sa dajú vybrať pri tréningu — cvičia svoje zvieracie cviky " +
                "v tvojich pauzách a zbierajú zvieratá.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (deti.isEmpty()) {
            Text("Zatiaľ žiadne.", style = MaterialTheme.typography.bodyMedium)
        } else {
            deti.forEach { d ->
                Text("• ${d.meno} (${d.vek} r.)", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { dialogDieta = true }) { Text("Pridať dieťa") }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = naZbierka) { Text("Zbierka zvierat") }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Prečo to tak je", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Krátke vysvetlenia pravidiel appky — objem, pauzy, zlyhanie, deti a motivácia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = naTeoria) { Text("Otvoriť teóriu") }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Záloha", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Celý denník (tréningy, testy, ciele, zvieratá) ako JSON do priečinka Downloads.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { model.exportuj(kontext) { stavExportu = it } }) {
            Text("Exportovať zálohu")
        }
        stavExportu?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
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

        Text("Aktualizácie", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Appka sa raz denne sama pozrie, či nie je novšia verzia. Tu ju vieš skontrolovať hneď.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            enabled = !hladam,
            onClick = {
                hladam = true
                stavAktualizacie = null
                rozsah.launch {
                    val nova = AppUpdater.zisti(kontext, nasilu = true)
                    if (nova == null) {
                        hladam = false
                        stavAktualizacie = "Máš najnovšiu verziu."
                    } else if (!AppUpdater.smieInstalovat(kontext)) {
                        hladam = false
                        stavAktualizacie = "Je nová verzia ${nova.nazovVerzie}, ale Android najprv " +
                            "potrebuje povolenie inštalovať aplikácie z tejto appky. Otváram nastavenie."
                        AppUpdater.otvorPovolenie(kontext)
                    } else {
                        stavAktualizacie = "Sťahujem ${nova.nazovVerzie}…"
                        val problem = AppUpdater.stiahniANainstaluj(kontext, nova)
                        hladam = false
                        stavAktualizacie = problem ?: "Stiahnuté — potvrď inštaláciu."
                    }
                }
            },
        ) { Text(if (hladam) "Kontrolujem…" else "Skontrolovať aktualizácie") }
        stavAktualizacie?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

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

    if (dialogDieta) {
        DialogDieta(
            zavri = { dialogDieta = false },
            uloz = { meno, vek -> model.pridajDieta(meno, vek) },
        )
    }
}

@Composable
private fun DialogDieta(zavri: () -> Unit, uloz: (String, Int) -> Unit) {
    var meno by remember { mutableStateOf("") }
    var vek by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = zavri,
        title = { Text("Nové dieťa") },
        text = {
            Column {
                OutlinedTextField(
                    value = meno,
                    onValueChange = { meno = it },
                    label = { Text("Meno") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vek,
                    onValueChange = { vek = it.filter { z -> z.isDigit() } },
                    label = { Text("Vek (roky)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val v = vek.toIntOrNull()
                if (meno.isNotBlank() && v != null) {
                    uloz(meno.trim(), v)
                    zavri()
                }
            }) { Text("Pridať") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = zavri) { Text("Zrušiť") }
        },
    )
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
