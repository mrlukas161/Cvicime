package sk.havrila.cvicime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sk.havrila.cvicime.aktualizacia.AppUpdater
import sk.havrila.cvicime.ui.komponenty.PasPlnenia
import sk.havrila.cvicime.ui.komponenty.rememberHaptika
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
import sk.havrila.cvicime.ui.theme.Grotesk

class MainActivity : ComponentActivity() {

    private val ziadostONotifikacie =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obsah kreslíme pod stavovú aj navigačnú lištu, odsadenia rieši Plocha() cez insety.
        // Navigačná lišta systému je priehľadná a bez závoja - pod ňou je buď naša spodná
        // lišta (surfaceContainer), alebo pokojné pozadie obrazovky. Farbu ikon systémových
        // líšt (svetlé/tmavé) nastaví enableEdgeToEdge podľa režimu, rovnako ako tému appky.
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        pytajNotifikacieAkTreba()
        setContent {
            CvicimeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
 * Vzhľad: ikona v tónovanom kruhu, nadpis písmom Grotesk, jedna plná hlavná akcia
 * a tiché „Neskôr". Logika (AppUpdater, povolenie inštalácie) je nezmenená.
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
    val farby = MaterialTheme.colorScheme
    val textStyl = MaterialTheme.typography.bodyLarge
    AlertDialog(
        onDismissRequest = { if (!stahuje) novaVerzia = null },
        containerColor = farby.surfaceContainerHigh,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(farby.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = farby.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                "Nová verzia ${v.nazovVerzie}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Grotesk,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    stahuje -> {
                        Text("Sťahujem… $percenta %", style = textStyl)
                        PasPlnenia(postup = percenta / 100f)
                    }
                    chyba != null -> Text(chyba!!, style = textStyl, color = farby.error)
                    !smie -> Text(
                        "Android najprv potrebuje povolenie inštalovať aplikácie z tejto appky. " +
                            "Zapneš ho jedným prepínačom a viac sa už nebude pýtať.",
                        style = textStyl,
                    )
                    else -> Text("Aktualizácia sa stiahne a systém sa spýta na inštaláciu.", style = textStyl)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !stahuje,
                onClick = {
                    if (!smie) {
                        AppUpdater.otvorPovolenie(kontext)
                        return@Button
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

/** Záložka spodnej lišty: plná ikona, keď je vybraná, obrysová, keď nie. */
private data class Zalozka(
    val cesta: String,
    val popis: String,
    val ikona: ImageVector,
    val ikonaObrys: ImageVector,
)

private val zalozky = listOf(
    Zalozka(Cesty.DOMOV, "Dnes", Icons.Filled.Today, Icons.Outlined.Today),
    Zalozka(Cesty.PROGRES, "Pokrok", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp),
    Zalozka(Cesty.KNIZNICA, "Cviky", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    Zalozka(Cesty.HISTORIA, "História", Icons.Filled.History, Icons.Outlined.History),
    Zalozka(Cesty.NASTAVENIA, "Nastavenia", Icons.Filled.Settings, Icons.Outlined.Settings),
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

// ------------------------------------------------------------------ pohyb medzi obrazovkami

/** Bežný prechod: krátky a vecný. */
private const val TRVANIE = 250

/** Prehrávač a test vychádzajú zdola cez celú výšku - o chlp dlhšie, aby pohyb nebol trhaný. */
private const val TRVANIE_ZDOLA = 320

/** Kým sa odchádzajúca obrazovka stratí, prichádzajúca ešte čaká (zdieľaná os - bez dvojitého obrazu). */
private const val ODCHOD_ZMIZNE = 80

/** Krivky M3 „emphasized": dlhý mäkký dobeh pri príchode, rýchly rozbeh pri odchode. */
private val ZvyraznenyDobeh = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ZvyraznenyRozbeh = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Prehrávač a test nie sú „ďalšia stránka", ale vrstva, ktorá príde zdola a zdola aj odíde. */
private val TRASY_ZDOLA = setOf(Cesty.PREHRAVAC, Cesty.TEST)

private val TRASY_ZALOZIEK = zalozky.map { it.cesta }

private fun NavBackStackEntry.jeZdola() = destination.route in TRASY_ZDOLA

/**
 * Smer vodorovného posunu: +1 = dopredu (nová obrazovka prichádza sprava), -1 = späť.
 * Medzi záložkami rozhoduje ich poradie v lište - obsah sa posunie tým smerom, kam ťukol prst.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.smer(navrat: Boolean): Int {
    val z = TRASY_ZALOZIEK.indexOf(initialState.destination.route)
    val na = TRASY_ZALOZIEK.indexOf(targetState.destination.route)
    return when {
        z >= 0 && na >= 0 && z != na -> if (na > z) 1 else -1
        navrat -> -1
        else -> 1
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.vstup(navrat: Boolean): EnterTransition = when {
    !navrat && targetState.jeZdola() ->
        slideInVertically(tween(TRVANIE_ZDOLA, easing = ZvyraznenyDobeh)) { it } +
            fadeIn(tween(TRVANIE_ZDOLA / 3))

    navrat && initialState.jeZdola() ->
        // Obrazovka pod prehrávačom sa po jeho odchode len vynorí späť.
        fadeIn(tween(TRVANIE)) + scaleIn(tween(TRVANIE, easing = ZvyraznenyDobeh), initialScale = 0.94f)

    else -> {
        val s = smer(navrat)
        fadeIn(tween(TRVANIE - ODCHOD_ZMIZNE, delayMillis = ODCHOD_ZMIZNE, easing = LinearOutSlowInEasing)) +
            slideInHorizontally(tween(TRVANIE, easing = FastOutSlowInEasing)) { s * it / 10 }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.odchod(navrat: Boolean): ExitTransition = when {
    navrat && initialState.jeZdola() ->
        slideOutVertically(tween(TRVANIE, easing = ZvyraznenyRozbeh)) { it }

    !navrat && targetState.jeZdola() ->
        // Obrazovka pod prichádzajúcim prehrávačom ustúpi do hĺbky.
        fadeOut(tween(TRVANIE)) + scaleOut(tween(TRVANIE), targetScale = 0.94f)

    else -> {
        val s = smer(navrat)
        fadeOut(tween(ODCHOD_ZMIZNE, easing = LinearEasing)) +
            slideOutHorizontally(tween(TRVANIE, easing = FastOutSlowInEasing)) { -s * it / 10 }
    }
}

// ------------------------------------------------------------------ kostra

@Composable
private fun Aplikacia() {
    val navigacia = rememberNavController()
    val zaznam by navigacia.currentBackStackEntryAsState()
    val aktualna = zaznam?.destination
    val haptika = rememberHaptika()

    // Spodnú lištu skrývame tam, kde by len prekážala - v onboardingu, teste, prehrávači
    // a na podstránkach. Pred prvým snímkom (aktualna == null) je štart = Dnes, takže lišta
    // je viditeľná hneď a pri spustení appky nevychádza zdola.
    val aktualnaZalozka = zalozky.firstOrNull { z -> aktualna?.hierarchy?.any { it.route == z.cesta } == true }?.cesta
    val ukazatListu = aktualna == null || aktualnaZalozka != null

    // Kým lišta odchádza (napr. pod prehrávač), drží si poslednú vybranú záložku,
    // aby indikátor nezmizol uprostred pohybu.
    var poslednaZalozka by rememberSaveable { mutableStateOf(Cesty.DOMOV) }
    val vybrana = aktualnaZalozka ?: poslednaZalozka
    SideEffect { poslednaZalozka = vybrana }

    Scaffold(
        // Bezpečná plocha = systémové lišty + výrez displeja + klávesnica.
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            AnimatedVisibility(
                visible = ukazatListu,
                enter = slideInVertically(tween(TRVANIE, easing = ZvyraznenyDobeh)) { it } + fadeIn(tween(TRVANIE)),
                exit = slideOutVertically(tween(TRVANIE, easing = ZvyraznenyRozbeh)) { it } + fadeOut(tween(TRVANIE)),
            ) {
                SpodnaLista(vybrana) { z ->
                    haptika.klik()
                    navigacia.navigate(z.cesta) {
                        popUpTo(navigacia.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { vypln ->
        Navigacia(navigacia, vypln)
    }
}

@Composable
private fun SpodnaLista(vybranaCesta: String, naZalozku: (Zalozka) -> Unit) {
    val farby = MaterialTheme.colorScheme
    NavigationBar(containerColor = farby.surfaceContainer, tonalElevation = 0.dp) {
        zalozky.forEach { z ->
            val vybrana = z.cesta == vybranaCesta
            NavigationBarItem(
                selected = vybrana,
                onClick = { naZalozku(z) },
                // Popis nesie štítok pod ikonou - ikona ho neopakuje (čítačka by ho čítala dvakrát).
                icon = { Icon(if (vybrana) z.ikona else z.ikonaObrys, contentDescription = null) },
                label = {
                    Text(
                        z.popis,
                        fontWeight = if (vybrana) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = farby.primary,
                    selectedTextColor = farby.onSurface,
                    indicatorColor = farby.primary.copy(alpha = 0.18f),
                    unselectedIconColor = farby.onSurfaceVariant,
                    unselectedTextColor = farby.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Plocha jednej obrazovky: nepriehľadné pozadie (aby sa obrazovky pri prechode neprekrývali)
 * a odsadenie od systému.
 *  - Záložky (so spodnou lištou) sa odsadzujú výplňou Scaffoldu - zhora stavová lišta, zdola
 *    naša lišta; otvorená klávesnica pridá zvyšok nad lištou.
 *  - Ostatné (prehrávač, test, onboarding, podstránky) idú na celú obrazovku a odsadzujú sa
 *    priamo od bezpečnej plochy okna - nehýbu sa teda, keď lišta odchádza alebo prichádza.
 * Insety sa tu spotrebujú, obrazovky si ich už nepridávajú (statusBarsPadding a pod. dá 0).
 */
@Composable
private fun Plocha(vyplnStav: State<PaddingValues>, sListou: Boolean, obsah: @Composable () -> Unit) {
    val odsadenie = if (sListou) {
        val vypln = vyplnStav.value
        Modifier
            .padding(vypln)
            .consumeWindowInsets(vypln)
            .imePadding()
    } else {
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(odsadenie),
    ) {
        obsah()
    }
}

@Composable
private fun Navigacia(navigacia: NavHostController, vypln: PaddingValues) {
    // Graf navigácie sa nesmie skladať znova pri každej zmene insetov (klávesnica, lišta) -
    // výplň preto ide do obrazoviek cez stav, nie priamo do stavača grafu.
    val vyplnStav = rememberUpdatedState(vypln)

    NavHost(
        navController = navigacia,
        startDestination = Cesty.DOMOV,
        enterTransition = { vstup(navrat = false) },
        exitTransition = { odchod(navrat = false) },
        popEnterTransition = { vstup(navrat = true) },
        popExitTransition = { odchod(navrat = true) },
    ) {
        composable(Cesty.DOMOV) {
            Plocha(vyplnStav, sListou = true) {
                DomovObrazovka(
                    naOnboarding = { navigacia.navigate(Cesty.ONBOARDING) },
                    naTest = { navigacia.navigate(Cesty.TEST) },
                    naPrehravac = { minuty, dieta -> navigacia.navigate(Cesty.prehravac(minuty, dieta)) },
                )
            }
        }
        composable(Cesty.PROGRES) {
            Plocha(vyplnStav, sListou = true) {
                ProgresObrazovka(naTest = { navigacia.navigate(Cesty.TEST) })
            }
        }
        composable(Cesty.ONBOARDING) {
            Plocha(vyplnStav, sListou = false) {
                OnboardingObrazovka(hotovo = { navigacia.popBackStack() })
            }
        }
        composable(Cesty.TEST) {
            Plocha(vyplnStav, sListou = false) {
                TestObrazovka(hotovo = { navigacia.popBackStack() })
            }
        }
        composable(Cesty.PREHRAVAC) { zaznam ->
            val minuty = zaznam.arguments?.getString("minuty")?.toIntOrNull() ?: 30
            val dieta = zaznam.arguments?.getString("dieta")?.toLongOrNull() ?: -1L
            Plocha(vyplnStav, sListou = false) {
                PrehravacObrazovka(minuty = minuty, dietaId = dieta, hotovo = { navigacia.popBackStack() })
            }
        }
        composable(Cesty.KNIZNICA) {
            Plocha(vyplnStav, sListou = true) {
                KniznicaObrazovka(naRebrik = { id -> navigacia.navigate(Cesty.rebrik(id)) })
            }
        }
        composable(Cesty.REBRIK) { zaznam ->
            val id = zaznam.arguments?.getString("rebrikId").orEmpty()
            Plocha(vyplnStav, sListou = false) {
                RebrikObrazovka(rebrikId = id, spat = { navigacia.popBackStack() })
            }
        }
        composable(Cesty.HISTORIA) {
            Plocha(vyplnStav, sListou = true) { HistoriaObrazovka() }
        }
        composable(Cesty.NASTAVENIA) {
            Plocha(vyplnStav, sListou = true) {
                NastaveniaObrazovka(
                    naOnboarding = { navigacia.navigate(Cesty.ONBOARDING) },
                    naTeoria = { navigacia.navigate(Cesty.TEORIA) },
                    naZbierka = { navigacia.navigate(Cesty.ZBIERKA) },
                )
            }
        }
        composable(Cesty.TEORIA) {
            Plocha(vyplnStav, sListou = false) {
                TeoriaObrazovka(naClanok = { id -> navigacia.navigate(Cesty.clanok(id)) })
            }
        }
        composable(Cesty.CLANOK) { zaznam ->
            val id = zaznam.arguments?.getString("clanokId").orEmpty()
            Plocha(vyplnStav, sListou = false) {
                ClanokObrazovka(clanokId = id, spat = { navigacia.popBackStack() })
            }
        }
        composable(Cesty.ZBIERKA) {
            Plocha(vyplnStav, sListou = false) { ZbierkaObrazovka() }
        }
    }
}
