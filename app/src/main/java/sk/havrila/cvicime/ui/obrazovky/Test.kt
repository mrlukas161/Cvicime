package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.data.model.Skupina
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.komponenty.Pozy
import sk.havrila.cvicime.ui.komponenty.SegmentovyPruh
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.pruzneStlacenie
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk

data class RebrikSCvikmi(val rebrik: RebrikEntity, val cviky: List<CvikEntity>)

class TestModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebriky = MutableStateFlow<List<RebrikSCvikmi>>(emptyList())
    val rebriky = _rebriky.asStateFlow()

    /** Už nastavené úrovne - obrazovka nimi predvyplní výber. */
    private val _urovne = MutableStateFlow<Map<String, Int>>(emptyMap())
    val urovne = _urovne.asStateFlow()

    // Katalóg sa načítava na pozadí pri štarte appky, takže sa naň čaká tokom,
    // nie jednorazovým čítaním - inak by test pri prvom spustení ostal prázdny.
    init {
        viewModelScope.launch {
            val zaujimave = setOf(
                Skupina.TLAK, Skupina.TAH, Skupina.NOHY, Skupina.STRED, Skupina.ZRUCNOST,
            )
            repo.rebrikyTok().collectLatest { vsetky ->
                _rebriky.value = vsetky
                    .filter { Skupina.z(it.skupina) in zaujimave }
                    .map { r -> RebrikSCvikmi(r, repo.cvikyRebrika(r.id).filter { !it.jeDetsky }) }
                    .filter { it.cviky.isNotEmpty() }
                _urovne.value = repo.denik.urovne().associate { it.rebrikId to it.uroven }
            }
        }
    }

    fun uloz(vyber: Map<String, Int>, hotovo: () -> Unit) {
        viewModelScope.launch {
            vyber.forEach { (rebrikId, uroven) ->
                repo.nastavUroven(rebrikId, uroven)
                repo.cvikyRebrika(rebrikId).firstOrNull { it.uroven == uroven }?.let { cvik ->
                    repo.zapisTest(cvik, cvik.cielMin, jeVstupny = true)
                }
            }
            val profil = repo.profil()
            repo.ulozProfil(profil.copy(vstupnyTestHotovy = true))
            hotovo()
        }
    }
}

/**
 * Vstupný test nezisťuje maximálky vzorcom - pýta sa priamo, ktorý najťažší stupeň
 * používateľ zvládne. Presne tak to robí tréner a nevyžaduje si to prepočty,
 * ktoré by aj tak boli vymyslené (koeficienty obtiažnosti cvikov nemáme z čoho vziať).
 *
 * Jeden rebrík = jedna karta. Nevybraný stupeň (0) sa neukladá - rebrík ostane, kde bol
 * (pri prvom teste na začiatku), presne ako doteraz.
 */
