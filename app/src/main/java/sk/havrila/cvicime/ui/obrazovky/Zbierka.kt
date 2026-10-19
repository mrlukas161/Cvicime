package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.DietaEntity
import sk.havrila.cvicime.deti.Zviera
import sk.havrila.cvicime.deti.Zvierata
import sk.havrila.cvicime.ui.komponenty.AnimovaneCislo
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
import sk.havrila.cvicime.ui.komponenty.sklonuj
import java.time.LocalDate

class ZbierkaModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _deti = MutableStateFlow<List<DietaEntity>>(emptyList())
    val deti = _deti.asStateFlow()

    /** dietaId -> (kľúč zvieraťa -> deň odomknutia) */
    private val _odomknute = MutableStateFlow<Map<Long, Map<String, Long>>>(emptyMap())
    val odomknute = _odomknute.asStateFlow()

    // Deti aj zvieratá tokom - zviera odomknuté v prehrávači sa tu ukáže bez reštartu obrazovky.
    init {
        viewModelScope.launch {
            combine(repo.detiTok(), repo.zvierataTok()) { deti, zvierata -> deti to zvierata }
                .collectLatest { (deti, zvierata) ->
                    _odomknute.value = deti.associate { d ->
                        d.id to zvierata
                            .filter { it.dietaId == d.id }
                            .groupBy { it.zvieraKluc }
                            .mapValues { (_, z) -> z.minOf { it.odomknuteEpochDen } }
                    }
                    _deti.value = deti
                }
        }
    }
}

/**
 * Zbierka zvierat - MAPA TOHO, ČO DIEŤA VIE, nie skóre.
 *
 * Zámerné rozhodnutia z rešerše:
 * - neodomknuté zvieratá sú len otázniky, nie siluety (identita sa neprezrádza vopred),
 * - počet objavených sa ukazuje len pre práve vybrané dieťa - bez percent, bez rebríčka
 *   a bez porovnania medzi deťmi; každé dieťa má vlastnú stránku,
 * - zviera raz odomknuté sa nikdy nestratí.
 */
