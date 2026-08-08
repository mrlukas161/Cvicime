package sk.havrila.cvicime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import sk.havrila.cvicime.aktualizacia.AppUpdater
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import sk.havrila.cvicime.ui.obrazovky.ClanokObrazovka
import sk.havrila.cvicime.ui.obrazovky.DomovObrazovka
import sk.havrila.cvicime.ui.obrazovky.HistoriaObrazovka
import sk.havrila.cvicime.ui.obrazovky.KniznicaObrazovka
import sk.havrila.cvicime.ui.obrazovky.NastaveniaObrazovka
import sk.havrila.cvicime.ui.obrazovky.OnboardingObrazovka
import sk.havrila.cvicime.ui.obrazovky.PrehravacObrazovka
import sk.havrila.cvicime.ui.obrazovky.ProgresObrazovka
import sk.havrila.cvicime.ui.obrazovky.RebrikObrazovka
import sk.havrila.cvicime.ui.obrazovky.TeoriaObrazovka
import sk.havrila.cvicime.ui.obrazovky.TestObrazovka
import sk.havrila.cvicime.ui.obrazovky.ZbierkaObrazovka
import sk.havrila.cvicime.ui.theme.CvicimeTheme

class MainActivity : ComponentActivity() {

    private val ziadostONotifikacie =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pytajNotifikacieAkTreba()
        setContent {
            CvicimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Aplikacia()
                    Aktualizacia()
                }
            }
        }
    }

    private fun pytajNotifikacieAkTreba() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val dane = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (dane != PackageManager.PERMISSION_GRANTED) {
            ziadostONotifikacie.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Kontrola novej verzie raz denne pri otvorení appky.
 * Zámerne nenápadná: keď nič nie je, nič sa neukáže; keď zlyhá sieť, tiež nič.
 */
@Composable
private fun Aktualizacia() {
    val kontext = LocalContext.current
    val rozsah = rememberCoroutineScope()
    var novaVerzia by remember { mutableStateOf<AppUpdater.NovaVerzia?>(null) }
    var stahuje by remember { mutableStateOf(false) }
    var percenta by remember { mutableIntStateOf(0) }
    var chyba by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        novaVerzia = AppUpdater.zisti(kontext, nasilu = false)
    }

    val v = novaVerzia ?: return
    val smie = AppUpdater.smieInstalovat(kontext)
    AlertDialog(
        onDismissRequest = { if (!stahuje) novaVerzia = null },
        title = { Text("Nová verzia ${v.nazovVerzie}") },
        text = {
            Column {
                when {
                    stahuje -> Text("Sťahujem… $percenta %")
                    chyba != null -> Text(chyba!!)
                    !smie -> Text(
                        "Android najprv potrebuje povolenie inštalovať aplikácie z tejto appky. " +
                            "Zapneš ho jedným prepínačom a viac sa už nebude pýtať."
                    )
                    else -> Text("Aktualizácia sa stiahne a systém sa spýta na inštaláciu.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !stahuje,
                onClick = {
                    if (!smie) {
                        AppUpdater.otvorPovolenie(kontext)
                        return@TextButton
                    }
                    stahuje = true
                    chyba = null
                    rozsah.launch {
                        val problem = AppUpdater.stiahniANainstaluj(kontext, v) { percenta = it }
                        stahuje = false
                        if (problem != null) chyba = problem else novaVerzia = null
                    }
                },
            ) { Text(if (!smie) "Povoliť" else "Aktualizovať") }
        },
        dismissButton = {
            TextButton(enabled = !stahuje, onClick = { novaVerzia = null }) { Text("Neskôr") }
        },
    )
}

private data class Zalozka(val cesta: String, val popis: String, val ikona: ImageVector)

private val zalozky = listOf(
    Zalozka(Cesty.DOMOV, "Dnes", Icons.Default.Today),
    Zalozka(Cesty.PROGRES, "Pokrok", Icons.AutoMirrored.Filled.TrendingUp),
    Zalozka(Cesty.KNIZNICA, "Cviky", Icons.Default.FitnessCenter),
    Zalozka(Cesty.HISTORIA, "História", Icons.Default.History),
    Zalozka(Cesty.NASTAVENIA, "Nastavenia", Icons.Default.Settings),
)

object Cesty {
    const val ONBOARDING = "onboarding"
    const val TEST = "test"
    const val DOMOV = "domov"
    const val PROGRES = "progres"
    const val PREHRAVAC = "prehravac/{minuty}/{dieta}"
    const val KNIZNICA = "kniznica"
    const val REBRIK = "kniznica/{rebrikId}"
    const val HISTORIA = "historia"
    const val NASTAVENIA = "nastavenia"
    const val TEORIA = "teoria"
    const val CLANOK = "teoria/{clanokId}"
    const val ZBIERKA = "zbierka"

    fun prehravac(minuty: Int, dieta: Long) = "prehravac/$minuty/$dieta"
    fun rebrik(id: String) = "kniznica/$id"
    fun clanok(id: String) = "teoria/$id"
}

@Composable
private fun Aplikacia() {
    val navigacia = rememberNavController()
    val zaznam by navigacia.currentBackStackEntryAsState()
    val aktualna = zaznam?.destination

    // Spodnú lištu skrývame tam, kde by len prekážala - v onboardingu, teste a prehrávači.
    val ukazatListu = zalozky.any { z -> aktualna?.hierarchy?.any { it.route == z.cesta } == true }

    Scaffold(
        bottomBar = {
            if (ukazatListu) {
                NavigationBar {
                    zalozky.forEach { z ->
                        val vybrana = aktualna?.hierarchy?.any { it.route == z.cesta } == true
                        NavigationBarItem(
                            selected = vybrana,
                            onClick = {
                                navigacia.navigate(z.cesta) {
                                    popUpTo(navigacia.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(z.ikona, contentDescription = z.popis) },
                            label = { Text(z.popis) },
                        )
                    }
                }
            }
        }
    ) { vypln ->
        Box(modifier = Modifier.padding(vypln)) {
            Navigacia(navigacia)
        }
    }
}

@Composable
private fun Navigacia(navigacia: NavHostController) {
    NavHost(navController = navigacia, startDestination = Cesty.DOMOV) {
        composable(Cesty.DOMOV) {
            DomovObrazovka(
                naOnboarding = { navigacia.navigate(Cesty.ONBOARDING) },
                naTest = { navigacia.navigate(Cesty.TEST) },
                naPrehravac = { minuty, dieta -> navigacia.navigate(Cesty.prehravac(minuty, dieta)) },
            )
        }
        composable(Cesty.PROGRES) {
            ProgresObrazovka(naTest = { navigacia.navigate(Cesty.TEST) })
        }
        composable(Cesty.ONBOARDING) {
            OnboardingObrazovka(hotovo = { navigacia.popBackStack() })
        }
        composable(Cesty.TEST) {
            TestObrazovka(hotovo = { navigacia.popBackStack() })
        }
        composable(Cesty.PREHRAVAC) { zaznam ->
            val minuty = zaznam.arguments?.getString("minuty")?.toIntOrNull() ?: 30
            val dieta = zaznam.arguments?.getString("dieta")?.toLongOrNull() ?: -1L
            PrehravacObrazovka(minuty = minuty, dietaId = dieta, hotovo = { navigacia.popBackStack() })
        }
        composable(Cesty.KNIZNICA) {
            KniznicaObrazovka(naRebrik = { id -> navigacia.navigate(Cesty.rebrik(id)) })
        }
        composable(Cesty.REBRIK) { zaznam ->
            val id = zaznam.arguments?.getString("rebrikId").orEmpty()
            RebrikObrazovka(rebrikId = id, spat = { navigacia.popBackStack() })
        }
        composable(Cesty.HISTORIA) { HistoriaObrazovka() }
        composable(Cesty.NASTAVENIA) {
            NastaveniaObrazovka(
                naOnboarding = { navigacia.navigate(Cesty.ONBOARDING) },
                naTeoria = { navigacia.navigate(Cesty.TEORIA) },
                naZbierka = { navigacia.navigate(Cesty.ZBIERKA) },
            )
        }
        composable(Cesty.TEORIA) {
            TeoriaObrazovka(naClanok = { id -> navigacia.navigate(Cesty.clanok(id)) })
        }
        composable(Cesty.CLANOK) { zaznam ->
            val id = zaznam.arguments?.getString("clanokId").orEmpty()
            ClanokObrazovka(clanokId = id, spat = { navigacia.popBackStack() })
        }
        composable(Cesty.ZBIERKA) { ZbierkaObrazovka() }
    }
}
