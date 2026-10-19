package sk.havrila.cvicime.ui.obrazovky

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.StackedBarChart
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.havrila.cvicime.teoria.Clanok
import sk.havrila.cvicime.teoria.Teoria
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import kotlin.math.ceil

/**
 * Teoretická časť: prečo appka robí to, čo robí.
 * Každý článok má štítok, či ide o dokázané zistenie alebo o prax trénerov -
 * appka nesmie tvrdiť viac, než vie.
 */
@Composable
fun TeoriaObrazovka(naClanok: (String) -> Unit) {
    // Obrazovka nemá spodnú lištu (otvára sa z Nastavení) - šípka späť cez systémový
    // dispečer, aby sa nemenila verejná signatúra.
    val spat = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "hlavicka") {
            Column {
                if (spat != null) {
                    IconButton(
                        onClick = { spat.onBackPressed() },
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
                    }
                }
                Column(
                    Modifier.padding(
                        start = 20.dp, end = 20.dp,
                        top = if (spat != null) 4.dp else 20.dp,
                        bottom = 12.dp,
                    )
                ) {
                    Text("Prečo to tak je", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Krátke vysvetlenia pravidiel, na ktorých tréningy stoja. Pri každom je poctivo " +
                            "napísané, či ide o vedecky dokázanú vec, alebo o osvedčenú prax bez štúdií.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(Teoria.clanky, key = { it.id }) { c ->
            KartaClanku(
                clanok = c,
                onClick = { naClanok(c.id) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun KartaClanku(clanok: Clanok, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val farba = farbaZakladu(clanok.zaklad)
    TonovanaKarta(modifier = modifier, onClick = onClick, vnutorneOdsadenie = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DlazdicaClanku(ikonaClanku(clanok.id), farba, velkost = 40)
            Spacer(Modifier.width(12.dp))
            Kapsula(nazovZakladu(clanok.zaklad), farba)
        }
        Spacer(Modifier.height(12.dp))
        Text(clanok.titulok, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            clanok.perex,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        CasCitania(clanok)
    }
}

@Composable
fun ClanokObrazovka(clanokId: String, spat: () -> Unit) {
    val c = Teoria.podla(clanokId)
    if (c == null) {
        Column(Modifier.fillMaxSize()) {
            IconButton(onClick = spat, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
            }
            PrazdnyStav(
                titulok = "Článok sa nenašiel",
                text = "Možno bol v novej verzii premenovaný. Vráť sa do zoznamu.",
                rebrikPostavicky = null,
            )
        }
        return
    }
    val farba = farbaZakladu(c.zaklad)
    val textClanku = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = spat, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
        }
        // Príjemná šírka riadku: na tablete sa text nerozťahuje cez celú obrazovku.
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 620.dp)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DlazdicaClanku(ikonaClanku(c.id), farba, velkost = 56)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Kapsula(nazovZakladu(c.zaklad), farba, ikona = ikonaZakladu(c.zaklad))
                    CasCitania(c)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(c.titulok, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                c.perex,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(farba)
            )
            Spacer(Modifier.height(20.dp))

            // Odseky oddelené prázdnym riadkom, riadky s "• " ako odrážky s visiacim odsadením.
            c.telo.split("\n\n").forEachIndexed { i, odsek ->
                if (i > 0) Spacer(Modifier.height(16.dp))
                odsek.split("\n").filter { it.isNotBlank() }.forEach { riadok ->
                    if (riadok.startsWith("• ")) {
                        Row(Modifier.padding(top = 6.dp, start = 4.dp)) {
                            Box(
                                Modifier
                                    .padding(top = 11.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(farba)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(riadok.removePrefix("• "), style = textClanku)
                        }
                    } else {
                        Text(riadok, style = textClanku)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            TonovanaKarta(farba = farba, vnutorneOdsadenie = 16.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        ikonaZakladu(c.zaklad),
                        contentDescription = null,
                        tint = farba,
                        modifier = Modifier.padding(top = 2.dp).size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        vysvetlenieZakladu(c.zaklad),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DlazdicaClanku(ikona: ImageVector, farba: Color, velkost: Int) {
    Box(
        Modifier
            .size(velkost.dp)
            .clip(RoundedCornerShape((velkost * 0.3f).dp))
            .background(farba.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ikona, contentDescription = null, tint = farba, modifier = Modifier.size((velkost * 0.55f).dp))
    }
}

@Composable
private fun CasCitania(clanok: Clanok) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${minutyCitania(clanok)} min čítania",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Odhad času čítania: ~200 slov za minútu, aspoň minúta. */
private fun minutyCitania(clanok: Clanok): Int {
    val slov = (clanok.perex + " " + clanok.telo).split(Regex("\\s+")).count { it.isNotBlank() }
    return ceil(slov / 200.0).toInt().coerceAtLeast(1)
}

private fun nazovZakladu(zaklad: String): String = zaklad.replaceFirstChar { it.uppercase() }

/** Dokázané = zelenomodrá, prax trénerov = plameň, zmiešané = jantár. Čitateľné v oboch režimoch. */
@Composable
private fun farbaZakladu(zaklad: String): Color {
    val tmavy = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (zaklad) {
        Teoria.DOKAZANE -> MaterialTheme.colorScheme.secondary
        Teoria.PRAX -> MaterialTheme.colorScheme.primary
        else -> if (tmavy) Color(0xFFE2AE4C) else Color(0xFF8A5A00)
    }
}

private fun ikonaZakladu(zaklad: String): ImageVector = when (zaklad) {
    Teoria.DOKAZANE -> Icons.Outlined.Science
    Teoria.PRAX -> Icons.Outlined.Groups
    else -> Icons.Outlined.Balance
}

private fun vysvetlenieZakladu(zaklad: String): String = when (zaklad) {
    Teoria.DOKAZANE -> "Podložené štúdiami — opiera sa o meta-analýzy a kontrolované štúdie."
    Teoria.PRAX -> "Prax trénerov — osvedčené v praxi, kontrolované štúdie k tomu zatiaľ chýbajú. " +
        "Appka to preto nepodáva ako istotu."
    else -> "Čiastočne podložené — časť tvrdení je overená štúdiami, zvyšok je osvedčená prax."
}

/** Ikona témy článku (v chrome appky ikony, nie emoji). */
private fun ikonaClanku(id: String): ImageVector = when (id) {
    "prvy-mesiac" -> Icons.Outlined.Psychology
    "objem" -> Icons.Outlined.StackedBarChart
    "zlyhanie" -> Icons.Outlined.DoNotDisturbOn
    "pauzy" -> Icons.Outlined.Timer
    "variacie" -> Icons.Outlined.Stairs
    "rozsah" -> Icons.Outlined.Straighten
    "zrucnosti" -> Icons.Outlined.TrackChanges
    "muscle-up" -> Icons.Outlined.FitnessCenter
    "denna-doba" -> Icons.Outlined.WbTwilight
    "bielkoviny" -> Icons.Outlined.Restaurant
    "deload" -> Icons.Outlined.BatteryChargingFull
    "deti" -> Icons.Outlined.ChildCare
    "navyk" -> Icons.Outlined.Repeat
    else -> Icons.AutoMirrored.Outlined.MenuBook
}