@Composable
fun ZbierkaObrazovka(model: ZbierkaModel = viewModel()) {
    val deti by model.deti.collectAsStateWithLifecycle()
    val odomknute by model.odomknute.collectAsStateWithLifecycle()
    var vybraneDieta by rememberSaveable { mutableStateOf<Long?>(null) }
    var detail by remember { mutableStateOf<String?>(null) }
    val haptika = rememberHaptika()
    // Obrazovka nemá spodnú lištu - šípka späť cez systémový dispečer (signatúra ostáva).
    val spat = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val dieta = deti.firstOrNull { it.id == vybraneDieta } ?: deti.firstOrNull()
    val poradieFarieb = remember(deti) { deti.sortedBy { it.id }.mapIndexed { i, d -> d.id to i }.toMap() }
    val ma = odomknute[dieta?.id] ?: emptyMap()
    val farba = dieta?.let { farbaDietata(it, poradieFarieb[it.id] ?: 0) } ?: MaterialTheme.colorScheme.tertiary
    val akcent = citatelnaFarbaDietata(farba)
    val dnes = LocalDate.now().toEpochDay()

    // Animácia objavenia beží len raz na dieťa a zviera - pri posúvaní mriežky sa neopakuje.
    val uzZobrazene = remember(dieta?.id) { mutableSetOf<String>() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hlavicka", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                if (spat != null) {
                    IconButton(
                        onClick = { spat.onBackPressed() },
                        modifier = Modifier.padding(top = 8.dp).offset(x = (-12).dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
                    }
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                Text("Moje zvieratá", style = MaterialTheme.typography.headlineLarge)
                if (deti.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Každé zvieratko je pohyb, ktorý ste spolu vyskúšali. Ťukni naň a pripomeň si, aký.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (deti.isEmpty()) {
            item(key = "prazdne", span = { GridItemSpan(maxLineSpan) }) {
                PrazdnyStav(
                    titulok = "Zatiaľ tu nikto nie je",
                    text = "Najprv pridaj deti v Nastaveniach. Zvieratá sa odomykajú pri spoločnom cvičení.",
                    rebrikPostavicky = "detske-chodze",
                )
            }
        } else {
            if (deti.size > 1) {
                item(key = "deti", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        deti.forEach { d ->
                            val f = farbaDietata(d, poradieFarieb[d.id] ?: 0)
                            CipDietata(
                                meno = d.meno,
                                vybrany = dieta?.id == d.id,
                                farba = f,
                                akcent = citatelnaFarbaDietata(f),
                                onClick = {
                                    haptika.klik()
                                    vybraneDieta = d.id
                                    detail = null
                                },
                            )
                        }
                    }
                }
            }

            item(key = "pocitadlo", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    AnimovaneCislo(
                        ma.size,
                        style = MaterialTheme.typography.headlineLarge,
                        color = akcent,
                    )
                    Text(
                        " z ${Zvierata.vsetky.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        sklonuj(ma.size, "objavené zvieratko", "objavené zvieratká", "objavených zvieratiek"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }

            val vybrane = detail?.let { k -> Zvierata.podla(k) }?.takeIf { it.kluc in ma }
            if (vybrane != null) {
                item(key = "detail", span = { GridItemSpan(maxLineSpan) }) {
                    DetailZvierata(
                        zviera = vybrane,
                        den = ma[vybrane.kluc],
                        dnes = dnes,
                        farba = farba,
                        zavri = { detail = null },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            itemsIndexed(Zvierata.vsetky, key = { _, z -> z.kluc }) { i, z ->
                val den = ma[z.kluc]
                val kluc = "${dieta?.id}-${z.kluc}"
                DlazdicaZvierata(
                    zviera = z,
                    odomknute = den != null,
                    novinka = den == dnes,
                    vybrane = detail == z.kluc,
                    animovat = kluc !in uzZobrazene,
                    oneskorenieMs = (i * 28L).coerceAtMost(420L),
                    kluc = kluc,
                    farba = farba,
                    akcent = akcent,
                    poAnimacii = { uzZobrazene.add(kluc) },
                    onClick = if (den != null) {
                        {
                            haptika.klik()
                            detail = if (detail == z.kluc) null else z.kluc
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun CipDietata(meno: String, vybrany: Boolean, farba: Color, akcent: Color, onClick: () -> Unit) {
    FilterChip(
        selected = vybrany,
        onClick = onClick,
        label = { Text(meno) },
        leadingIcon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(farba)
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = farba.copy(alpha = 0.24f),
            selectedLabelColor = akcent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = vybrany,
            borderColor = farba.copy(alpha = 0.5f),
            selectedBorderColor = farba,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

@Composable
private fun DlazdicaZvierata(
    zviera: Zviera,
    odomknute: Boolean,
    novinka: Boolean,
    vybrane: Boolean,
    animovat: Boolean,
    oneskorenieMs: Long,
    kluc: String,
    farba: Color,
    akcent: Color,
    poAnimacii: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val mierka = remember(kluc) { Animatable(if (animovat) 0.4f else 1f) }
    val alfa = remember(kluc) { Animatable(if (animovat) 0f else 1f) }
    LaunchedEffect(kluc) {
        if (mierka.value < 1f) {
            delay(oneskorenieMs)
            launch { alfa.animateTo(1f, tween(200)) }
            mierka.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        }
        poAnimacii()
    }

    val tmavy = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val zaklad = MaterialTheme.colorScheme.surfaceContainerLow
    val pozadie = if (odomknute) farba.copy(alpha = if (tmavy) 0.24f else 0.18f).compositeOver(zaklad) else zaklad
    val tvar = MaterialTheme.shapes.medium

    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = mierka.value
                scaleY = mierka.value
                alpha = alfa.value
            }
            .clip(tvar)
            .background(pozadie)
            .border(
                width = if (vybrane) 2.dp else 1.dp,
                color = when {
                    vybrane -> akcent
                    odomknute -> farba.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                },
                shape = tvar,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (odomknute) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(zviera.emoji, fontSize = 40.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    zviera.meno,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            if (novinka) {
                // Dnes objavené - len tichá bodka, žiadny nápis "nové!".
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(akcent)
                )
            }
        } else {
            Icon(
                Icons.Filled.QuestionMark,
                contentDescription = "Zatiaľ neobjavené",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun DetailZvierata(
    zviera: Zviera,
    den: Long?,
    dnes: Long,
    farba: Color,
    zavri: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonovanaKarta(modifier = modifier, farba = farba, onClick = zavri, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(zviera.emoji, fontSize = 52.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(zviera.meno, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(zviera.veta, style = MaterialTheme.typography.bodyMedium)
                if (den != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (den) {
                            dnes -> "Objavené dnes"
                            dnes - 1 -> "Objavené včera"
                            else -> LocalDate.ofEpochDay(den).let { "Objavené ${it.dayOfMonth}. ${it.monthValue}. ${it.year}" }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Farba dieťaťa. Ak ju dieťa v databáze nemá (pridané v Nastaveniach má 0), pridelí sa
 * z palety podľa poradia pridania - stabilne, nemení sa so zoradením podľa veku.
 * Rovnaká paleta a pravidlo sú v Nastaveniach.
 */
private val PALETA_DETI = listOf(
    Color(0xFFF2B138), // slnečná
    Color(0xFF4FA3D9), // nebeská
    Color(0xFFE57AA6), // malinová
    Color(0xFF6E9B5A), // listová
    Color(0xFF9C7AD9), // levanduľová
    Color(0xFFE0703C), // plameň
)

private fun farbaDietata(dieta: DietaEntity, poradie: Int): Color =
    if (dieta.farba != 0) Color(dieta.farba) else PALETA_DETI[poradie % PALETA_DETI.size]

@Composable
private fun citatelnaFarbaDietata(farba: Color): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) lerp(farba, Color.White, 0.20f)
    else lerp(farba, Color.Black, 0.38f)
