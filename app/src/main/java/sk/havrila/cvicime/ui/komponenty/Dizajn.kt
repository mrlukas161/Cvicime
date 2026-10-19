package sk.havrila.cvicime.ui.komponenty

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Spoločné stavebné kamene redizajnu 0.6. Obrazovky ich skladajú, nekreslia si
 * vlastné varianty - jednotný vzhľad je to, čo odlišuje produkt od zlepenca obrazoviek.
 *
 * Pravidlá jazyka:
 *  - Číslo je hrdina: veľké hodnoty písmom Grotesk, popis pod nimi malý a tlmený.
 *  - Karty sú tónované plochy bez tieňa (surfaceContainer*), zaoblenie z MaterialTheme.shapes.
 *  - Farba má význam: farby blokov (FarbyBlokov), primary = akcia, tertiary = deti.
 *  - Pohyb vždy niečo hovorí (zmena hodnoty, prechod kroku), nikdy len ozdoba.
 */

// ------------------------------------------------------------------ haptika

/** Krátke hmatové potvrdenia. Správa sa podľa systémového nastavenia vibrácií pri dotyku. */
class Haptika(private val view: View) {
    /** Bežné ťuknutie na tlačidlo. */
    fun klik() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Potvrdenie dôležitej akcie (séria zapísaná, krok hotový). */
    fun potvrdenie() {
        val konst = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
        view.performHapticFeedback(konst)
    }

    /** Výrazný moment - rekord, postup na vyšší stupeň, koniec tréningu. */
    fun uspech() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Krok stepperu / posuvníka. */
    fun tik() {
        val konst = if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_RELEASE
        else HapticFeedbackConstants.KEYBOARD_TAP
        view.performHapticFeedback(konst)
    }
}

@Composable
fun rememberHaptika(): Haptika {
    val view = LocalView.current
    return remember(view) { Haptika(view) }
}

// ------------------------------------------------------------------ pohyb

/** Pri stlačení sa prvok jemne zmenší a pružne vráti - hmatateľnosť bez hluku. */
@Composable
fun Modifier.pruzneStlacenie(interakcia: MutableInteractionSource): Modifier {
    val stlacene by interakcia.collectIsPressedAsState()
    val mierka by animateFloatAsState(
        targetValue = if (stlacene) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pruzneStlacenie",
    )
    return this.graphicsLayer { scaleX = mierka; scaleY = mierka }
}

/** Číslo, ktoré pri zmene "prejde" hore alebo dole ako na mechanickom počítadle. */
@Composable
fun AnimovaneCislo(
    hodnota: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.Unspecified,
    pripona: String = "",
) {
    AnimatedContent(
        targetState = hodnota,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            } else {
                (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
            }
        },
        modifier = modifier,
        label = "animovaneCislo",
    ) { h ->
        Text("$h$pripona", style = style, color = color)
    }
}

// ------------------------------------------------------------------ kruh a pruhy

/**
 * Kruhový ukazovateľ (odpočet, plnenie cieľa). [postup] 0..1.
 * Pri odpočte posielaj plynulú hodnotu (napr. z milisekúnd), animácia je krátka a lineárna,
 * aby kruh "tiekol" a neposkakoval po sekundách.
 */
@Composable
fun KruhPostupu(
    postup: Float,
    modifier: Modifier = Modifier,
    farba: Color = MaterialTheme.colorScheme.primary,
    stopa: Color = farba.copy(alpha = 0.16f),
    hrubka: Dp = 14.dp,
    plynulo: Boolean = true,
    obsah: @Composable BoxScope.() -> Unit = {},
) {
    val animovany by animateFloatAsState(
        targetValue = postup.coerceIn(0f, 1f),
        animationSpec = if (plynulo) tween(durationMillis = 280, easing = LinearEasing)
        else spring(stiffness = Spring.StiffnessLow),
        label = "kruhPostupu",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val h = hrubka.toPx()
            val priemer = size.minDimension - h
            val lavyHorny = Offset((size.width - priemer) / 2f, (size.height - priemer) / 2f)
            val velkost = Size(priemer, priemer)
            drawArc(stopa, 0f, 360f, false, lavyHorny, velkost, style = Stroke(h))
            if (animovany > 0f) {
                drawArc(
                    farba, -90f, 360f * animovany, false, lavyHorny, velkost,
                    style = Stroke(h, cap = StrokeCap.Round),
                )
            }
        }
        obsah()
    }
}

