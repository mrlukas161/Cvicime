package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.komponenty.Pozy
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.pruzneStlacenie
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.theme.Grotesk

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

    private val _prveSpustenie = MutableStateFlow(false)
    val prveSpustenie = _prveSpustenie.asStateFlow()

    init {
        viewModelScope.launch {
            repo.rebrikyTok().collectLatest { rebriky ->
                _zrucnosti.value = rebriky.filter { it.jeZrucnost }
            }
        }
        viewModelScope.launch {
            val profil = repo.profil()
            val ciele = repo.denik.aktivneCiele()
            // Pri PRVOM spustení sa ignoruje východzí profil ("ziadne,podlozka") a ponúknu sa
            // rozumné predvoľby. Inak by prvý používateľ videl zapnutú len podložku
            // a appka by mu vedela poskladať len cviky na zemi.
            _prveSpustenie.value = !profil.onboardingHotovy
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

    /** Ten istý výpočet, aký sa uloží do profilu - aby číslo na obrazovke sedelo s uloženým. */
    fun odporucaneBielkoviny(vahaKg: Double): Int = repo.odporucaneBielkoviny(vahaKg)

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

/** Kroky sprievodcu. Poradie = poradie obrazoviek. */
private enum class KrokOnboardingu(val titulok: String, val popis: String) {
    ZRUCNOSTI(
        "Čo sa chceš naučiť?",
        "Vyber najviac dve — tretia by ubrala čas obom. Poradie výberu je priorita.",
    ),
    PARTIE(
        "Na čom chceš pribrať?",
        "Voliteľné — môžeš vybrať viac partií alebo žiadnu.",
    ),
    NARADIE(
        "Čo máš doma?",
        "Bez zapnutého náradia ostanú len cviky na zemi — hrazdu a jednoručky si zapni.",
    ),
    CISLA(
        "Pár čísel",
        "Váha slúži len na výpočet denného cieľa bielkovín (1,6–2,2 g na kilo).",
    ),
    ZHRNUTIE(
        "Všetko sedí?",
        "Podľa tohto sa skladá každý tréning. Dá sa to kedykoľvek zmeniť.",
    ),
}

/** Najviac dve zručnosti - tretia by ubrala čas obom (ukladá sa aj tak len prvých dvoch). */
private const val MAX_ZRUCNOSTI = 2

@Composable
fun OnboardingObrazovka(hotovo: () -> Unit, model: OnboardingModel = viewModel()) {
    val zrucnosti by model.zrucnosti.collectAsStateWithLifecycle()
    val ulozene by model.ulozene.collectAsStateWithLifecycle()
    val prveSpustenie by model.prveSpustenie.collectAsStateWithLifecycle()

    val vybraneZrucnosti = remember { mutableListOf<String>().toMutableStateList() }
    val vybranePartie = remember { mutableListOf<Partia>().toMutableStateList() }
    val vybraneNaradie = remember { mutableListOf<Naradie>().toMutableStateList() }
    var vaha by remember { mutableStateOf("") }
    var vyska by remember { mutableStateOf("") }
    var predvyplnene by remember { mutableStateOf(false) }
    var krok by remember { mutableIntStateOf(0) }
    var uklada by remember { mutableStateOf(false) }
    val kroky = KrokOnboardingu.entries

    // Predvyplnenie uloženým stavom. Bez tohto sa obrazovka po opätovnom otvorení
    // tvárila prázdna a vyzeralo to, že sa ciele neuložili.
    LaunchedEffect(ulozene) {
        val u = ulozene ?: return@LaunchedEffect
        if (predvyplnene) return@LaunchedEffect
        vybraneZrucnosti.clear(); vybraneZrucnosti.addAll(u.zrucnosti)
        vybranePartie.clear(); vybranePartie.addAll(u.partie)
        vybraneNaradie.clear()
        val odporucane = listOf(
            Naradie.HRAZDA, Naradie.JEDNORUCKY, Naradie.PODLOZKA,
            Naradie.STENA, Naradie.SCHOD, Naradie.STOLICKA,
        )
        vybraneNaradie.addAll(if (prveSpustenie || u.naradie.isEmpty()) odporucane else u.naradie)
        vaha = u.vahaKg
        vyska = u.vyskaCm
        predvyplnene = true
    }

    // Uložiť sa dá až po predvyplnení - inak by prázdny výber zmazal uložené ciele.
    // Poistka proti dvojitému ťuknutiu: hotovo() robí návrat späť a dvakrát by vrátil o obrazovku viac.
    val uloz: () -> Unit = {
        if (predvyplnene && !uklada) {
            uklada = true
            model.uloz(
                vybraneZrucnosti = vybraneZrucnosti.take(MAX_ZRUCNOSTI),
                vybranePartie = vybranePartie.toList(),
                vybraneNaradie = vybraneNaradie.toList(),
                vahaKg = vaha.toDoubleOrNull() ?: 80.0,
                vyskaCm = vyska.toIntOrNull() ?: 180,
                hotovo = hotovo,
            )
        }
    }

    // Systémové "späť" vracia o krok, z prvého kroku opúšťa obrazovku ako doteraz.
    BackHandler(enabled = krok > 0) { krok-- }

    Column(
        Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.navigationBars)
            .imePadding()
    ) {
        // ---- ukazovateľ krokov
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    "Krok ${krok + 1} / ${kroky.size}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Pri úprave (nie pri prvom spustení) sa dá uložiť hneď - netreba prejsť všetky kroky.
                // V poslednom kroku je uloženie hlavná akcia dole, tu by bolo navyše.
                if (!prveSpustenie && predvyplnene && krok != kroky.lastIndex) {
                    TextButton(onClick = uloz, enabled = !uklada) { Text("Uložiť") }
                }
            }
            PasPlnenia(
                postup = (krok + 1f) / kroky.size,
                modifier = Modifier.padding(end = 12.dp),
                vyska = 6.dp,
            )
        }

        // ---- obsah kroku
        AnimatedContent(
            targetState = krok,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val smer = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(300)) { w -> smer * w / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(250)) { w -> -smer * w / 3 } + fadeOut(tween(200)))
            },
            label = "krokOnboardingu",
        ) { k ->
            val aktualny = kroky[k]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(aktualny.titulok, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    aktualny.popis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                when (aktualny) {
                    KrokOnboardingu.ZRUCNOSTI -> KrokZrucnosti(zrucnosti, vybraneZrucnosti)
                    KrokOnboardingu.PARTIE -> MriezkaVyberu(
                        polozky = Partia.entries,
                        vybrane = vybranePartie,
                        nazov = { it.nazov },
                    )
                    KrokOnboardingu.NARADIE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Naradie.entries.filter { it != Naradie.ZIADNE }.forEach { n ->
                            VyberovaKarta(
                                nazov = n.nazov,
                                vybrana = n in vybraneNaradie,
                                naKlik = { prepni(vybraneNaradie, n) },
                                ikona = ikonaNaradia(n),
                            )
                        }
                    }
                    KrokOnboardingu.CISLA -> KrokCisla(
                        vaha = vaha,
                        naVahu = { vaha = it.replace(',', '.').filter { z -> z.isDigit() || z == '.' } },
                        vyska = vyska,
                        naVysku = { vyska = it.filter { z -> z.isDigit() } },
                        bielkoviny = vaha.toDoubleOrNull()?.let { model.odporucaneBielkoviny(it) },
                    )
                    KrokOnboardingu.ZHRNUTIE -> KrokZhrnutie(
                        zrucnosti = vybraneZrucnosti.take(MAX_ZRUCNOSTI).map { id ->
                            zrucnosti.firstOrNull { it.id == id }?.nazov ?: id
                        },
                        partie = vybranePartie.map { it.nazov },
                        naradie = vybraneNaradie.map { it.nazov },
                        vahaKg = vaha.toDoubleOrNull() ?: 80.0,
                        vyskaCm = vyska.toIntOrNull() ?: 180,
                        bielkoviny = model.odporucaneBielkoviny(vaha.toDoubleOrNull() ?: 80.0),
                        naKrok = { krok = it.ordinal },
                    )
                }
            }
        }

        // ---- spodná lišta Späť / Ďalej
        val posledny = krok == kroky.lastIndex
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = krok > 0) {
                OutlinedButton(
                    onClick = { if (krok > 0) krok-- },
                    modifier = Modifier.heightIn(min = 64.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Späť", style = MaterialTheme.typography.labelLarge)
                }
            }
            VelkeTlacidlo(
                // Krátko - vedľa tlačidla Späť sa dlhší text zalomil do dvoch riadkov.
                text = if (posledny) "Uložiť" else "Ďalej",
                onClick = { if (posledny) uloz() else krok++ },
                modifier = Modifier.weight(1f),
                ikona = if (posledny) Icons.Filled.Check else null,
                enabled = if (posledny) predvyplnene && !uklada else true,
            )
        }
    }
}

