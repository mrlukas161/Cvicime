package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import sk.havrila.cvicime.aktualizacia.AppUpdater
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.DietaEntity
import sk.havrila.cvicime.data.denik.ProfilEntity
import sk.havrila.cvicime.data.denik.UcastnikEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Obmedzenie
import sk.havrila.cvicime.trening.RezimZvuku
import sk.havrila.cvicime.ui.komponenty.Inicialka
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk

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

    private val _deti = MutableStateFlow<List<DietaEntity>>(emptyList())
    val deti = _deti.asStateFlow()

    init {
        viewModelScope.launch {
            repo.detiTok().collectLatest { _deti.value = it }
        }
    }

    private val _dospeli = MutableStateFlow<List<UcastnikEntity>>(emptyList())
    val dospeli = _dospeli.asStateFlow()

    init {
        viewModelScope.launch {
            repo.vsetciUcastniciTok().collectLatest { _dospeli.value = it }
        }
    }

    /** Nový aj upravený dospelý; pri panvovom dne repozitár sám pridá cieľ rebríka Panvové dno. */
    fun ulozDospeleho(ucastnik: UcastnikEntity) {
        viewModelScope.launch { repo.ulozUcastnika(ucastnik) }
    }

    /** Nové aj upravené dieťa (oprava preklepu v mene, vek, farba). */
    fun ulozDieta(dieta: DietaEntity) {
        viewModelScope.launch { repo.ulozDieta(dieta) }
    }

    /**
     * Odstránenie je mäkké: dieťa zmizne z výberu, ale jeho zvieratá a účasť na tréningoch
     * v denníku ostanú (história sa nemaže).
     */
    fun odstranDieta(dieta: DietaEntity) {
        viewModelScope.launch { repo.ulozDieta(dieta.copy(aktivne = false)) }
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

/**
 * Nastavenia ako skupiny riadkov (ikona + titul + popis + ovládač vpravo), nie dlhý formulár.
 * Každá doterajšia funkcia ostáva: ciele, náradie, prepínače prehrávača, kotva pripomienky,
 * deti a zbierka, teória, export, retest, aktualizácie, o appke.
 */
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
    val dospeli by model.dospeli.collectAsStateWithLifecycle()
    val kontext = LocalContext.current
    val rozsah = rememberCoroutineScope()
    val haptika = rememberHaptika()

    var vibracie by remember { mutableStateOf(model.prefs.vibracieZapnute) }
    var obrazovka by remember { mutableStateOf(model.prefs.obrazovkaStaleZapnuta) }
    var hlas by remember { mutableStateOf(model.prefs.rezimZvuku == RezimZvuku.HLAS) }
    var hladam by remember { mutableStateOf(false) }
    var stavAktualizacie by remember { mutableStateOf<String?>(null) }
    // null = zavretý, 0 = nové dieťa, inak id upravovaného (Long sa pri otočení zachová)
    var dialogDieta by rememberSaveable { mutableStateOf<Long?>(null) }
    // null = dialóg zavretý; 0 = pridanie, inak id upravovaného dospelého
    var dialogDospely by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportujem by remember { mutableStateOf(false) }
    var stavExportu by remember { mutableStateOf<String?>(null) }
    var stavTestu by remember { mutableStateOf<String?>(null) }

    var upravujemKotvu by remember { mutableStateOf(false) }
    var dialogCas by remember { mutableStateOf(false) }
    var stavKotvy by remember { mutableStateOf<String?>(null) }
    var kotvaText by remember(profil?.kotvaText) { mutableStateOf(profil?.kotvaText ?: "") }
    var kotvaMinuty by remember(profil?.kotvaMinuty) { mutableStateOf(profil?.kotvaMinuty ?: -1) }

    // Farby skupín - rovnaký jazyk ako zvyšok appky (primary = tréning, tertiary = deti…).
    val farbaTrening = MaterialTheme.colorScheme.primary
    val farbaPrehravac = MaterialTheme.colorScheme.secondary
    val farbaRodina = MaterialTheme.colorScheme.tertiary
    val farbaPripomienky = citatelnaFarbaNastaveni(FarbyBlokov.pre(Blok.ROZOHRIATIE))
    val farbaData = citatelnaFarbaNastaveni(FarbyBlokov.pre(Blok.MOBILITA))
    val farbaOAppke = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Nastavenia", style = MaterialTheme.typography.headlineLarge)

        // ------------------------------------------------------------ tréning a ciele
        SekciaNastaveni("Tréning a ciele") {
            RiadokNastavenia(
                ikona = Icons.Outlined.Flag,
                farba = farbaTrening,
                titul = "Ciele",
                popis = if (ciele.isEmpty()) {
                    "Zatiaľ žiadne. Bez cieľov appka skladá len všeobecný tréning."
                } else {
                    ciele.joinToString(" · ") { it.nazov }
                },
                onClick = naOnboarding,
                vpravo = { Sipka() },
            )
            Oddelovac()
            val naradie = profil?.naradie.orEmpty().split(",")
                .mapNotNull { Naradie.z(it.trim()) }
                .filter { it != Naradie.ZIADNE }
            RiadokNastavenia(
                ikona = Icons.Outlined.FitnessCenter,
                farba = farbaTrening,
                titul = "Náradie",
                popis = if (naradie.isEmpty()) "Len vlastná váha" else naradie.joinToString(" · ") { it.nazov },
                onClick = naOnboarding,
                vpravo = { Sipka() },
            )
            Oddelovac()
            RiadokNastavenia(
                ikona = Icons.Outlined.MonitorWeight,
                farba = farbaTrening,
                titul = "Telo a výživa",
                popis = "Váha ${profil?.vahaKg?.let { formatujKg(it) } ?: "–"} kg · " +
                    "cieľ bielkovín ${profil?.bielkovinyCielG ?: 0} g/deň",
                onClick = naOnboarding,
                vpravo = { Sipka() },
            )
            Oddelovac()
            RiadokNastavenia(
                ikona = Icons.Outlined.Replay,
                farba = farbaTrening,
                titul = "Spraviť vstupný test znova",
                popis = stavTestu ?: ("Retest sa oplatí po štyroch týždňoch — a v podobnú dennú dobu ako " +
                    "prvýkrát. Sila cez deň kolíše, inak by graf ukázal progres, ktorý žiadny nie je."),
                onClick = {
                    model.zopakujTest()
                    haptika.potvrdenie()
                    stavTestu = "Hotovo — test ťa čaká na obrazovke Dnes."
                },
            )
        }

        // ------------------------------------------------------------ prehrávač
        SekciaNastaveni("Prehrávač") {
            RiadokPrepinaca(
                ikona = Icons.Outlined.RecordVoiceOver,
                farba = farbaPrehravac,
                titul = "Hlas a pípanie",
                popis = "Ohlási cvik a pípa pri odpočte. Hudbu z inej appky na chvíľu stíši. " +
                    "V prehrávači sa dá prepnúť tlačidlom reproduktora.",
                hodnota = hlas,
            ) {
                hlas = it
                model.prefs.hlasZapnuty = it
                model.prefs.rezimZvuku = if (it) RezimZvuku.HLAS else RezimZvuku.PIPANIE
            }
            Oddelovac()
            RiadokPrepinaca(
                ikona = Icons.Outlined.Vibration,
                farba = farbaPrehravac,
                titul = "Vibrácie na konci série",
                popis = null,
                hodnota = vibracie,
            ) {
                vibracie = it
                model.prefs.vibracieZapnute = it
            }
            Oddelovac()
            RiadokPrepinaca(
                ikona = Icons.Outlined.LightMode,
                farba = farbaPrehravac,
                titul = "Nechať obrazovku zapnutú",
                popis = "Počas tréningu nezhasne",
                hodnota = obrazovka,
            ) {
                obrazovka = it
                model.prefs.obrazovkaStaleZapnuta = it
            }
        }

        // ------------------------------------------------------------ rodina
        SekciaNastaveni(
            "Rodina",
            pata = "Dospelý cvičí popri tebe vlastný plán so svojimi stupňami — začína na prvom " +
                "a ďalej ho posúva appka podľa jeho sérií. Deti cvičia svoje zvieracie cviky " +
                "v tvojich pauzách a zbierajú zvieratá. Kto dnes cvičí, vyberieš na obrazovke Dnes.",
        ) {
            dospeli.forEach { u ->
                RiadokDospeleho(u) { dialogDospely = u.id }
                Oddelovac()
            }
            RiadokNastavenia(
                ikona = Icons.Outlined.PersonAdd,
                farba = farbaRodina,
                titul = "Pridať dospelého",
                popis = if (dospeli.isEmpty()) "Partner či partnerka — každý so svojím plánom" else null,
                onClick = { dialogDospely = 0L },
            )
            Oddelovac()
            val poradieFarieb = deti.sortedBy { it.id }.mapIndexed { i, d -> d.id to i }.toMap()
            deti.forEach { d ->
                RiadokDietata(d, farbaDietataNastavenia(d, poradieFarieb[d.id] ?: 0)) { dialogDieta = d.id }
                Oddelovac()
            }
            RiadokNastavenia(
                ikona = Icons.Outlined.PersonAdd,
                farba = farbaRodina,
                titul = "Pridať dieťa",
                popis = if (deti.isEmpty()) "Zatiaľ žiadne" else null,
                onClick = { dialogDieta = 0L },
            )
            Oddelovac()
            RiadokNastavenia(
                ikona = Icons.Outlined.Pets,
                farba = farbaRodina,
                titul = "Zbierka zvierat",
                popis = "Zvieratá, ktoré deti objavili pri cvičení",
                onClick = naZbierka,
                vpravo = { Sipka() },
            )
        }

        // ------------------------------------------------------------ pripomienky
        SekciaNastaveni("Pripomienky", pata = "Kotva funguje lepšie než hodina.") {
            val ulozenaKotva = profil?.kotvaText.orEmpty()
            val ulozeneMinuty = profil?.kotvaMinuty ?: -1
            val otocenie by animateFloatAsState(if (upravujemKotvu) 180f else 0f, label = "kotvaSipka")
            RiadokNastavenia(
                ikona = Icons.Outlined.NotificationsActive,
                farba = farbaPripomienky,
                titul = "Pripomienka s kotvou",
                popis = stavKotvy ?: when {
                    ulozeneMinuty < 0 -> "Vypnutá"
                    ulozenaKotva.isBlank() -> "Každý deň o ${hhmm(ulozeneMinuty)}"
                    else -> "„$ulozenaKotva“ · každý deň o ${hhmm(ulozeneMinuty)}"
                },
                onClick = {
                    upravujemKotvu = !upravujemKotvu
                    stavKotvy = null
                },
                vpravo = {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (upravujemKotvu) "Zbaliť" else "Upraviť pripomienku",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(otocenie),
                    )
                },
            )
            AnimatedVisibility(
                visible = upravujemKotvu,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Text(
                        "Naviaž ju na niečo, čo sa deje aj tak — napríklad „keď uložím deti“. " +
                            "Bez času je pripomienka vypnutá.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = kotvaText,
                        onValueChange = { kotvaText = it },
                        label = { Text("Kotva (napr. keď uložím deti)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { dialogCas = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (kotvaMinuty >= 0) "Kontrola o ${hhmm(kotvaMinuty)}" else "Nastaviť čas")
                        }
                        if (kotvaMinuty >= 0) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { kotvaMinuty = -1 }) { Text("Bez času") }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            model.ulozKotvu(kontext, kotvaText.trim(), kotvaMinuty)
                            haptika.potvrdenie()
                            upravujemKotvu = false
                            stavKotvy = if (kotvaMinuty >= 0) {
                                "Uložené — každý deň o ${hhmm(kotvaMinuty)}"
                            } else {
                                "Uložené — pripomienka je vypnutá"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Uložiť pripomienku") }
                }
            }
        }

        // ------------------------------------------------------------ dáta
        SekciaNastaveni("Dáta") {
            RiadokNastavenia(
                ikona = Icons.Outlined.Download,
                farba = farbaData,
                titul = "Exportovať zálohu",
                popis = stavExportu
                    ?: "Celý denník (tréningy, testy, ciele, zvieratá) ako JSON do priečinka Downloads.",
                onClick = if (exportujem) {
                    null
                } else {
                    {
                        exportujem = true
                        stavExportu = null
                        model.exportuj(kontext) {
                            stavExportu = it
                            exportujem = false
                        }
                    }
                },
                vpravo = if (exportujem) {
                    { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp) }
                } else {
                    null
                },
            )
        }

        // ------------------------------------------------------------ o appke
        SekciaNastaveni(
            "O appke",
            pata = "Časť pravidiel v appke (nácvik zručností, prahy postupu, deload) vychádza " +
                "z praxe trénerov, nie z dôkazov — vedecké štúdie na ne zatiaľ neexistujú. " +
                "Kde to tak je, appka to hovorí priamo.",
        ) {
            RiadokNastavenia(
                ikona = Icons.Outlined.AutoStories,
                farba = farbaOAppke,
                titul = "Prečo to tak je",
                popis = "Krátke vysvetlenia pravidiel appky — objem, pauzy, zlyhanie, deti a motivácia.",
                onClick = naTeoria,
                vpravo = { Sipka() },
            )
            Oddelovac()
            RiadokNastavenia(
                ikona = Icons.Outlined.SystemUpdate,
                farba = farbaOAppke,
                titul = "Skontrolovať aktualizácie",
                popis = stavAktualizacie
                    ?: "Appka sa raz denne sama pozrie, či nie je novšia verzia. Tu ju vieš skontrolovať hneď.",
                onClick = if (hladam) {
                    null
                } else {
                    {
                        hladam = true
                        stavAktualizacie = "Kontrolujem…"
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
                    }
                },
                vpravo = if (hladam) {
                    { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp) }
                } else {
                    null
                },
            )
            Oddelovac()
            RiadokNastavenia(
                ikona = Icons.Outlined.Info,
                farba = farbaOAppke,
                titul = "Cvičíme ${BuildConfig.VERSION_NAME}",
                popis = "Build ${BuildConfig.VERSION_CODE} · knižnica $pocetCvikov " +
                    sklonuj(pocetCvikov, "cvik", "cviky", "cvikov"),
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    dialogDospely?.let { id ->
        DialogDospeleho(
            povodny = dospeli.firstOrNull { it.id == id },
            pocetDospelych = dospeli.size,
            zavri = { dialogDospely = null },
            uloz = { model.ulozDospeleho(it) },
        )
    }
    dialogDieta?.let { id ->
        val povodne = deti.firstOrNull { it.id == id }
        DialogDieta(
            povodne = povodne,
            predvolenaFarba = PALETA_DETI_NASTAVENIA[deti.size % PALETA_DETI_NASTAVENIA.size],
            zavri = { dialogDieta = null },
            uloz = { model.ulozDieta(it) },
            odstran = povodne?.let { d -> { model.odstranDieta(d) } },
        )
    }
    if (dialogCas) {
        DialogCasu(
            minuty = kotvaMinuty,
            zavri = { dialogCas = false },
            potvrd = { kotvaMinuty = it },
        )
    }
}