/**
 * Pruh celého tréningu po krokoch - každý segment má farbu svojho bloku.
 * Hotové segmenty sú plné, aktuálny sa plní podľa [postupAktualneho], budúce sú stopa.
 */
@Composable
fun SegmentovyPruh(
    farby: List<Color>,
    aktualny: Int,
    modifier: Modifier = Modifier,
    postupAktualneho: Float = 0f,
    vyska: Dp = 6.dp,
    medzera: Dp = 3.dp,
) {
    val stopa = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val animovany by animateFloatAsState(postupAktualneho.coerceIn(0f, 1f), tween(280, easing = LinearEasing), label = "segment")
    Canvas(modifier.fillMaxWidth().height(vyska)) {
        if (farby.isEmpty()) return@Canvas
        val m = medzera.toPx()
        val sirka = (size.width - m * (farby.size - 1)) / farby.size
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        farby.forEachIndexed { i, f ->
            val x = i * (sirka + m)
            drawRoundRect(stopa, Offset(x, 0f), Size(sirka, size.height), r)
            val plnenie = when {
                i < aktualny -> 1f
                i == aktualny -> animovany
                else -> 0f
            }
            if (plnenie > 0f) drawRoundRect(f, Offset(x, 0f), Size(sirka * plnenie, size.height), r)
        }
    }
}

/** Vodorovný ukazovateľ plnenia (objem partie, brána cieľa). */
@Composable
fun PasPlnenia(
    postup: Float,
    modifier: Modifier = Modifier,
    farba: Color = MaterialTheme.colorScheme.primary,
    vyska: Dp = 10.dp,
) {
    val animovany by animateFloatAsState(postup.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "pas")
    Box(
        modifier
            .fillMaxWidth()
            .height(vyska)
            .clip(CircleShape)
            .background(farba.copy(alpha = 0.16f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animovany)
                .height(vyska)
                .clip(CircleShape)
                .background(farba)
        )
    }
}

// ------------------------------------------------------------------ karty a texty

/**
 * Základná karta appky: tónovaná plocha bez tieňa. S [farba] dostane jemný nádych farby
 * (napr. bloku), bez nej je neutrálna.
 */
@Composable
fun TonovanaKarta(
    modifier: Modifier = Modifier,
    farba: Color? = null,
    onClick: (() -> Unit)? = null,
    vnutorneOdsadenie: Dp = 20.dp,
    obsah: @Composable ColumnScope.() -> Unit,
) {
    val zaklad = MaterialTheme.colorScheme.surfaceContainerLow
    val pozadie = farba?.copy(alpha = 0.12f)?.compositeOver(zaklad) ?: zaklad
    val tvar = MaterialTheme.shapes.large
    if (onClick != null) {
        val interakcia = remember { MutableInteractionSource() }
        val haptika = rememberHaptika()
        Surface(
            onClick = { haptika.klik(); onClick() },
            modifier = modifier.fillMaxWidth().pruzneStlacenie(interakcia),
            shape = tvar,
            color = pozadie,
            interactionSource = interakcia,
        ) {
            Column(Modifier.padding(vnutorneOdsadenie), content = obsah)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = tvar, color = pozadie) {
            Column(Modifier.padding(vnutorneOdsadenie), content = obsah)
        }
    }
}

