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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import sk.havrila.cvicime.ui.obrazovky.DomovObrazovka
import sk.havrila.cvicime.ui.obrazovky.HistoriaObrazovka
import sk.havrila.cvicime.ui.obrazovky.KniznicaObrazovka
import sk.havrila.cvicime.ui.obrazovky.NastaveniaObrazovka
import sk.havrila.cvicime.ui.obrazovky.OnboardingObrazovka
import sk.havrila.cvicime.ui.obrazovky.PrehravacObrazovka
import sk.havrila.cvicime.ui.obrazovky.RebrikObrazovka
import sk.havrila.cvicime.ui.obrazovky.TestObrazovka
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

private data class Zalozka(val cesta: String, val popis: String, val ikona: ImageVector)

private val zalozky = listOf(
    Zalozka(Cesty.DOMOV, "Dnes", Icons.Default.Today),
    Zalozka(Cesty.KNIZNICA, "Cviky", Icons.Default.FitnessCenter),
    Zalozka(Cesty.HISTORIA, "História", Icons.Default.History),
    Zalozka(Cesty.NASTAVENIA, "Nastavenia", Icons.Default.Settings),
)

object Cesty {
    const val ONBOARDING = "onboarding"
    const val TEST = "test"
    const val DOMOV = "domov"
    const val PREHRAVAC = "prehravac/{minuty}"
    const val KNIZNICA = "kniznica"
    const val REBRIK = "kniznica/{rebrikId}"
    const val HISTORIA = "historia"
    const val NASTAVENIA = "nastavenia"

    fun prehravac(minuty: Int) = "prehravac/$minuty"
    fun rebrik(id: String) = "kniznica/$id"
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
                naPrehravac = { minuty -> navigacia.navigate(Cesty.prehravac(minuty)) },
            )
        }
        composable(Cesty.ONBOARDING) {
            OnboardingObrazovka(hotovo = { navigacia.popBackStack() })
        }
        composable(Cesty.TEST) {
            TestObrazovka(hotovo = { navigacia.popBackStack() })
        }
        composable(Cesty.PREHRAVAC) { zaznam ->
            val minuty = zaznam.arguments?.getString("minuty")?.toIntOrNull() ?: 30
            PrehravacObrazovka(minuty = minuty, hotovo = { navigacia.popBackStack() })
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
            NastaveniaObrazovka(naOnboarding = { navigacia.navigate(Cesty.ONBOARDING) })
        }
    }
}