@Composable
fun TestObrazovka(hotovo: () -> Unit, model: TestModel = viewModel()) {
    val rebriky by model.rebriky.collectAsStateWithLifecycle()
    val urovne by model.urovne.collectAsStateWithLifecycle()
    val vyber = remember { mutableStateMapOf<String, Int>() }
    var predvyplnene by remember { mutableStateOf(false) }
    var krok by remember { mutableIntStateOf(0) }
    var uklada by remember { mutableStateOf(false) }

    LaunchedEffect(urovne) {
        if (predvyplnene || urovne.isEmpty()) return@LaunchedEffect
        vyber.putAll(urovne)
        predvyplnene = true
    }

    // Poistka proti dvojitému ťuknutiu: hotovo() robí návrat späť a dvakrát by vrátil o obrazovku viac.
    val uloz: () -> Unit = {
        if (!uklada) {
            uklada = true
            model.uloz(vyber.toMap(), hotovo)
        }
    }

    val pocet = rebriky.size
    val aktualny = krok.coerceIn(0, (pocet - 1).coerceAtLeast(0))
    val posledny = pocet == 0 || aktualny == pocet - 1

    // Systémové "späť" vracia o cvik, z prvého opúšťa test ako doteraz.
    BackHandler(enabled = aktualny > 0) { krok = aktualny - 1 }

    Column(Modifier.fillMaxSize()) {
        // ---- hlavička a pás postupu testom
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Vstupný test", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                // Uložiť sa dá kedykoľvek - zvyšné rebríky ostanú, kde sú. V poslednom kroku je to akcia dole.
                if (!posledny) {
                    TextButton(onClick = uloz, enabled = !uklada) { Text("Uložiť") }
                }
            }
            Text(
                "Nastav najťažší stupeň, ktorý zvládneš aspoň trikrát s poriadnou technikou. " +
                    "Ani prvý? Nechaj nulu — začneme tam.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            if (pocet > 0) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Cvik ${aktualny + 1} / $pocet",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "nastavené ${vyber.keys.count { k -> rebriky.any { it.rebrik.id == k } }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                PasPlnenia(
                    postup = (aktualny + 1f) / pocet,
                    modifier = Modifier.padding(end = 12.dp),
                    vyska = 6.dp,
                )
            }
        }

        if (pocet == 0) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Načítavam knižnicu cvikov…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            AnimatedContent(
                targetState = aktualny,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val smer = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { w -> smer * w / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(250)) { w -> -smer * w / 3 } + fadeOut(tween(200)))
                },
                label = "krokTestu",
            ) { k ->
                val polozka = rebriky.getOrNull(k) ?: return@AnimatedContent
                val id = polozka.rebrik.id
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    KartaTestu(
                        polozka = polozka,
                        uroven = vyber[id] ?: 0,
                        naZmenu = { u -> if (u <= 0) vyber.remove(id) else vyber[id] = u },
                    )
                    RebrikStupnov(
                        polozka = polozka,
                        uroven = vyber[id] ?: 0,
                        naZmenu = { u -> if (u <= 0) vyber.remove(id) else vyber[id] = u },
                    )
                }
            }
        }

        // ---- spodná lišta
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = aktualny > 0) {
                OutlinedButton(
                    onClick = { if (aktualny > 0) krok = aktualny - 1 },
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
                text = if (posledny) "Hotovo" else "Ďalej",
                onClick = { if (posledny) uloz() else krok = aktualny + 1 },
                modifier = Modifier.weight(1f),
                ikona = if (posledny) Icons.Filled.Check else null,
                enabled = !uklada,
            )
        }
    }
}