/** Nadpis sekcie s voliteľnou akciou vpravo ("Všetko", "Upraviť"). */
@Composable
fun NadpisSekcie(
    text: String,
    modifier: Modifier = Modifier,
    akcia: String? = null,
    naAkciu: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk, letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.6f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (akcia != null && naAkciu != null) {
            TextButton(onClick = naAkciu) { Text(akcia) }
        }
    }
}

/** Štatistická dlaždica: veľké číslo + malý popis. */
@Composable
fun StatDlazdica(
    hodnota: String,
    popis: String,
    modifier: Modifier = Modifier,
    farba: Color = MaterialTheme.colorScheme.onSurface,
    ikona: ImageVector? = null,
) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (ikona != null) {
                Icon(ikona, contentDescription = null, tint = farba.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(6.dp))
            }
            Text(hodnota, style = MaterialTheme.typography.headlineMedium, color = farba, maxLines = 1)
            Text(popis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** Kruh s prvým písmenom mena vo farbe cvičiaceho - dospelí aj deti. */
@Composable
fun Inicialka(meno: String, farba: Color, velkost: Dp) {
    Box(
        Modifier
            .size(velkost)
            .clip(CircleShape)
            .background(farba),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            meno.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
            color = if (farba.luminance() > 0.18f) Color(0xFF1C1917) else Color.White,
        )
    }
}

/** Malá farebná kapsula - blok, náradie, štítok článku. */
@Composable
fun Kapsula(
    text: String,
    farba: Color,
    modifier: Modifier = Modifier,
    ikona: ImageVector? = null,
    plna: Boolean = false,
) {
    val pozadie = if (plna) farba else farba.copy(alpha = 0.16f)
    val popredie = if (plna) Color.White else farba
    Row(
        modifier
            .clip(CircleShape)
            .background(pozadie)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (ikona != null) Icon(ikona, contentDescription = null, tint = popredie, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = popredie, maxLines = 1)
    }
}

