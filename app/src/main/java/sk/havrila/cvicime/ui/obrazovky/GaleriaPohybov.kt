package sk.havrila.cvicime.ui.obrazovky

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.PostavickaVzoru
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.pohyb.Pohlad
import sk.havrila.cvicime.ui.komponenty.pohyb.Pohyby
import sk.havrila.cvicime.ui.komponenty.pohyb.Vzor
import sk.havrila.cvicime.ui.komponenty.sklonuj

/**
 * Galéria pohybov: všetky vzory animácií naraz, aby sa dalo okom skontrolovať,
 * či pohyb sedí s popisom cviku. Ťuknutím sa vzor zväčší a ukáže, ktoré cviky ho používajú.
 * Otvára sa z Knižnice (vlastný stav, bez navigačnej cesty).
 */
@Composable
fun GaleriaPohybov(
    cviky: Map<String, List<CvikEntity>>,
    rebriky: List<RebrikEntity>,
    spat: () -> Unit,
) {
    BackHandler(onBack = spat)
    val context = LocalContext.current
    val katalog = remember { Pohyby.katalog(context) }
    val podlaPohybu = remember(cviky) {
        cviky.values.flatten().filter { it.pohyb != null }.groupBy { it.pohyb!! }
    }
    val nazvyRebrikov = remember(rebriky) { rebriky.associate { it.id to it.nazov } }
    var vybrany by rememberSaveable { mutableStateOf<String?>(null) }

    val farba = MaterialTheme.colorScheme.onSurface
    val akcent = MaterialTheme.colorScheme.primary

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hlavicka", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                IconButton(onClick = spat) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
                }
                Text(
                    "Galéria pohybov",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${katalog.vzory.size} ${sklonuj(katalog.vzory.size, "pohyb", "pohyby", "pohybov")} " +
                        "pre všetky cviky. Ťukni na pohyb a uvidíš ho zväčšený aj s cvikmi, ktoré ho používajú.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        items(katalog.vzory, key = { it.id }) { vzor ->
            val pocet = podlaPohybu[vzor.id].orEmpty().size
            TonovanaKarta(onClick = { vybrany = vzor.id }, vnutorneOdsadenie = 12.dp) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)
                        .background(akcent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    PostavickaVzoru(vzor, Modifier.fillMaxSize().padding(6.dp), farba, akcent)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    vzor.nazov,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (pocet == 0) "bez cviku" else "$pocet ${sklonuj(pocet, "cvik", "cviky", "cvikov")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val detail = vybrany?.let { katalog[it] }
    if (detail != null) {
        DetailPohybu(
            vzor = detail,
            cviky = podlaPohybu[detail.id].orEmpty(),
            nazvyRebrikov = nazvyRebrikov,
            zavri = { vybrany = null },
        )
    }
}

@Composable
private fun DetailPohybu(
    vzor: Vzor,
    cviky: List<CvikEntity>,
    nazvyRebrikov: Map<String, String>,
    zavri: () -> Unit,
) {
    val farba = MaterialTheme.colorScheme.onSurface
    val akcent = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = zavri, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    IconButton(onClick = zavri) {
                        Icon(Icons.Filled.Close, contentDescription = "Zavrieť")
                    }
                }
                Box(
                    Modifier.padding(horizontal = 20.dp).widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f)
                        .clip(CircleShape).background(akcent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    PostavickaVzoru(vzor, Modifier.fillMaxSize().padding(28.dp), farba, akcent)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    vzor.nazov,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Text(
                    vzor.id,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Kapsula(
                        when (vzor.pohlad) {
                            Pohlad.BOK -> "z boku"
                            Pohlad.PRED -> "spredu"
                            Pohlad.ZHORA -> "zhora"
                        },
                        akcent,
                    )
                    Kapsula(if (vzor.druh == "vydrz") "výdrž" else "opakovanie", akcent)
                    if (vzor.posun != 0.0) Kapsula(if (vzor.posun > 0) "pohyb vpred" else "pohyb vzad", akcent)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(16.dp))
                    NadpisSekcie(if (cviky.isEmpty()) "Žiadny cvik" else "Cviky s týmto pohybom")
                    Spacer(Modifier.height(8.dp))
                    cviky.forEachIndexed { i, c ->
                        if (i > 0) {
                            HorizontalDivider(
                                Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                        Text(c.nazov, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${nazvyRebrikov[c.rebrikId] ?: c.rebrikId} · stupeň ${c.uroven}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        c.cue.split("|").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                            Text(
                                "· $it",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
