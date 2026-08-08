package sk.havrila.cvicime.domain

import kotlin.math.ceil

/**
 * Výpočet pokroku k cieľom.
 *
 * Každá zručnosť má BRÁNY - prerekvizity vyjadrené ako podiel iného rebríka,
 * ktorý treba mať vylezený. Podiely vychádzajú z trénerskej praxe (nie zo štúdií;
 * appka to v Teórii priznáva) - napr. muscle-up vyžaduje ~10 príťahov, čo zodpovedá
 * vysokému stupňu rebríka príťahov.
 *
 * Čistá funkcia bez databázy - dá sa testovať.
 */
data class Brana(
    val rebrikId: String,
    val nazov: String,
    /** aký podiel rebríka treba mať (0..1) */
    val podiel: Double,
)

data class StavBrany(
    val brana: Brana,
    val aktualnaUroven: Int,
    val potrebnaUroven: Int,
    /** 0..1 */
    val pokrok: Double,
)

data class StavCiela(
    val kluc: String,
    val nazov: String,
    val brany: List<StavBrany>,
    /** celkový pokrok cieľa 0..1 = priemer brán */
    val pokrok: Double,
    /** brána s najnižším pokrokom - to ťa drží */
    val brzda: StavBrany?,
)

object Pokrok {

    /** Brány zručností. Vlastný rebrík zručnosti je vždy jednou z brán. */
    private val brany: Map<String, List<Brana>> = mapOf(
        "muscleup" to listOf(
            Brana("muscleup", "Rebrík muscle-upu", 1.0),
            Brana("pritahy", "Príťahy", 0.7),
            Brana("vis", "Vis a stisk", 0.6),
        ),
        "stojka" to listOf(
            Brana("stojka", "Rebrík stojky", 1.0),
            Brana("kliky-do-stojky", "Tlak nad hlavou", 0.5),
            Brana("mobilita-ramena", "Pohyblivosť ramien", 0.6),
            Brana("mobilita-zapastia", "Pripravené zápästia", 0.5),
        ),
        "lsit" to listOf(
            Brana("lsit", "Rebrík L-situ", 1.0),
            Brana("hollow", "Pevný stred (hollow)", 0.6),
            Brana("mobilita-kompresia", "Kompresia bedier", 0.5),
        ),
    )

    /**
     * Vyhodnotí zručnosť.
     *
     * @param urovne rebrikId -> aktuálna úroveň používateľa
     * @param maxUrovne rebrikId -> najvyšší stupeň v katalógu
     */
    fun vyhodnotZrucnost(
        kluc: String,
        nazov: String,
        urovne: Map<String, Int>,
        maxUrovne: Map<String, Int>,
    ): StavCiela {
        val zoznam = brany[kluc] ?: listOf(Brana(kluc, nazov, 1.0))
        val stavy = zoznam.mapNotNull { b ->
            val max = maxUrovne[b.rebrikId] ?: return@mapNotNull null
            val potrebna = ceil(max * b.podiel).toInt().coerceIn(1, max)
            val aktualna = (urovne[b.rebrikId] ?: 1).coerceAtLeast(1)
            StavBrany(
                brana = b,
                aktualnaUroven = aktualna,
                potrebnaUroven = potrebna,
                // úroveň 1 = stojíš na začiatku, preto -1 v čitateli aj menovateli
                pokrok = if (potrebna <= 1) 1.0 else ((aktualna - 1).toDouble() / (potrebna - 1)).coerceIn(0.0, 1.0),
            )
        }
        val pokrok = if (stavy.isEmpty()) 0.0 else stavy.sumOf { it.pokrok } / stavy.size
        return StavCiela(
            kluc = kluc,
            nazov = nazov,
            brany = stavy,
            pokrok = pokrok,
            brzda = stavy.filter { it.pokrok < 1.0 }.minByOrNull { it.pokrok },
        )
    }

    /**
     * Slovná formulácia brzdy - jedna veta, ktorú appka ukáže.
     * Vecná, bez „si super" aj bez výčitiek.
     */
    fun vetaBrzdy(ciel: StavCiela): String? {
        val b = ciel.brzda ?: return null
        return "${ciel.nazov}: najviac ťa drží „${b.brana.nazov}\" — " +
            "si na stupni ${b.aktualnaUroven} z potrebných ${b.potrebnaUroven}."
    }
}