/** Hlavná akcia obrazovky - vysoká, na celú šírku, s haptikou. Jedna na obrazovku. */
@Composable
fun VelkeTlacidlo(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ikona: ImageVector? = null,
    farba: Color = MaterialTheme.colorScheme.primary,
    farbaTextu: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
) {
    val haptika = rememberHaptika()
    val interakcia = remember { MutableInteractionSource() }
    Button(
        onClick = { haptika.potvrdenie(); onClick() },
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).pruzneStlacenie(interakcia),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = farba, contentColor = farbaTextu),
        interactionSource = interakcia,
    ) {
        if (ikona != null) {
            Icon(ikona, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Prázdny stav - postavička, veta, voliteľne akcia. Nikdy len prázdna plocha. */
@Composable
fun PrazdnyStav(
    titulok: String,
    text: String,
    modifier: Modifier = Modifier,
    rebrikPostavicky: String? = "kliky",
    tlacidlo: String? = null,
    akcia: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (rebrikPostavicky != null) {
            Box(
                Modifier.size(132.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Postavicka(
                    rebrikPostavicky, Modifier.size(104.dp),
                    farba = MaterialTheme.colorScheme.onSurface,
                    farbaAkcent = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(titulok, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tlacidlo != null && akcia != null) {
            Spacer(Modifier.height(4.dp))
            VelkeTlacidlo(tlacidlo, akcia)
        }
    }
}

// ------------------------------------------------------------------ týždeň

val SKRATKY_DNI = listOf("Po", "Ut", "St", "Št", "Pi", "So", "Ne")

/**
 * Pás aktuálneho týždňa Po–Ne. Odcvičený deň = plný kruh, dnes = obrys.
 * Zámerne to NIE JE počítadlo série dní (Lukáš cvičí nepravidelne a séria by trestala) -
 * je to len zrkadlo týždňa.
 */
@Composable
fun TyzdenPas(
    dniSTreningom: Set<Long>,
    modifier: Modifier = Modifier,
    dnes: Long = LocalDate.now().toEpochDay(),
    farba: Color = MaterialTheme.colorScheme.primary,
) {
    val d = LocalDate.ofEpochDay(dnes)
    val pondelok = d.minusDays((d.dayOfWeek.value - 1).toLong()).toEpochDay()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val den = pondelok + i
            val odcviceny = den in dniSTreningom
            val jeDnes = den == dnes
            val buduci = den > dnes
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    SKRATKY_DNI[i],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (jeDnes) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (jeDnes) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                odcviceny -> farba
                                buduci -> Color.Transparent
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            }
                        )
                        .then(
                            if (jeDnes && !odcviceny) Modifier.border(2.dp, farba, CircleShape) else Modifier
                        )
                        .then(
                            if (buduci) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        LocalDate.ofEpochDay(den).dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (odcviceny) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ oslava

private data class Ciastica(val uhol: Float, val rychlost: Float, val farba: Color, val velkost: Float, val rotacia: Float)

/**
 * Výbuch konfiet - len pri skutočnom úspechu (rekord, postup, odhalené zviera).
 * Kreslí sa cez celý [modifier] (daj ho do Boxu nad obsah, fillMaxSize). Zmena [kluc] ho spustí znova.
 */
@Composable
fun Konfety(spustit: Boolean, modifier: Modifier = Modifier, kluc: Any = Unit) {
    if (!spustit) return
    val farby = remember {
        Blok.entries.map { FarbyBlokov.pre(it) } + Color(0xFFF2B138)
    }
    val ciastice = remember(kluc) {
        val r = Random(kluc.hashCode())
        List(70) {
            Ciastica(
                uhol = r.nextFloat() * 360f,
                rychlost = 0.35f + r.nextFloat() * 0.65f,
                farba = farby[r.nextInt(farby.size)],
                velkost = 6f + r.nextFloat() * 8f,
                rotacia = r.nextFloat() * 720f - 360f,
            )
        }
    }
    val cas = remember(kluc) { Animatable(0f) }
    LaunchedEffect(kluc) {
        cas.snapTo(0f)
        cas.animateTo(1f, tween(1800, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = cas.value
        if (t >= 1f) return@Canvas
        val stred = Offset(size.width / 2f, size.height * 0.38f)
        val dosah = size.minDimension * 0.75f
        ciastice.forEach { c ->
            val rad = Math.toRadians(c.uhol.toDouble())
            val vzd = dosah * c.rychlost * (1f - (1f - t) * (1f - t))
            val gravitacia = size.height * 0.45f * t * t
            val x = stred.x + (cos(rad) * vzd).toFloat()
            val y = stred.y + (sin(rad) * vzd).toFloat() + gravitacia
            val alfa = (1f - t).coerceIn(0f, 1f)
            rotate(c.rotacia * t, pivot = Offset(x, y)) {
                drawRect(
                    color = c.farba.copy(alpha = alfa),
                    topLeft = Offset(x - c.velkost / 2f, y - c.velkost / 4f),
                    size = Size(c.velkost, c.velkost / 2f),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ formátovanie

/** "45 min", "1 h 05 min". */
fun formatujTrvanie(ms: Long): String {
    val minuty = (ms / 60_000L).coerceAtLeast(0)
    return if (minuty < 60) "$minuty min" else "${minuty / 60} h ${"%02d".format(minuty % 60)} min"
}

/** Hodnota série podľa merania: "12×", "45 s", "1:05", "20 m". */
fun formatujHodnotu(hodnota: Int, meranie: String): String = when (meranie) {
    "vydrz" -> if (hodnota >= 60) "${hodnota / 60}:${"%02d".format(hodnota % 60)}" else "$hodnota s"
    "vzdialenost" -> "$hodnota m"
    else -> "$hodnota×"
}

/** Slovenské skloňovanie podľa čísla: sklonuj(5, "tréning", "tréningy", "tréningov"). */
fun sklonuj(n: Int, jeden: String, dvaAzStyri: String, viac: String): String = when (n) {
    1 -> jeden
    in 2..4 -> dvaAzStyri
    else -> viac
}
