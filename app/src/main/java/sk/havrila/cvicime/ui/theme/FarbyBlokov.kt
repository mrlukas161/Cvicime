package sk.havrila.cvicime.ui.theme

import androidx.compose.ui.graphics.Color
import sk.havrila.cvicime.data.model.Blok

/**
 * Každý blok tréningu má vlastnú farbu, ktorá sa opakuje naprieč appkou
 * (plán na Domove, hlavička prehrávača, História). Jeden konzistentný
 * farebný jazyk namiesto jednoliatej plochy - používateľ sa naučí čítať
 * tréning podľa farieb skôr než podľa slov.
 */
object FarbyBlokov {

    private val mapa = mapOf(
        Blok.ROZOHRIATIE to Color(0xFFD9A03C), // jantárová - zahrievanie
        Blok.SKILL to Color(0xFFE0703C),       // plameň - hlavná farba appky, cieľové zručnosti
        Blok.SILA to Color(0xFF2E7D6E),        // hlboká zelenomodrá - ťažká práca
        Blok.KONDICIA to Color(0xFFC2554A),    // tehlová - kolotoč
        Blok.MOBILITA to Color(0xFF6E9B5A),    // listová - uvoľnenie
    )

    fun pre(blok: Blok): Color = mapa[blok] ?: Color(0xFF888888)
}
