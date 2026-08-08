package sk.havrila.cvicime.ui.komponenty

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Animovaná ilustrácia cviku: jednoduchá postavička kreslená na Canvas,
 * ktorá sa hýbe medzi dvoma kľúčovými pózami daného rebríka.
 *
 * Zámerne vektor v kóde a nie 200 obrázkov: žiadne licenčné riziko (rešerš ukázala
 * nevyriešené licencie voľných sád), jednotný vzhľad, nulová veľkosť APK navyše.
 * Póza je na REBRÍK, nie na cvik - stupne toho istého vzoru zdieľajú pohyb.
 */
data class Poza(
    val hlava: Offset,
    val krk: Offset,
    val bok: Offset,
    val kolenoP: Offset,
    val chodidloP: Offset,
    val kolenoZ: Offset,
    val chodidloZ: Offset,
    val laketP: Offset,
    val rukaP: Offset,
    val laketZ: Offset,
    val rukaZ: Offset,
)

data class PozyCviku(val a: Poza, val b: Poza, val hrazda: Boolean = false)

private fun o(x: Float, y: Float) = Offset(x, y)

/** Lineárna interpolácia medzi pózami. */
private fun zmiesaj(a: Poza, b: Poza, t: Float): Poza {
    fun m(p: Offset, q: Offset) = Offset(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
    return Poza(
        m(a.hlava, b.hlava), m(a.krk, b.krk), m(a.bok, b.bok),
        m(a.kolenoP, b.kolenoP), m(a.chodidloP, b.chodidloP),
        m(a.kolenoZ, b.kolenoZ), m(a.chodidloZ, b.chodidloZ),
        m(a.laketP, b.laketP), m(a.rukaP, b.rukaP),
        m(a.laketZ, b.laketZ), m(a.rukaZ, b.rukaZ),
    )
}

object Pozy {

    // ---- opakovane použité pózy ----

    private val stoj = Poza(
        hlava = o(0.50f, 0.13f), krk = o(0.50f, 0.24f), bok = o(0.50f, 0.52f),
        kolenoP = o(0.52f, 0.70f), chodidloP = o(0.52f, 0.90f),
        kolenoZ = o(0.48f, 0.70f), chodidloZ = o(0.48f, 0.90f),
        laketP = o(0.54f, 0.37f), rukaP = o(0.55f, 0.50f),
        laketZ = o(0.46f, 0.37f), rukaZ = o(0.45f, 0.50f),
    )

    private val vis = Poza(
        hlava = o(0.50f, 0.26f), krk = o(0.50f, 0.35f), bok = o(0.50f, 0.60f),
        kolenoP = o(0.53f, 0.74f), chodidloP = o(0.51f, 0.88f),
        kolenoZ = o(0.47f, 0.74f), chodidloZ = o(0.49f, 0.88f),
        laketP = o(0.56f, 0.21f), rukaP = o(0.58f, 0.10f),
        laketZ = o(0.44f, 0.21f), rukaZ = o(0.42f, 0.10f),
    )

    private val visZhyb = Poza(
        hlava = o(0.50f, 0.13f), krk = o(0.50f, 0.22f), bok = o(0.50f, 0.46f),
        kolenoP = o(0.54f, 0.60f), chodidloP = o(0.50f, 0.74f),
        kolenoZ = o(0.46f, 0.60f), chodidloZ = o(0.44f, 0.74f),
        laketP = o(0.62f, 0.16f), rukaP = o(0.58f, 0.10f),
        laketZ = o(0.38f, 0.16f), rukaZ = o(0.42f, 0.10f),
    )

    private val klikHore = Poza(
        hlava = o(0.84f, 0.38f), krk = o(0.75f, 0.42f), bok = o(0.48f, 0.52f),
        kolenoP = o(0.31f, 0.59f), chodidloP = o(0.13f, 0.66f),
        kolenoZ = o(0.29f, 0.62f), chodidloZ = o(0.11f, 0.69f),
        laketP = o(0.73f, 0.57f), rukaP = o(0.72f, 0.72f),
        laketZ = o(0.77f, 0.58f), rukaZ = o(0.76f, 0.73f),
    )

    private val klikDole = Poza(
        hlava = o(0.85f, 0.55f), krk = o(0.75f, 0.58f), bok = o(0.47f, 0.63f),
        kolenoP = o(0.30f, 0.67f), chodidloP = o(0.13f, 0.71f),
        kolenoZ = o(0.28f, 0.69f), chodidloZ = o(0.11f, 0.73f),
        laketP = o(0.82f, 0.68f), rukaP = o(0.72f, 0.74f),
        laketZ = o(0.86f, 0.69f), rukaZ = o(0.76f, 0.75f),
    )

    private val drep = Poza(
        hlava = o(0.57f, 0.36f), krk = o(0.55f, 0.45f), bok = o(0.42f, 0.63f),
        kolenoP = o(0.58f, 0.69f), chodidloP = o(0.55f, 0.90f),
        kolenoZ = o(0.54f, 0.71f), chodidloZ = o(0.51f, 0.90f),
        laketP = o(0.64f, 0.50f), rukaP = o(0.74f, 0.48f),
        laketZ = o(0.60f, 0.52f), rukaZ = o(0.70f, 0.50f),
    )

    private val doska = Poza(
        hlava = o(0.83f, 0.42f), krk = o(0.74f, 0.45f), bok = o(0.47f, 0.55f),
        kolenoP = o(0.30f, 0.61f), chodidloP = o(0.12f, 0.68f),
        kolenoZ = o(0.28f, 0.63f), chodidloZ = o(0.10f, 0.70f),
        laketP = o(0.72f, 0.60f), rukaP = o(0.80f, 0.72f),
        laketZ = o(0.76f, 0.61f), rukaZ = o(0.84f, 0.73f),
    )

    private val medvediaChodzaA = Poza(
        hlava = o(0.80f, 0.40f), krk = o(0.71f, 0.44f), bok = o(0.44f, 0.46f),
        kolenoP = o(0.40f, 0.62f), chodidloP = o(0.46f, 0.78f),
        kolenoZ = o(0.28f, 0.60f), chodidloZ = o(0.20f, 0.78f),
        laketP = o(0.74f, 0.58f), rukaP = o(0.78f, 0.76f),
        laketZ = o(0.62f, 0.58f), rukaZ = o(0.58f, 0.76f),
    )

    private val medvediaChodzaB = Poza(
        hlava = o(0.80f, 0.40f), krk = o(0.71f, 0.44f), bok = o(0.44f, 0.46f),
        kolenoP = o(0.32f, 0.60f), chodidloP = o(0.24f, 0.78f),
        kolenoZ = o(0.42f, 0.62f), chodidloZ = o(0.50f, 0.78f),
        laketP = o(0.64f, 0.58f), rukaP = o(0.60f, 0.76f),
        laketZ = o(0.76f, 0.58f), rukaZ = o(0.82f, 0.76f),
    )

    private val zabaDole = Poza(
        hlava = o(0.52f, 0.50f), krk = o(0.50f, 0.58f), bok = o(0.44f, 0.72f),
        kolenoP = o(0.58f, 0.74f), chodidloP = o(0.54f, 0.90f),
        kolenoZ = o(0.52f, 0.76f), chodidloZ = o(0.48f, 0.90f),
        laketP = o(0.54f, 0.70f), rukaP = o(0.56f, 0.86f),
        laketZ = o(0.46f, 0.72f), rukaZ = o(0.44f, 0.86f),
    )

    private val zabaVyskok = Poza(
        hlava = o(0.52f, 0.10f), krk = o(0.51f, 0.20f), bok = o(0.48f, 0.44f),
        kolenoP = o(0.55f, 0.58f), chodidloP = o(0.50f, 0.70f),
        kolenoZ = o(0.45f, 0.58f), chodidloZ = o(0.42f, 0.70f),
        laketP = o(0.60f, 0.14f), rukaP = o(0.64f, 0.04f),
        laketZ = o(0.42f, 0.14f), rukaZ = o(0.38f, 0.04f),
    )

    private val mapa: Map<String, PozyCviku> = buildMap {
        put("kliky", PozyCviku(klikHore, klikDole))
        put("kliky-do-stojky", PozyCviku(
            // pike: zadok hore
            Poza(
                hlava = o(0.70f, 0.52f), krk = o(0.64f, 0.48f), bok = o(0.46f, 0.30f),
                kolenoP = o(0.34f, 0.52f), chodidloP = o(0.26f, 0.74f),
                kolenoZ = o(0.32f, 0.54f), chodidloZ = o(0.24f, 0.76f),
                laketP = o(0.68f, 0.60f), rukaP = o(0.70f, 0.74f),
                laketZ = o(0.72f, 0.61f), rukaZ = o(0.74f, 0.75f),
            ),
            Poza(
                hlava = o(0.72f, 0.66f), krk = o(0.66f, 0.58f), bok = o(0.46f, 0.32f),
                kolenoP = o(0.34f, 0.53f), chodidloP = o(0.26f, 0.75f),
                kolenoZ = o(0.32f, 0.55f), chodidloZ = o(0.24f, 0.77f),
                laketP = o(0.76f, 0.70f), rukaP = o(0.70f, 0.75f),
                laketZ = o(0.80f, 0.71f), rukaZ = o(0.74f, 0.76f),
            ),
        ))
        put("tlak-nad-hlavu", PozyCviku(
            stoj.copy(
                laketP = o(0.58f, 0.28f), rukaP = o(0.58f, 0.20f),
                laketZ = o(0.42f, 0.28f), rukaZ = o(0.42f, 0.20f),
            ),
            stoj.copy(
                laketP = o(0.56f, 0.14f), rukaP = o(0.56f, 0.04f),
                laketZ = o(0.44f, 0.14f), rukaZ = o(0.44f, 0.04f),
            ),
        ))
        put("pritahy", PozyCviku(vis, visZhyb, hrazda = true))
        put("muscleup", PozyCviku(
            vis,
            // nad hrazdou v podpore
            Poza(
                hlava = o(0.50f, 0.00f), krk = o(0.50f, 0.08f), bok = o(0.50f, 0.32f),
                kolenoP = o(0.53f, 0.48f), chodidloP = o(0.51f, 0.62f),
                kolenoZ = o(0.47f, 0.48f), chodidloZ = o(0.49f, 0.62f),
                laketP = o(0.58f, 0.14f), rukaP = o(0.58f, 0.10f),
                laketZ = o(0.42f, 0.14f), rukaZ = o(0.42f, 0.10f),
            ),
            hrazda = true,
        ))
        put("vis", PozyCviku(vis, vis.copy(bok = o(0.50f, 0.58f)), hrazda = true))
        put("predlaktia", PozyCviku(vis, vis.copy(bok = o(0.50f, 0.58f)), hrazda = true))
        put("detske-hrazda", PozyCviku(vis, vis.copy(kolenoP = o(0.56f, 0.66f), kolenoZ = o(0.44f, 0.66f)), hrazda = true))
        put("nohy-k-hrazde", PozyCviku(
            vis,
            vis.copy(
                kolenoP = o(0.62f, 0.42f), chodidloP = o(0.68f, 0.28f),
                kolenoZ = o(0.58f, 0.44f), chodidloZ = o(0.64f, 0.30f),
            ),
            hrazda = true,
        ))
        put("veslovanie", PozyCviku(
            // predklon, ruky dole
            Poza(
                hlava = o(0.70f, 0.30f), krk = o(0.63f, 0.34f), bok = o(0.45f, 0.52f),
                kolenoP = o(0.50f, 0.70f), chodidloP = o(0.48f, 0.90f),
                kolenoZ = o(0.44f, 0.70f), chodidloZ = o(0.42f, 0.90f),
                laketP = o(0.64f, 0.48f), rukaP = o(0.65f, 0.62f),
                laketZ = o(0.60f, 0.49f), rukaZ = o(0.61f, 0.63f),
            ),
            Poza(
                hlava = o(0.70f, 0.30f), krk = o(0.63f, 0.34f), bok = o(0.45f, 0.52f),
                kolenoP = o(0.50f, 0.70f), chodidloP = o(0.48f, 0.90f),
                kolenoZ = o(0.44f, 0.70f), chodidloZ = o(0.42f, 0.90f),
                laketP = o(0.60f, 0.42f), rukaP = o(0.66f, 0.44f),
                laketZ = o(0.56f, 0.43f), rukaZ = o(0.62f, 0.45f),
            ),
        ))
        put("zadne-delty", PozyCviku(
            Poza(
                hlava = o(0.68f, 0.28f), krk = o(0.62f, 0.33f), bok = o(0.46f, 0.52f),
                kolenoP = o(0.50f, 0.70f), chodidloP = o(0.48f, 0.90f),
                kolenoZ = o(0.44f, 0.70f), chodidloZ = o(0.42f, 0.90f),
                laketP = o(0.62f, 0.46f), rukaP = o(0.62f, 0.58f),
                laketZ = o(0.58f, 0.47f), rukaZ = o(0.58f, 0.59f),
            ),
            Poza(
                hlava = o(0.68f, 0.28f), krk = o(0.62f, 0.33f), bok = o(0.46f, 0.52f),
                kolenoP = o(0.50f, 0.70f), chodidloP = o(0.48f, 0.90f),
                kolenoZ = o(0.44f, 0.70f), chodidloZ = o(0.42f, 0.90f),
                laketP = o(0.74f, 0.36f), rukaP = o(0.84f, 0.32f),
                laketZ = o(0.46f, 0.38f), rukaZ = o(0.36f, 0.34f),
            ),
        ))
        put("drep", PozyCviku(stoj, drep))
        put("detske-chodze", PozyCviku(medvediaChodzaA, medvediaChodzaB))
        put("detske-skoky", PozyCviku(zabaDole, zabaVyskok))
        put("detske-vydrze", PozyCviku(
            // bocian - stoj na jednej nohe
            stoj.copy(kolenoP = o(0.58f, 0.58f), chodidloP = o(0.54f, 0.68f)),
            stoj.copy(
                kolenoP = o(0.60f, 0.56f), chodidloP = o(0.56f, 0.66f),
                laketP = o(0.62f, 0.30f), rukaP = o(0.70f, 0.26f),
                laketZ = o(0.38f, 0.30f), rukaZ = o(0.30f, 0.26f),
            ),
        ))
        put("vypady", PozyCviku(
            stoj,
            Poza(
                hlava = o(0.52f, 0.28f), krk = o(0.51f, 0.38f), bok = o(0.48f, 0.62f),
                kolenoP = o(0.64f, 0.72f), chodidloP = o(0.64f, 0.90f),
                kolenoZ = o(0.38f, 0.82f), chodidloZ = o(0.26f, 0.88f),
                laketP = o(0.55f, 0.50f), rukaP = o(0.56f, 0.60f),
                laketZ = o(0.47f, 0.50f), rukaZ = o(0.46f, 0.60f),
            ),
        ))
        put("sldl", PozyCviku(
            stoj,
            // predklon na jednej nohe, druhá vzad
            Poza(
                hlava = o(0.78f, 0.34f), krk = o(0.70f, 0.37f), bok = o(0.48f, 0.48f),
                kolenoP = o(0.50f, 0.68f), chodidloP = o(0.50f, 0.90f),
                kolenoZ = o(0.30f, 0.44f), chodidloZ = o(0.14f, 0.42f),
                laketP = o(0.68f, 0.50f), rukaP = o(0.68f, 0.62f),
                laketZ = o(0.64f, 0.51f), rukaZ = o(0.64f, 0.63f),
            ),
        ))
        put("most", PozyCviku(
            // ľah, kolená hore
            Poza(
                hlava = o(0.14f, 0.72f), krk = o(0.22f, 0.72f), bok = o(0.48f, 0.74f),
                kolenoP = o(0.62f, 0.56f), chodidloP = o(0.70f, 0.76f),
                kolenoZ = o(0.58f, 0.58f), chodidloZ = o(0.66f, 0.78f),
                laketP = o(0.32f, 0.76f), rukaP = o(0.40f, 0.78f),
                laketZ = o(0.28f, 0.77f), rukaZ = o(0.36f, 0.79f),
            ),
            // boky hore
            Poza(
                hlava = o(0.14f, 0.72f), krk = o(0.22f, 0.70f), bok = o(0.50f, 0.50f),
                kolenoP = o(0.64f, 0.52f), chodidloP = o(0.70f, 0.76f),
                kolenoZ = o(0.60f, 0.54f), chodidloZ = o(0.66f, 0.78f),
                laketP = o(0.32f, 0.76f), rukaP = o(0.40f, 0.78f),
                laketZ = o(0.28f, 0.77f), rukaZ = o(0.36f, 0.79f),
            ),
        ))
        put("vypony", PozyCviku(
            stoj,
            stoj.copy(
                hlava = o(0.50f, 0.08f), krk = o(0.50f, 0.19f), bok = o(0.50f, 0.47f),
                kolenoP = o(0.52f, 0.66f), chodidloP = o(0.52f, 0.84f),
                kolenoZ = o(0.48f, 0.66f), chodidloZ = o(0.48f, 0.84f),
            ),
        ))
        put("poskoky", PozyCviku(
            stoj,
            stoj.copy(
                hlava = o(0.50f, 0.06f), krk = o(0.50f, 0.17f), bok = o(0.50f, 0.45f),
                kolenoP = o(0.54f, 0.60f), chodidloP = o(0.52f, 0.74f),
                kolenoZ = o(0.46f, 0.60f), chodidloZ = o(0.48f, 0.74f),
            ),
        ))
        put("doska", PozyCviku(doska, doska.copy(bok = o(0.47f, 0.53f))))
        put("hollow", PozyCviku(
            // ľah, ruky a nohy mierne nad zemou
            Poza(
                hlava = o(0.16f, 0.62f), krk = o(0.24f, 0.66f), bok = o(0.50f, 0.72f),
                kolenoP = o(0.66f, 0.66f), chodidloP = o(0.80f, 0.60f),
                kolenoZ = o(0.64f, 0.68f), chodidloZ = o(0.78f, 0.62f),
                laketP = o(0.12f, 0.54f), rukaP = o(0.06f, 0.48f),
                laketZ = o(0.14f, 0.56f), rukaZ = o(0.08f, 0.50f),
            ),
            Poza(
                hlava = o(0.16f, 0.58f), krk = o(0.24f, 0.63f), bok = o(0.50f, 0.72f),
                kolenoP = o(0.66f, 0.62f), chodidloP = o(0.82f, 0.54f),
                kolenoZ = o(0.64f, 0.64f), chodidloZ = o(0.80f, 0.56f),
                laketP = o(0.10f, 0.50f), rukaP = o(0.04f, 0.42f),
                laketZ = o(0.12f, 0.52f), rukaZ = o(0.06f, 0.44f),
            ),
        ))
        put("lsit", PozyCviku(
            // sed, ruky opreté, nohy na zemi
            Poza(
                hlava = o(0.42f, 0.36f), krk = o(0.44f, 0.46f), bok = o(0.46f, 0.68f),
                kolenoP = o(0.62f, 0.70f), chodidloP = o(0.78f, 0.74f),
                kolenoZ = o(0.60f, 0.72f), chodidloZ = o(0.76f, 0.76f),
                laketP = o(0.40f, 0.58f), rukaP = o(0.38f, 0.74f),
                laketZ = o(0.36f, 0.59f), rukaZ = o(0.34f, 0.75f),
            ),
            // nohy vodorovne vo vzduchu
            Poza(
                hlava = o(0.42f, 0.30f), krk = o(0.44f, 0.40f), bok = o(0.46f, 0.62f),
                kolenoP = o(0.64f, 0.58f), chodidloP = o(0.82f, 0.56f),
                kolenoZ = o(0.62f, 0.60f), chodidloZ = o(0.80f, 0.58f),
                laketP = o(0.40f, 0.54f), rukaP = o(0.38f, 0.70f),
                laketZ = o(0.36f, 0.55f), rukaZ = o(0.34f, 0.71f),
            ),
        ))
        put("stojka", PozyCviku(
            // stojka pri stene: hore nohami
            Poza(
                hlava = o(0.50f, 0.78f), krk = o(0.50f, 0.68f), bok = o(0.50f, 0.42f),
                kolenoP = o(0.52f, 0.26f), chodidloP = o(0.52f, 0.10f),
                kolenoZ = o(0.48f, 0.26f), chodidloZ = o(0.48f, 0.10f),
                laketP = o(0.57f, 0.80f), rukaP = o(0.58f, 0.90f),
                laketZ = o(0.43f, 0.80f), rukaZ = o(0.42f, 0.90f),
            ),
            Poza(
                hlava = o(0.50f, 0.78f), krk = o(0.50f, 0.68f), bok = o(0.50f, 0.42f),
                kolenoP = o(0.54f, 0.25f), chodidloP = o(0.56f, 0.10f),
                kolenoZ = o(0.46f, 0.25f), chodidloZ = o(0.44f, 0.10f),
                laketP = o(0.57f, 0.80f), rukaP = o(0.58f, 0.90f),
                laketZ = o(0.43f, 0.80f), rukaZ = o(0.42f, 0.90f),
            ),
        ))
        put("rozohriatie", PozyCviku(
            stoj,
            // panák: ruky hore, nohy od seba
            Poza(
                hlava = o(0.50f, 0.10f), krk = o(0.50f, 0.21f), bok = o(0.50f, 0.50f),
                kolenoP = o(0.60f, 0.68f), chodidloP = o(0.66f, 0.88f),
                kolenoZ = o(0.40f, 0.68f), chodidloZ = o(0.34f, 0.88f),
                laketP = o(0.62f, 0.16f), rukaP = o(0.68f, 0.06f),
                laketZ = o(0.38f, 0.16f), rukaZ = o(0.32f, 0.06f),
            ),
        ))
        put("mobilita-ramena", PozyCviku(
            stoj,
            stoj.copy(
                laketP = o(0.56f, 0.12f), rukaP = o(0.54f, 0.02f),
                laketZ = o(0.44f, 0.12f), rukaZ = o(0.46f, 0.02f),
            ),
        ))
        put("mobilita-kompresia", PozyCviku(
            // sed, nohy vystreté, predklon
            Poza(
                hlava = o(0.36f, 0.42f), krk = o(0.40f, 0.50f), bok = o(0.44f, 0.70f),
                kolenoP = o(0.60f, 0.70f), chodidloP = o(0.78f, 0.72f),
                kolenoZ = o(0.58f, 0.72f), chodidloZ = o(0.76f, 0.74f),
                laketP = o(0.48f, 0.56f), rukaP = o(0.56f, 0.62f),
                laketZ = o(0.44f, 0.57f), rukaZ = o(0.52f, 0.63f),
            ),
            Poza(
                hlava = o(0.48f, 0.52f), krk = o(0.48f, 0.58f), bok = o(0.44f, 0.70f),
                kolenoP = o(0.60f, 0.70f), chodidloP = o(0.78f, 0.72f),
                kolenoZ = o(0.58f, 0.72f), chodidloZ = o(0.76f, 0.74f),
                laketP = o(0.58f, 0.62f), rukaP = o(0.70f, 0.66f),
                laketZ = o(0.54f, 0.63f), rukaZ = o(0.66f, 0.67f),
            ),
        ))
        put("mobilita-clenky", PozyCviku(drep, drep.copy(bok = o(0.40f, 0.66f))))
        put("mobilita-zapastia", PozyCviku(
            // kľak, dlane na zemi
            Poza(
                hlava = o(0.66f, 0.34f), krk = o(0.60f, 0.40f), bok = o(0.40f, 0.56f),
                kolenoP = o(0.36f, 0.74f), chodidloP = o(0.20f, 0.76f),
                kolenoZ = o(0.34f, 0.76f), chodidloZ = o(0.18f, 0.78f),
                laketP = o(0.60f, 0.56f), rukaP = o(0.62f, 0.74f),
                laketZ = o(0.64f, 0.57f), rukaZ = o(0.66f, 0.75f),
            ),
            Poza(
                hlava = o(0.62f, 0.32f), krk = o(0.57f, 0.38f), bok = o(0.40f, 0.56f),
                kolenoP = o(0.36f, 0.74f), chodidloP = o(0.20f, 0.76f),
                kolenoZ = o(0.34f, 0.76f), chodidloZ = o(0.18f, 0.78f),
                laketP = o(0.56f, 0.54f), rukaP = o(0.56f, 0.74f),
                laketZ = o(0.60f, 0.55f), rukaZ = o(0.60f, 0.75f),
            ),
        ))
    }

    fun pre(rebrikId: String): PozyCviku? = mapa[rebrikId]
}

/**
 * Vykreslí animovanú postavičku pre daný rebrík. Ak rebrík pózy nemá, nekreslí nič
 * (volajúci nech vtedy nechá miesto textu).
 */
@Composable
fun Postavicka(
    rebrikId: String,
    modifier: Modifier = Modifier,
    farba: Color,
    farbaAkcent: Color,
    trvanieMs: Int = 1400,
) {
    val pozy = Pozy.pre(rebrikId) ?: return
    val prechod = rememberInfiniteTransition(label = "postavicka")
    val t by prechod.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(trvanieMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "t",
    )

    Canvas(modifier = modifier) {
        val p = zmiesaj(pozy.a, pozy.b, t)
        val w = size.width
        val h = size.height
        fun s(o: Offset) = Offset(o.x * w, o.y * h)
        val hrubka = (w * 0.035f).coerceAtLeast(6f)
        val styl = Stroke(width = hrubka, cap = StrokeCap.Round)

        if (pozy.hrazda) {
            drawLine(farbaAkcent, Offset(w * 0.18f, h * 0.10f), Offset(w * 0.82f, h * 0.10f), hrubka * 0.8f, StrokeCap.Round)
        }

        // trup
        drawLine(farba, s(p.krk), s(p.bok), hrubka, StrokeCap.Round)
        // nohy
        drawLine(farba, s(p.bok), s(p.kolenoZ), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.kolenoZ), s(p.chodidloZ), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.bok), s(p.kolenoP), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.kolenoP), s(p.chodidloP), hrubka, StrokeCap.Round)
        // ruky
        drawLine(farba, s(p.krk), s(p.laketZ), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.laketZ), s(p.rukaZ), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.krk), s(p.laketP), hrubka, StrokeCap.Round)
        drawLine(farba, s(p.laketP), s(p.rukaP), hrubka, StrokeCap.Round)
        // hlava
        drawCircle(farba, radius = w * 0.055f, center = s(p.hlava), style = styl)
    }
}