// ------------------------------------------------------------------ kroky

@Composable
private fun KrokZrucnosti(zrucnosti: List<RebrikEntity>, vybrane: SnapshotStateList<String>) {
    if (zrucnosti.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val haptika = rememberHaptika()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        zrucnosti.chunked(2).forEach { riadok ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                riadok.forEach { r ->
                    val poradie = vybrane.indexOf(r.id).takeIf { it >= 0 }?.plus(1)
                    KartaZrucnosti(
                        rebrik = r,
                        poradie = poradie,
                        povolena = poradie != null || vybrane.size < MAX_ZRUCNOSTI,
                        naKlik = {
                            haptika.klik()
                            if (r.id in vybrane) vybrane.remove(r.id)
                            else if (vybrane.size < MAX_ZRUCNOSTI) vybrane.add(r.id)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (riadok.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Vysoká karta zručnosti s postavičkou. Číslo v rohu = priorita podľa poradia výberu. */
@Composable
private fun KartaZrucnosti(
    rebrik: RebrikEntity,
    poradie: Int?,
    povolena: Boolean,
    naKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vybrana = poradie != null
    val farba = MaterialTheme.colorScheme.primary
    val zaklad = MaterialTheme.colorScheme.surfaceContainerLow
    val pozadie by animateColorAsState(
        if (vybrana) farba.copy(alpha = 0.12f).compositeOver(zaklad) else zaklad,
        label = "kartaZrucnosti",
    )
    val okraj by animateColorAsState(if (vybrana) farba else Color.Transparent, label = "okrajZrucnosti")
    val interakcia = remember { MutableInteractionSource() }
    Surface(
        checked = vybrana,
        onCheckedChange = { naKlik() },
        modifier = modifier.alpha(if (povolena) 1f else 0.45f).pruzneStlacenie(interakcia),
        enabled = povolena,
        shape = MaterialTheme.shapes.large,
        color = pozadie,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, okraj),
        interactionSource = interakcia,
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(farba.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (Pozy.pre(rebrik.id) != null) {
                        Postavicka(
                            rebrikId = rebrik.id,
                            modifier = Modifier.size(78.dp),
                            farba = MaterialTheme.colorScheme.onSurface,
                            farbaAkcent = farba,
                        )
                    } else {
                        Icon(
                            Icons.Filled.SportsGymnastics,
                            contentDescription = null,
                            tint = farba,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    rebrik.nazov,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                Zaskrtnutie(vybrana = vybrana, odznak = poradie?.toString())
            }
        }
    }
}

/** Dvojstĺpcová mriežka výberových kariet (partie). */
@Composable
private fun <T> MriezkaVyberu(polozky: List<T>, vybrane: SnapshotStateList<T>, nazov: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        polozky.chunked(2).forEach { riadok ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                riadok.forEach { p ->
                    VyberovaKarta(
                        nazov = nazov(p),
                        vybrana = p in vybrane,
                        naKlik = { prepni(vybrane, p) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (riadok.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Výberová karta: (ikona) + názov + zaškrtnutie. Celá plocha je dotyková. */
@Composable
private fun VyberovaKarta(
    nazov: String,
    vybrana: Boolean,
    naKlik: () -> Unit,
    modifier: Modifier = Modifier,
    ikona: ImageVector? = null,
) {
    val haptika = rememberHaptika()
    val farba = MaterialTheme.colorScheme.primary
    val zaklad = MaterialTheme.colorScheme.surfaceContainerLow
    val pozadie by animateColorAsState(
        if (vybrana) farba.copy(alpha = 0.12f).compositeOver(zaklad) else zaklad,
        label = "vyberovaKarta",
    )
    val okraj by animateColorAsState(if (vybrana) farba else Color.Transparent, label = "okrajKarty")
    val interakcia = remember { MutableInteractionSource() }
    Surface(
        checked = vybrana,
        onCheckedChange = { haptika.klik(); naKlik() },
        modifier = modifier.fillMaxWidth().pruzneStlacenie(interakcia),
        shape = MaterialTheme.shapes.medium,
        color = pozadie,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, okraj),
        interactionSource = interakcia,
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ikona != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(farba.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ikona, contentDescription = null, tint = farba, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                nazov,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (vybrana) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Zaskrtnutie(vybrana = vybrana)
        }
    }
}

/** Kruh, ktorý sa po výbere pružne vyplní - fajka alebo číslo poradia. */
@Composable
private fun Zaskrtnutie(vybrana: Boolean, odznak: String? = null) {
    val farba = MaterialTheme.colorScheme.primary
    val pozadie by animateColorAsState(if (vybrana) farba else Color.Transparent, label = "zaskrtnutie")
    val okraj by animateColorAsState(
        if (vybrana) farba else MaterialTheme.colorScheme.outline,
        label = "zaskrtnutieOkraj",
    )
    val mierka by animateFloatAsState(
        targetValue = if (vybrana) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "zaskrtnutieMierka",
    )
    Box(
        Modifier
            .size(28.dp)
            .graphicsLayer { scaleX = mierka; scaleY = mierka }
            .clip(CircleShape)
            .background(pozadie)
            .border(2.dp, okraj, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = vybrana, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            if (odznak != null) {
                Text(
                    odznak,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun KrokCisla(
    vaha: String,
    naVahu: (String) -> Unit,
    vyska: String,
    naVysku: (String) -> Unit,
    bielkoviny: Int?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CisloPole(
            hodnota = vaha,
            naZmenu = naVahu,
            popis = "Váha",
            jednotka = "kg",
            ikona = Icons.Filled.MonitorWeight,
            klavesnica = KeyboardType.Decimal,
            akcia = ImeAction.Next,
        )
        CisloPole(
            hodnota = vyska,
            naZmenu = naVysku,
            popis = "Výška",
            jednotka = "cm",
            ikona = Icons.Filled.Height,
            klavesnica = KeyboardType.Number,
            akcia = ImeAction.Done,
        )
        AnimatedVisibility(visible = bielkoviny != null) {
            val farba = MaterialTheme.colorScheme.secondary
            TonovanaKarta(farba = farba, modifier = Modifier.padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(farba.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Egg, contentDescription = null, tint = farba, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Denný cieľ bielkovín",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "~",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AnimovaneCislo(
                                hodnota = bielkoviny ?: 0,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                " g",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Veľké číselné pole - číslo písmom Grotesk, jednotka ako prípona. */
@Composable
private fun CisloPole(
    hodnota: String,
    naZmenu: (String) -> Unit,
    popis: String,
    jednotka: String,
    ikona: ImageVector,
    klavesnica: KeyboardType,
    akcia: ImeAction,
) {
    OutlinedTextField(
        value = hodnota,
        onValueChange = naZmenu,
        label = { Text(popis) },
        leadingIcon = { Icon(ikona, contentDescription = null) },
        suffix = { Text(jednotka, style = MaterialTheme.typography.titleMedium) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium,
        keyboardOptions = KeyboardOptions(keyboardType = klavesnica, imeAction = akcia),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun KrokZhrnutie(
    zrucnosti: List<String>,
    partie: List<String>,
    naradie: List<String>,
    vahaKg: Double,
    vyskaCm: Int,
    bielkoviny: Int,
    naKrok: (KrokOnboardingu) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RiadokZhrnutia(
            popis = "Zručnosti",
            hodnota = if (zrucnosti.isEmpty()) "žiadna" else zrucnosti.mapIndexed { i, z -> "${i + 1}. $z" }.joinToString("   "),
            naUpravu = { naKrok(KrokOnboardingu.ZRUCNOSTI) },
        )
        RiadokZhrnutia(
            popis = "Partie",
            hodnota = if (partie.isEmpty()) "žiadna" else partie.joinToString(", "),
            naUpravu = { naKrok(KrokOnboardingu.PARTIE) },
        )
        RiadokZhrnutia(
            popis = "Náradie",
            hodnota = if (naradie.isEmpty()) "žiadne — len cviky na zemi" else naradie.joinToString(", "),
            naUpravu = { naKrok(KrokOnboardingu.NARADIE) },
        )
        RiadokZhrnutia(
            popis = "Telo a bielkoviny",
            hodnota = "${vahaKg.toString().removeSuffix(".0")} kg · $vyskaCm cm · ~$bielkoviny g bielkovín denne",
            naUpravu = { naKrok(KrokOnboardingu.CISLA) },
        )
    }
}

@Composable
private fun RiadokZhrnutia(popis: String, hodnota: String, naUpravu: () -> Unit) {
    TonovanaKarta(onClick = naUpravu, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    popis.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Grotesk, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(hodnota, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Upraviť",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ pomocné

private fun ikonaNaradia(n: Naradie): ImageVector = when (n) {
    Naradie.HRAZDA -> Icons.Filled.SportsGymnastics
    Naradie.PEVNA_HRAZDA -> Icons.Filled.Park
    Naradie.BRADLA -> Icons.Filled.DragHandle
    Naradie.PARALETY -> Icons.Filled.HorizontalRule
    Naradie.KRUHY -> Icons.Filled.TripOrigin
    Naradie.JEDNORUCKY -> Icons.Filled.FitnessCenter
    Naradie.PODLOZKA -> Icons.Filled.SelfImprovement
    Naradie.STOLICKA -> Icons.Filled.Chair
    Naradie.SCHOD -> Icons.Filled.Stairs
    Naradie.STENA -> Icons.Filled.Fence
    Naradie.ZIADNE -> Icons.Filled.Check
}

private fun <T> prepni(zoznam: SnapshotStateList<T>, hodnota: T) {
    if (hodnota in zoznam) zoznam.remove(hodnota) else zoznam.add(hodnota)
}
