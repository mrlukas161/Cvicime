package sk.havrila.cvicime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Farby sú zámerne mimo obvyklej fitness estetiky (neónovo zelená na čiernej).
 * Pálená oranžová a tlmená zelenomodrá pôsobia pokojne a vecne - appka má byť tréner,
 * nie roztlieskávačka. Detská vrstva má vlastný akcent (slnečná žltá), aby bolo
 * na prvý pohľad zrejmé, čia je práve obrazovka.
 */
val Plamen = Color(0xFFE0703C)
val PlamenTmavy = Color(0xFFB2542A)
val Zelenomodra = Color(0xFF4FA894)
val ZelenomodraTmava = Color(0xFF1B4D45)
val DetskaZlta = Color(0xFFF2B138)
val Uhol = Color(0xFF12100E)
val UholSvetlejsi = Color(0xFF1E1B18)
val Papier = Color(0xFFFAF7F2)

private val tmava = darkColorScheme(
    primary = Plamen,
    onPrimary = Color(0xFF231007),
    primaryContainer = PlamenTmavy,
    onPrimaryContainer = Color(0xFFFFE6D9),
    secondary = Zelenomodra,
    onSecondary = Color(0xFF06231E),
    secondaryContainer = ZelenomodraTmava,
    onSecondaryContainer = Color(0xFFD3F1EA),
    tertiary = DetskaZlta,
    onTertiary = Color(0xFF2A1D00),
    background = Uhol,
    onBackground = Color(0xFFEFE9E3),
    surface = UholSvetlejsi,
    onSurface = Color(0xFFEFE9E3),
    surfaceVariant = Color(0xFF2A2622),
    onSurfaceVariant = Color(0xFFBFB6AC),
    outline = Color(0xFF5C544C),
    error = Color(0xFFE5796B),
)

private val svetla = lightColorScheme(
    primary = PlamenTmavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF3A1600),
    secondary = ZelenomodraTmava,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEDE5),
    onSecondaryContainer = Color(0xFF04211C),
    tertiary = Color(0xFF8A6100),
    onTertiary = Color.White,
    background = Papier,
    onBackground = Color(0xFF1C1917),
    surface = Color.White,
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFEDE6DD),
    onSurfaceVariant = Color(0xFF4E463E),
    outline = Color(0xFF80776E),
    error = Color(0xFFB3261E),
)

/**
 * Písmo je väčšie ako býva zvykom. Appka sa číta spotenými rukami, bez okuliarov,
 * a často z telefónu položeného na zemi - preto je základ 17 sp a čísla v prehrávači
 * idú až na 84 sp.
 */
private val pismo = Typography().let { z ->
    z.copy(
        displayLarge = z.displayLarge.copy(fontSize = 84.sp, fontWeight = FontWeight.Bold),
        headlineLarge = z.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = z.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = z.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = z.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = z.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        labelLarge = z.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    )
}

/** Veľké číslo v prehrávači - čitateľné z niekoľkých metrov. */
val StylVelkeCislo = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Bold)

@Composable
fun CvicimeTheme(tmavy: Boolean = isSystemInDarkTheme(), obsah: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (tmavy) tmava else svetla,
        typography = pismo,
        content = obsah,
    )
}
