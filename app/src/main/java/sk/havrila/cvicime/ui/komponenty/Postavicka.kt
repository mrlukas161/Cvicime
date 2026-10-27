package sk.havrila.cvicime.ui.komponenty

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import sk.havrila.cvicime.ui.komponenty.pohyb.Bod
import sk.havrila.cvicime.ui.komponenty.pohyb.Clen
import sk.havrila.cvicime.ui.komponenty.pohyb.Kinematika
import sk.havrila.cvicime.ui.komponenty.pohyb.Pohyby
import sk.havrila.cvicime.ui.komponenty.pohyb.Vzor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Animovaná ilustrácia cviku: postavička s pevnými dĺžkami kostí, kreslená na Canvas.
 *
 * Pohyb je na CVIK (pole `pohyb` v katalógu) - vzory sú dáta v assets/pohyby.json,
 * kinematika v [Kinematika]. Zámerne vektor a nie obrázky: žiadne licenčné riziko,
 * jednotný vzhľad, nulová veľkosť APK navyše.
 */
object Pozy {
    /**
     * Predvolený pohyb rebríka (id vzoru) alebo null. Zachované kvôli starším volaniam
     * `Pozy.pre(rebrikId) != null`; na konkrétny cvik použi `Pohyby.maPohyb(context, cvik.pohyb)`.
     */
    fun pre(rebrikId: String): String? = Pohyby.predvolenyPre(rebrikId)
}

/**
 * Vykreslí animovanú postavičku. Ak je zadaný [pohyb] (id vzoru z katalógu cviku), kreslí ten,
 * inak predvolený pohyb rebríka. Ak nie je ani jeden, nekreslí nič.
 *
 * @param trvanieMs tempo: 1400 = tempo zo vzoru, väčšie číslo = pomalšie (zachované kvôli kompatibilite).
 */
@Composable
fun Postavicka(
    rebrikId: String,
    modifier: Modifier = Modifier,
    farba: Color,
    farbaAkcent: Color,
    trvanieMs: Int = 1400,
    pohyb: String? = null,
) {
    val context = LocalContext.current
    val vzor = remember(pohyb, rebrikId) { Pohyby.vzor(context, pohyb, rebrikId) } ?: return
    PostavickaVzoru(vzor, modifier, farba, farbaAkcent, tempo = trvanieMs / 1400f)
}