/** Karta cviku: postavička, názov rebríka a veľký stepper stupňa s haptikou. */
@Composable
private fun KartaTestu(polozka: RebrikSCvikmi, uroven: Int, naZmenu: (Int) -> Unit) {
    val haptika = rememberHaptika()
    val rebrik = polozka.rebrik
    val stupne = polozka.cviky.map { it.uroven }
    val poloha = stupne.indexOf(uroven) // -1 = nevybraté
    val farba = farbaRebrika(rebrik)
    val citatelnaFarba = citatelnaVTeste(farba)

    fun nastav(p: Int) {
        val nova = p.coerceIn(-1, stupne.lastIndex)
        if (nova == poloha) return
        haptika.tik()
        naZmenu(if (nova < 0) 0 else stupne[nova])
    }

    TonovanaKarta(farba = farba) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kapsula(text = Skupina.z(rebrik.skupina).nazov, farba = citatelnaFarba)
            Spacer(Modifier.weight(1f))
            Text(
                "${stupne.size} ${sklonuj(stupne.size, "stupeň", "stupne", "stupňov")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).clip(CircleShape).background(farba.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (Pozy.pre(rebrik.id) != null) {
                    Postavicka(
                        rebrikId = rebrik.id,
                        modifier = Modifier.size(124.dp),
                        farba = MaterialTheme.colorScheme.onSurface,
                        farbaAkcent = farba,
                    )
                } else {
                    Icon(
                        Icons.Filled.SportsGymnastics,
                        contentDescription = null,
                        tint = citatelnaFarba,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            rebrik.nazov,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            KrokoveTlacidlo(
                ikona = Icons.Filled.Remove,
                popis = "Nižší stupeň",
                enabled = poloha >= 0,
                farba = farba,
                onClick = { nastav(poloha - 1) },
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimovaneCislo(
                    hodnota = if (poloha >= 0) uroven else 0,
                    style = MaterialTheme.typography.displayLarge,
                    color = if (poloha >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (poloha >= 0) "stupeň z ${stupne.lastOrNull() ?: 0}" else "nevybraté",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            KrokoveTlacidlo(
                ikona = Icons.Filled.Add,
                popis = "Vyšší stupeň",
                enabled = poloha < stupne.lastIndex,
                farba = farba,
                onClick = { nastav(poloha + 1) },
            )
        }
        Spacer(Modifier.height(16.dp))
        SegmentovyPruh(
            farby = List(stupne.size) { farba },
            aktualny = poloha + 1,
            vyska = 8.dp,
        )
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = poloha,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                } else {
                    (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                }
            },
            label = "nazovStupna",
        ) { p ->
            Text(
                if (p >= 0) polozka.cviky[p].nazov else "Ani prvý stupeň zatiaľ nie — to je v poriadku.",
                style = if (p >= 0) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (p >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Celý rebrík pod stepperom - ťuk na stupeň ho vyberie, ťuk na vybraný ho zruší (ako doteraz). */
@Composable
private fun RebrikStupnov(polozka: RebrikSCvikmi, uroven: Int, naZmenu: (Int) -> Unit) {
    val haptika = rememberHaptika()
    val farba = farbaRebrika(polozka.rebrik)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NadpisSekcie("Všetky stupne")
        TonovanaKarta(vnutorneOdsadenie = 8.dp) {
            polozka.cviky.forEach { cvik ->
                val vybrany = cvik.uroven == uroven
                val zvladnuty = uroven > 0 && cvik.uroven < uroven
                val pozadie by animateColorAsState(
                    if (vybrany) farba.copy(alpha = 0.16f) else Color.Transparent,
                    label = "stupenPozadie",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(pozadie)
                        .clickable(role = Role.RadioButton) {
                            haptika.tik()
                            naZmenu(if (vybrany) 0 else cvik.uroven)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val plny = vybrany || zvladnuty
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (plny) farba else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${cvik.uroven}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
                            color = if (plny) textNaFarbe(farba) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        cvik.nazov,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (vybrany) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (vybrany || zvladnuty) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (vybrany) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "vybraný",
                            tint = citatelnaVTeste(farba),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Veľké okrúhle tlačidlo stepperu - ovládateľné aj spotenou rukou. */
@Composable
private fun KrokoveTlacidlo(
    ikona: ImageVector,
    popis: String,
    enabled: Boolean,
    farba: Color,
    onClick: () -> Unit,
) {
    val interakcia = remember { MutableInteractionSource() }
    val pozadie by animateColorAsState(
        if (enabled) farba.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        label = "krokoveTlacidlo",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = pozadie,
        contentColor = if (enabled) citatelnaVTeste(farba) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        modifier = Modifier.size(72.dp).pruzneStlacenie(interakcia),
        interactionSource = interakcia,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(ikona, contentDescription = popis, modifier = Modifier.size(36.dp))
        }
    }
}

// ------------------------------------------------------------------ farby

/** Rebríky zručností majú farbu bloku Zručnosť, ostatné farbu bloku Sila. */
private fun farbaRebrika(r: RebrikEntity): Color =
    FarbyBlokov.pre(if (Skupina.z(r.skupina) == Skupina.ZRUCNOST) Blok.SKILL else Blok.SILA)

private fun textNaFarbe(farba: Color): Color = if (farba.luminance() > 0.18f) Color(0xFF1C1917) else Color.White

/** Text vo farbe bloku: vo svetlom režime stmavený, v tmavom mierne zosvetlený (kontrast). */
@Composable
private fun citatelnaVTeste(farba: Color): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) lerp(farba, Color.Black, 0.3f)
    else lerp(farba, Color.White, 0.2f)