// ------------------------------------------------------------------ stavebné kamene

@Composable
private fun SekciaNastaveni(
    nadpis: String,
    pata: String? = null,
    obsah: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        NadpisSekcie(nadpis, Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(8.dp))
        TonovanaKarta(vnutorneOdsadenie = 0.dp, obsah = obsah)
        if (pata != null) {
            Text(
                pata,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun RiadokNastavenia(
    ikona: ImageVector,
    farba: Color,
    titul: String,
    popis: String? = null,
    onClick: (() -> Unit)? = null,
    vpravo: (@Composable () -> Unit)? = null,
) {
    val haptika = rememberHaptika()
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { haptika.klik(); onClick() } else Modifier
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkonaRiadku(ikona, farba)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).animateContentSize()) {
            Text(titul, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (popis != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    popis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (vpravo != null) {
            Spacer(Modifier.width(12.dp))
            vpravo()
        }
    }
}

@Composable
private fun RiadokPrepinaca(
    ikona: ImageVector,
    farba: Color,
    titul: String,
    popis: String?,
    hodnota: Boolean,
    zmena: (Boolean) -> Unit,
) {
    val haptika = rememberHaptika()
    // Celý riadok je prepínač (väčšia dotyková plocha, správna sémantika pre TalkBack).
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = hodnota,
                role = Role.Switch,
                onValueChange = { haptika.klik(); zmena(it) },
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkonaRiadku(ikona, farba)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titul, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (popis != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    popis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = hodnota, onCheckedChange = null)
    }
}

@Composable
private fun RiadokDietata(dieta: DietaEntity, farba: Color, naUpravu: () -> Unit) {
    val haptika = rememberHaptika()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { haptika.klik(); naUpravu() }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(farba.copy(alpha = 0.24f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dieta.meno.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Grotesk),
                color = citatelnaFarbaNastaveni(farba),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(dieta.meno, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${dieta.vek} ${sklonuj(dieta.vek, "rok", "roky", "rokov")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Sipka()
    }
}

/** Dospelý v Rodine: inicialka v jeho farbe, obmedzenia, stav. Ťuk = úprava. */
@Composable
private fun RiadokDospeleho(u: UcastnikEntity, naUpravu: () -> Unit) {
    val haptika = rememberHaptika()
    val obmedzenia = Obmedzenie.zCsv(u.obmedzenia)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { haptika.klik(); naUpravu() }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Inicialka(u.meno, if (u.aktivny) Color(u.farba) else MaterialTheme.colorScheme.outlineVariant, 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(u.meno, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                buildList {
                    if (!u.aktivny) add("Neaktívny")
                    obmedzenia.forEach { add(it.nazov) }
                    if (isEmpty()) add("Bez obmedzení")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Sipka()
    }
}

/** Farby dospelých - odlišné od detskej palety, aby sa na čipoch nemýlili. */
private val PALETA_DOSPELYCH = listOf(
    Color(0xFFD0643B), // tehlová
    Color(0xFF2F8F83), // morská
    Color(0xFF7C6BC4), // fialková
    Color(0xFFC2477A), // malinová
    Color(0xFF5B7FBF), // oceľová modrá
    Color(0xFF8A9A3B), // olivová
)

/**
 * Pridanie či úprava dospelého: meno, farba, obmedzenia, aktívny. Pri obmedzení s dlhším
 * vysvetlením (Panvové dno) sa text režimu ukáže priamo pod prepínačom.
 */
@Composable
private fun DialogDospeleho(
    povodny: UcastnikEntity?,
    pocetDospelych: Int,
    zavri: () -> Unit,
    uloz: (UcastnikEntity) -> Unit,
) {
    var meno by rememberSaveable { mutableStateOf(povodny?.meno.orEmpty()) }
    var farba by rememberSaveable {
        mutableStateOf(povodny?.farba ?: PALETA_DOSPELYCH[pocetDospelych % PALETA_DOSPELYCH.size].toArgb())
    }
    var obmedzenia by remember { mutableStateOf(Obmedzenie.zCsv(povodny?.obmedzenia.orEmpty())) }
    var aktivny by rememberSaveable { mutableStateOf(povodny?.aktivny ?: true) }
    var info by remember { mutableStateOf<Obmedzenie?>(null) }
    val haptika = rememberHaptika()

    AlertDialog(
        onDismissRequest = zavri,
        title = { Text(if (povodny == null) "Nový dospelý" else "Upraviť: ${povodny.meno}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = meno,
                    onValueChange = { meno = it.take(24) },
                    label = { Text("Meno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Farba", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETA_DOSPELYCH.forEach { f ->
                        val vybrana = f.toArgb() == farba
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(f)
                                .border(3.dp, if (vybrana) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .selectable(selected = vybrana, role = Role.RadioButton) {
                                    haptika.klik()
                                    farba = f.toArgb()
                                },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Obmedzenia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Obmedzenie.entries.forEach { o ->
                    val zapnute = o in obmedzenia
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = zapnute, role = Role.Switch) {
                                haptika.klik()
                                obmedzenia = if (it) obmedzenia + o else obmedzenia - o
                                if (it && o.vysvetlenie != null) info = o
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(o.nazov, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(o.popis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (o.vysvetlenie != null) {
                            IconButton(onClick = { info = if (info == o) null else o }) {
                                Icon(Icons.Outlined.Info, contentDescription = "Čo robí režim ${o.nazov}")
                            }
                        }
                        Switch(checked = zapnute, onCheckedChange = null)
                    }
                    val text = o.vysvetlenie
                    AnimatedVisibility(
                        visible = info == o && text != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        TonovanaKarta(farba = Color(farba), vnutorneOdsadenie = 12.dp) {
                            Text(text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = aktivny, role = Role.Switch) { haptika.klik(); aktivny = it }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Aktívny", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "Neaktívny sa na obrazovke Dnes neponúka, jeho história ostáva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = aktivny, onCheckedChange = null)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = meno.isNotBlank(),
                onClick = {
                    val csv = obmedzenia.joinToString(",") { it.kluc }
                    uloz(
                        (povodny ?: UcastnikEntity(meno = "", farba = farba)).copy(
                            meno = meno.trim(),
                            farba = farba,
                            obmedzenia = csv,
                            aktivny = aktivny,
                        )
                    )
                    zavri()
                },
            ) { Text(if (povodny == null) "Pridať" else "Uložiť") }
        },
        dismissButton = {
            TextButton(onClick = zavri) { Text("Zrušiť") }
        },
    )
}

@Composable
private fun IkonaRiadku(ikona: ImageVector, farba: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(farba.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ikona, contentDescription = null, tint = farba, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Sipka() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Oddelovac() {
    HorizontalDivider(
        Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

/** Pridanie aj úprava dieťaťa: meno, vek, farba a pri úprave odstránenie (s potvrdením). */
@Composable
private fun DialogDieta(
    povodne: DietaEntity?,
    predvolenaFarba: Color,
    zavri: () -> Unit,
    uloz: (DietaEntity) -> Unit,
    odstran: (() -> Unit)?,
) {
    // rememberSaveable - rozpísané meno prežije otočenie displeja
    var meno by rememberSaveable { mutableStateOf(povodne?.meno.orEmpty()) }
    var vek by rememberSaveable { mutableStateOf(povodne?.vek?.toString().orEmpty()) }
    var farba by rememberSaveable { mutableStateOf(povodne?.farba?.takeIf { it != 0 } ?: predvolenaFarba.toArgb()) }
    var potvrdOdstranenie by rememberSaveable { mutableStateOf(false) }
    val haptika = rememberHaptika()
    val platne = meno.isNotBlank() && vek.toIntOrNull() != null

    if (potvrdOdstranenie && odstran != null && povodne != null) {
        AlertDialog(
            onDismissRequest = { potvrdOdstranenie = false },
            title = { Text("Odstrániť ${povodne.meno}?") },
            text = {
                Text("Zmizne z výberu pri tréningu. Objavené zvieratá a história cvičenia ostanú v denníku.")
            },
            confirmButton = {
                TextButton(onClick = {
                    odstran()
                    zavri()
                }) { Text("Odstrániť") }
            },
            dismissButton = { TextButton(onClick = { potvrdOdstranenie = false }) { Text("Nechať") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = zavri,
        title = { Text(if (povodne == null) "Nové dieťa" else "Upraviť: ${povodne.meno}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = meno,
                    onValueChange = { meno = it.take(24) },
                    label = { Text("Meno") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = vek,
                    onValueChange = { vek = it.filter { z -> z.isDigit() }.take(2) },
                    label = { Text("Vek (roky)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
                Text("Farba", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETA_DETI_NASTAVENIA.forEach { f ->
                        val vybrana = f.toArgb() == farba
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(f)
                                .border(3.dp, if (vybrana) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .selectable(selected = vybrana, role = Role.RadioButton) {
                                    haptika.klik()
                                    farba = f.toArgb()
                                },
                        )
                    }
                }
                if (odstran != null) {
                    TextButton(onClick = { potvrdOdstranenie = true }) {
                        Text("Odstrániť dieťa", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = platne,
                onClick = {
                    val v = vek.toIntOrNull()
                    if (meno.isNotBlank() && v != null) {
                        uloz(
                            (povodne ?: DietaEntity(meno = "", vek = 0, farba = 0))
                                .copy(meno = meno.trim(), vek = v, farba = farba)
                        )
                        zavri()
                    }
                },
            ) { Text(if (povodne == null) "Pridať" else "Uložiť") }
        },
        dismissButton = {
            TextButton(onClick = zavri) { Text("Zrušiť") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogCasu(minuty: Int, zavri: () -> Unit, potvrd: (Int) -> Unit) {
    val stav = rememberTimePickerState(
        initialHour = if (minuty >= 0) minuty / 60 else 21,
        initialMinute = if (minuty >= 0) minuty % 60 else 0,
        is24Hour = true,
    )
    TimePickerDialog(
        onDismissRequest = zavri,
        title = { Text("Čas kontroly") },
        confirmButton = {
            TextButton(onClick = {
                potvrd(stav.hour * 60 + stav.minute)
                zavri()
            }) { Text("Nastaviť") }
        },
        dismissButton = {
            TextButton(onClick = zavri) { Text("Zrušiť") }
        },
    ) {
        TimePicker(state = stav)
    }
}

// ------------------------------------------------------------------ pomocníci

private fun hhmm(minuty: Int): String = "%02d:%02d".format(minuty / 60, minuty % 60)

private fun formatujKg(kg: Double): String = kg.toString().removeSuffix(".0").replace('.', ',')

/** Rovnaká paleta a pravidlo ako v Zbierke: farba z DB, inak podľa poradia pridania. */
private val PALETA_DETI_NASTAVENIA = listOf(
    Color(0xFFF2B138),
    Color(0xFF4FA3D9),
    Color(0xFFE57AA6),
    Color(0xFF6E9B5A),
    Color(0xFF9C7AD9),
    Color(0xFFE0703C),
)

private fun farbaDietataNastavenia(dieta: DietaEntity, poradie: Int): Color =
    if (dieta.farba != 0) Color(dieta.farba) else PALETA_DETI_NASTAVENIA[poradie % PALETA_DETI_NASTAVENIA.size]

@Composable
private fun citatelnaFarbaNastaveni(farba: Color): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) lerp(farba, Color.White, 0.22f)
    else lerp(farba, Color.Black, 0.32f)