/** Postavička pre už načítaný vzor (galéria pohybov). */
@Composable
fun PostavickaVzoru(
    vzor: Vzor,
    modifier: Modifier = Modifier,
    farba: Color,
    farbaAkcent: Color,
    tempo: Float = 1f,
) {
    val prechod = rememberInfiniteTransition(label = "postavicka")
    val cas = prechod.animateFloat(
        initialValue = 0f,
        targetValue = vzor.cyklus.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((vzor.cyklus * tempo).roundToInt().coerceAtLeast(1), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cas",
    )
    // Stav sa číta až pri kreslení - animácia prekresľuje, nerekomponuje.
    Canvas(modifier = modifier) {
        nakresliVzor(vzor, cas.value.toDouble(), farba, farbaAkcent)
    }
}

/** Kreslenie jedného okamihu pohybu. Zodpovedá funkcii kresli() v tools/pohyby_nahlad.py. */
fun DrawScope.nakresliVzor(vzor: Vzor, cas: Double, farba: Color, akcent: Color) {
    val ramec = vzor.ramec
    val strana = min(size.width, size.height)
    if (strana <= 0f) return
    val mierka = strana / ramec.strana.toFloat()
    val sx = size.width / 2f
    val sy = size.height / 2f
    fun p(b: Bod) = Offset(sx + ((b.x - ramec.cx) * mierka).toFloat(), sy - ((b.y - ramec.cy) * mierka).toFloat())
    fun q(x: Double, y: Double) = p(Bod(x, y))
    val hrubka = (strana * 0.035f).coerceAtLeast(2f)
    val tenka = hrubka * 0.7f
    fun ciara(a: Offset, b: Offset, c: Color, h: Float = hrubka, alfa: Float = 1f) =
        drawLine(c, a, b, strokeWidth = h, cap = StrokeCap.Round, alpha = alfa)

    val lavy = ramec.cx - sx / mierka
    val pravy = ramec.cx + sx / mierka
    val cyklus = vzor.cyklus.toDouble()

    // ---- rekvizity za postavou
    for (r in vzor.rekvizity) {
        when (r.typ) {
            "zem" -> {
                ciara(q(lavy, -0.005), q(pravy, -0.005), akcent, tenka, 0.5f)
                if (vzor.posun != 0.0) {
                    val krok = 0.25
                    val faza = (cas / cyklus * vzor.posun).mod(krok)
                    var x = lavy - faza + krok
                    while (x < pravy) {
                        ciara(q(x, -0.04), q(x + 0.06, -0.04), akcent, tenka * 0.8f, 0.45f)
                        x += krok
                    }
                }
            }
            "stena" -> ciara(q(r.c("x"), r.c("y1", 0.0)), q(r.c("x"), r.c("y2", 1.3)), akcent, tenka * 1.2f, 0.7f)
            "stolicka" -> {
                val x = r.c("x")
                val sed = 0.26
                ciara(q(x - 0.12, sed), q(x + 0.12, sed), akcent, tenka)
                ciara(q(x - 0.11, sed), q(x - 0.11, 0.0), akcent, tenka * 0.8f)
                ciara(q(x + 0.11, sed), q(x + 0.11, 0.0), akcent, tenka * 0.8f)
                val op = r.c("operadlo")
                if (op != 0.0) {
                    val xo = x + 0.11 * op
                    ciara(q(xo, sed), q(xo, sed + 0.24), akcent, tenka)
                }
            }
            "schod" -> {
                val h = r.c("h", 0.1)
                val x1 = r.c("x1")
                val x2 = r.c("x2")
                ciara(q(x1, h), q(x2, h), akcent, tenka)
                ciara(q(x1, h), q(x1, 0.0), akcent, tenka * 0.8f)
                ciara(q(x2, h), q(x2, 0.0), akcent, tenka * 0.8f)
            }
            "bradla" -> {
                val y = r.c("y")
                ciara(q(r.c("x1"), y), q(r.c("x2"), y), akcent, tenka)
                ciara(q(r.c("x1") + 0.04, y), q(r.c("x1") + 0.04, 0.0), akcent, tenka * 0.7f, 0.7f)
                ciara(q(r.c("x2") - 0.04, y), q(r.c("x2") - 0.04, 0.0), akcent, tenka * 0.7f, 0.7f)
            }
            "kruhy" -> ciara(q(r.c("x"), r.c("y") + 0.04), q(r.c("x"), r.c("y") + 0.6), akcent, tenka * 0.5f, 0.7f)
            "podlozka" -> ciara(q(r.c("x1"), 0.012), q(r.c("x2"), 0.012), akcent, tenka * 1.4f, 0.35f)
            "hrazda" -> {
                // z boku: prierez tyče (bodka) + jemný náznak tyče do hĺbky; spredu celá tyč
                val dl = r.c("dlzka", 0.28)
                ciara(q(r.c("x") - dl / 2, r.c("y")), q(r.c("x") + dl / 2, r.c("y")), akcent, tenka,
                    if (r.ma("dlzka")) 1f else 0.35f)
            }
        }
    }

    // ---- postava
    val poza = Kinematika.dych(vzor, Kinematika.pozaVCase(vzor, cas), cas)
    val telo = Kinematika.telo(vzor, poza)
    fun koncatina(c: Clen, alfa: Float) {
        val k = telo[c]
        ciara(p(k.koren), p(k.klb), farba, alfa = alfa)
        ciara(p(k.klb), p(k.koniec), farba, alfa = alfa)
        if (vzor.zboku || !c.noha) ciara(p(k.koniec), p(k.spicka), farba, hrubka * 0.8f, alfa)
    }
    val zadne = if (vzor.zboku) 0.5f else 1f
    koncatina(Clen.NZ, zadne)
    koncatina(Clen.RZ, zadne)
    if (abs(poza.chrbat) > 1e-4) {
        val s = Kinematika.smer(poza.trup + 90)
        val stred = Bod(
            (telo.bok.x + telo.krk.x) / 2 + s.x * poza.chrbat * 2,
            (telo.bok.y + telo.krk.y) / 2 + s.y * poza.chrbat * 2,
        )
        val a = p(telo.bok)
        val k = p(stred)
        val b = p(telo.krk)
        val cesta = Path().apply {
            moveTo(a.x, a.y)
            quadraticTo(k.x, k.y, b.x, b.y)
        }
        drawPath(cesta, farba, style = Stroke(width = hrubka, cap = StrokeCap.Round))
    } else {
        ciara(p(telo.bok), p(telo.krk), farba)
    }
    if (vzor.zboku) {
        if (poza.lopatky > 0) ciara(p(telo.krk), p(telo[Clen.RP].koren), farba)
    } else {
        ciara(p(telo[Clen.NP].koren), p(telo[Clen.NZ].koren), farba)
        ciara(p(telo[Clen.RP].koren), p(telo[Clen.RZ].koren), farba)
    }
    val r = vzor.rozmery
    ciara(p(telo.krk), p(Kinematika.posun(telo.krk, poza.hlava, r.krk - r.hlava)), farba)
    drawCircle(farba, radius = (r.hlava * mierka).toFloat(), center = p(telo.hlava), style = Stroke(width = hrubka * 0.9f))
    if (vzor.oko) {
        val oko = Kinematika.posun(Kinematika.posun(telo.hlava, poza.hlava - 90, r.hlava * 0.45), poza.hlava, r.hlava * 0.15)
        drawCircle(farba, radius = (r.hlava * 0.16 * mierka).toFloat(), center = p(oko))
    }
    koncatina(Clen.NP, 1f)
    koncatina(Clen.RP, 1f)

    // ---- rekvizity pred postavou
    for (rk in vzor.rekvizity) {
        when (rk.typ) {
            "hrazda" -> if (!rk.ma("dlzka")) {
                drawCircle(akcent, radius = (0.03 * mierka).toFloat(), center = q(rk.c("x"), rk.c("y")))
            }
            "kruhy" -> drawCircle(akcent, radius = (0.045 * mierka).toFloat(), center = q(rk.c("x"), rk.c("y")),
                style = Stroke(width = tenka))
            "jednorucka" -> {
                val c = when (val bod = rk.bod ?: "rP") {
                    "bok" -> telo.bok
                    "nP" -> telo[Clen.NP].koniec
                    "nZ" -> telo[Clen.NZ].koniec
                    "rZ" -> telo[Clen.RZ].spicka
                    else -> telo[Clen.RP].spicka
                }
                drawCircle(akcent, radius = (0.042 * mierka).toFloat(), center = p(c))
            }
            "uterak" -> ciara(q(rk.c("x"), rk.c("y")), q(rk.c("x"), rk.c("y2")), akcent, tenka * 0.9f)
            "taska" -> {
                val c = telo[Clen.RP].spicka
                ciara(q(c.x - 0.06, c.y - 0.06), q(c.x + 0.06, c.y - 0.06), akcent, hrubka * 2.2f)
            }
            "pulz" -> {
                val faza = (0.5 - 0.5 * cos(2 * PI * (cas % cyklus) / cyklus)).toFloat()
                drawCircle(akcent, radius = ((0.035 + 0.035 * faza) * mierka).toFloat(), center = p(telo.bok),
                    alpha = 0.35f + 0.4f * faza)
            }
            "ciara" -> ciara(q(rk.c("x"), 0.0), q(rk.c("x"), 0.05), akcent, tenka, 0.8f)
        }
    }
}
