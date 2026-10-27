package sk.havrila.cvicime.ui.komponenty.pohyb

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Kinematika postavičky - čistý Kotlin bez Androidu, aby sa dala testovať.
 *
 * Svet: y hore, podlaha y = 0, postava z boku pozerá doprava (+x), výška v stoji ~1.0.
 * Uhly segmentov sú v stupňoch v súradniciach sveta: 0 = dopredu, 90 = hore, -90 = dole.
 * Póza = koreň (bok) + uhol trupu + končatiny. Končatina je zadaná uhlami, cieľom pre
 * 2-kĺbovú inverznú kinematiku (dlaň na zemi, ruka na hrazde) alebo smerom prvého segmentu
 * na bod (koleno na zemi). Dĺžky segmentov sú pevné, takže sa končatiny nikdy nenaťahujú.
 *
 * Rovnakú matematiku má tools/pohyby_nahlad.py (kontaktný hárok) - pri zmene upraviť oboje.
 */

data class Bod(val x: Double, val y: Double)

/** Pevné dĺžky segmentov (sekcia "kostra" v pohyby.json). */
data class Rozmery(
    val trup: Double = 0.30,
    val krk: Double = 0.11,
    val hlava: Double = 0.065,
    val stehno: Double = 0.245,
    val holen: Double = 0.245,
    val chodidlo: Double = 0.07,
    val rameno: Double = 0.18,
    val predlaktie: Double = 0.16,
    val dlan: Double = 0.05,
    /** polovičná šírka panvy pri pohľade spredu */
    val panva: Double = 0.07,
    /** polovičná šírka pliec pri pohľade spredu */
    val plecia: Double = 0.13,
)

/** Štyri končatiny: noha/ruka, P = bližšia k divákovi (spredu: pravá strana obrázka). */
enum class Clen(val noha: Boolean, val blizka: Boolean) {
    NP(true, true), NZ(true, false), RP(false, true), RZ(false, false)
}

sealed interface Koncatina {
    /** Uhly prvého a druhého segmentu, voliteľne chodidla/dlane. */
    data class Uhly(val u1: Double, val u2: Double, val u3: Double? = null) : Koncatina

    /** Koniec (členok/zápästie) na bod, kĺb na strane [o] (+1 = vľavo od smeru koreň→cieľ). */
    data class Ik(val x: Double, val y: Double, val o: Int? = null, val c: Double? = null) : Koncatina

    /** Prvý segment mieri na bod (napr. koleno na zem), druhý má uhol [u2]. */
    data class Na(val x: Double, val y: Double, val u2: Double, val c: Double? = null) : Koncatina
}

data class Poza(
    val bok: Bod,
    val trup: Double,
    val hlava: Double,
    val chrbat: Double,
    val lopatky: Double,
    val koncatiny: Map<Clen, Koncatina>,
    /** ms presunu z tejto pózy do nasledujúcej */
    val t: Int,
    /** ms výdrže v tejto póze pred presunom */
    val drz: Int,
) {
    operator fun get(c: Clen): Koncatina = koncatiny.getValue(c)
}

enum class Pohlad { BOK, PRED, ZHORA }

data class Rekvizita(val typ: String, val cisla: Map<String, Double> = emptyMap(), val bod: String? = null) {
    fun c(kluc: String, inak: Double = 0.0): Double = cisla[kluc] ?: inak
    fun ma(kluc: String): Boolean = kluc in cisla
}

/** Body jednej končatiny: koreň (bok/rameno), kĺb (koleno/lakeť), koniec (členok/zápästie), špička/prsty. */
data class Retaz(val koren: Bod, val klb: Bod, val koniec: Bod, val spicka: Bod)

data class Telo(val bok: Bod, val krk: Bod, val hlava: Bod, val koncatiny: Map<Clen, Retaz>) {
    operator fun get(c: Clen): Retaz = koncatiny.getValue(c)
}

/** Štvorcový výrez sveta. */
data class Ramec(val cx: Double, val cy: Double, val strana: Double)

class Vzor(
    val id: String,
    val nazov: String,
    val pohlad: Pohlad,
    /** "opakovanie" alebo "vydrz" */
    val druh: String,
    /** posun zeme za cyklus (chôdze, plazenie): kladný = postava ide dopredu */
    val posun: Double,
    val linearna: Boolean,
    val dych: Boolean,
    val priemetTrupu: Double,
    oko: Boolean,
    val rekvizity: List<Rekvizita>,
    val pozy: List<Poza>,
    val rozmery: Rozmery,
) {
    val zboku: Boolean get() = pohlad == Pohlad.BOK
    val oko: Boolean = oko && pohlad == Pohlad.BOK
    val cyklus: Int = max(1, pozy.sumOf { it.drz + it.t })
    val ramec: Ramec by lazy { Kinematika.ramec(this) }
}

object Kinematika {

    fun smer(uhol: Double): Bod {
        val r = Math.toRadians(uhol)
        return Bod(cos(r), sin(r))
    }

    fun posun(b: Bod, uhol: Double, dlzka: Double): Bod {
        val s = smer(uhol)
        return Bod(b.x + s.x * dlzka, b.y + s.y * dlzka)
    }

    fun uholK(a: Bod, b: Bod): Double = Math.toDegrees(atan2(b.y - a.y, b.x - a.x))

    fun vzdialenost(a: Bod, b: Bod): Double = hypot(b.x - a.x, b.y - a.y)

    /** 2-kĺbová IK. Nedosiahnuteľný cieľ = končatina vystretá smerom k nemu (dĺžky ostávajú). */
    fun ik(koren: Bod, ciel: Bod, l1: Double, l2: Double, o: Int): Pair<Double, Double> {
        val dx = ciel.x - koren.x
        val dy = ciel.y - koren.y
        val d = min(max(hypot(dx, dy), abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
        val zaklad = Math.toDegrees(atan2(dy, dx))
        val cosA = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)
        val a = Math.toDegrees(acos(cosA.coerceIn(-1.0, 1.0)))
        val u1 = zaklad + (if (o >= 0) 1 else -1) * a
        val klb = posun(koren, u1, l1)
        return u1 to uholK(klb, ciel)
    }

    /** Uhol posunutý o násobok 360 čo najbližšie k [k] (interpolácia nepôjde dlhou cestou). */
    fun najblizsi(uhol: Double, k: Double): Double {
        var u = uhol
        while (u - k > 180) u -= 360
        while (u - k < -180) u += 360
        return u
    }

    private fun dlzky(v: Vzor, c: Clen): Pair<Double, Double> =
        if (c.noha) v.rozmery.stehno to v.rozmery.holen else v.rozmery.rameno to v.rozmery.predlaktie

    private fun oPre(c: Clen, k: Koncatina.Ik): Int = k.o ?: if (c.noha) 1 else -1

    fun krk(v: Vzor, bok: Bod, trup: Double): Bod = posun(bok, trup, v.rozmery.trup * v.priemetTrupu)

    fun koren(v: Vzor, c: Clen, bok: Bod, trup: Double, lopatky: Double): Bod {
        val zaklad = if (c.noha) bok else posun(bok, trup, v.rozmery.trup * v.priemetTrupu + lopatky)
        if (v.zboku) return zaklad
        val strana = if (c.blizka) 1 else -1
        return if (c.noha) {
            Bod(zaklad.x + strana * v.rozmery.panva, zaklad.y)
        } else {
            Bod(zaklad.x + strana * v.rozmery.plecia, zaklad.y - 0.03)
        }
    }

    /** Končatina -> uhly (u1, u2, u3 alebo null). */
    fun vyries(v: Vzor, c: Clen, k: Koncatina, koren: Bod): Triple<Double, Double, Double?> {
        val (l1, l2) = dlzky(v, c)
        return when (k) {
            is Koncatina.Uhly -> Triple(k.u1, k.u2, k.u3)
            is Koncatina.Ik -> {
                val (u1, u2) = ik(koren, Bod(k.x, k.y), l1, l2, oPre(c, k))
                Triple(u1, u2, k.c)
            }
            is Koncatina.Na -> Triple(uholK(koren, Bod(k.x, k.y)), k.u2, k.c)
        }
    }

    /** Chodidlo kolmo na holeň dopredu, dlaň v predĺžení predlaktia. */
    fun predvolenyKoniec(v: Vzor, c: Clen, u2: Double): Double =
        if (c.noha) (if (v.zboku) u2 + 90 else -90.0) else u2

    // ------------------------------------------------------------------ čas

    fun pozaVCase(v: Vzor, cas: Double): Poza {
        val n = v.pozy.size
        var c = ((cas % v.cyklus) + v.cyklus) % v.cyklus
        for ((i, p) in v.pozy.withIndex()) {
            if (c < p.drz) return zmiesaj(v, p, p, 0.0)
            c -= p.drz
            if (c < p.t || i == n - 1) {
                val u = if (p.t <= 0) 0.0 else min(1.0, c / p.t)
                return zmiesaj(v, p, v.pozy[(i + 1) % n], krivka(v, u))
            }
            c -= p.t
        }
        return v.pozy.first()
    }

    private fun krivka(v: Vzor, u: Double): Double = if (v.linearna) u else u * u * (3 - 2 * u)

    fun zmiesaj(v: Vzor, a: Poza, b: Poza, e: Double): Poza {
        fun m(x: Double, y: Double) = x + (y - x) * e
        fun mn(x: Double?, y: Double?) = if (x != null && y != null) m(x, y) else null
        val konc = Clen.entries.associateWith { c ->
            val ka = a[c]
            val kb = b[c]
            when {
                ka is Koncatina.Ik && kb is Koncatina.Ik ->
                    Koncatina.Ik(m(ka.x, kb.x), m(ka.y, kb.y), oPre(c, ka), mn(ka.c, kb.c))
                ka is Koncatina.Na && kb is Koncatina.Na ->
                    Koncatina.Na(m(ka.x, kb.x), m(ka.y, kb.y), m(ka.u2, kb.u2), mn(ka.c, kb.c))
                else -> {
                    val ra = vyries(v, c, ka, koren(v, c, a.bok, a.trup, a.lopatky))
                    val rb = vyries(v, c, kb, koren(v, c, b.bok, b.trup, b.lopatky))
                    var a1 = ra.first
                    var a2 = ra.second
                    var b1 = rb.first
                    var b2 = rb.second
                    if (ka !is Koncatina.Uhly) {
                        a1 = najblizsi(a1, b1)
                        a2 = najblizsi(a2, b2)
                    } else if (kb !is Koncatina.Uhly) {
                        b1 = najblizsi(b1, a1)
                        b2 = najblizsi(b2, a2)
                    }
                    val a3 = ra.third ?: predvolenyKoniec(v, c, a2)
                    val b3 = rb.third ?: predvolenyKoniec(v, c, b2)
                    Koncatina.Uhly(m(a1, b1), m(a2, b2), m(a3, b3))
                }
            }
        }
        return Poza(
            bok = Bod(m(a.bok.x, b.bok.x), m(a.bok.y, b.bok.y)),
            trup = m(a.trup, b.trup),
            hlava = m(a.hlava, b.hlava),
            chrbat = m(a.chrbat, b.chrbat),
            lopatky = m(a.lopatky, b.lopatky),
            koncatiny = konc,
            t = a.t,
            drz = 0,
        )
    }

    /** Jemné dýchanie pri výdržiach. */
    fun dych(v: Vzor, p: Poza, cas: Double): Poza {
        if (!v.dych) return p
        val s = sin(2 * Math.PI * cas / 4000.0)
        return p.copy(trup = p.trup + 1.2 * s, bok = Bod(p.bok.x, p.bok.y + 0.004 * s))
    }

    // ------------------------------------------------------------------ dopredná kinematika

    fun telo(v: Vzor, p: Poza): Telo {
        val r = v.rozmery
        val krk = krk(v, p.bok, p.trup)
        val hlava = posun(krk, p.hlava, r.krk)
        val konc = Clen.entries.associateWith { c ->
            val (l1, l2) = dlzky(v, c)
            val koren = koren(v, c, p.bok, p.trup, p.lopatky)
            val (u1, u2, u3n) = vyries(v, c, p[c], koren)
            val u3 = u3n ?: predvolenyKoniec(v, c, u2)
            val klb = posun(koren, u1, l1)
            val koniec = posun(klb, u2, l2)
            Retaz(koren, klb, koniec, posun(koniec, u3, if (c.noha) r.chodidlo else r.dlan))
        }
        return Telo(p.bok, krk, hlava, konc)
    }

    fun telo(v: Vzor, cas: Double): Telo = telo(v, dych(v, pozaVCase(v, cas), cas))

    // ------------------------------------------------------------------ rámec

    private fun bodyRekvizit(v: Vzor): List<Bod> = buildList {
        for (r in v.rekvizity) {
            when (r.typ) {
                "stena" -> {
                    add(Bod(r.c("x"), r.c("y1", 0.0)))
                    add(Bod(r.c("x"), r.c("y2", 0.9)))
                }
                "stolicka" -> {
                    add(Bod(r.c("x") - 0.13, 0.0))
                    add(Bod(r.c("x") + 0.13, 0.26 + if (r.c("operadlo") != 0.0) 0.24 else 0.0))
                }
                "schod" -> {
                    add(Bod(r.c("x1"), 0.0))
                    add(Bod(r.c("x2"), r.c("h", 0.1)))
                }
                "hrazda" -> {
                    add(Bod(r.c("x"), r.c("y") + 0.03))
                    add(Bod(r.c("x"), r.c("y") - 0.03))
                }
                "bradla" -> {
                    add(Bod(r.c("x1"), r.c("y")))
                    add(Bod(r.c("x2"), r.c("y")))
                }
                "kruhy" -> add(Bod(r.c("x"), r.c("y") + 0.12))
                "podlozka" -> {
                    add(Bod(r.c("x1"), 0.0))
                    add(Bod(r.c("x2"), 0.0))
                }
            }
        }
    }

    /** Výrez cez 24 vzoriek cyklu (bez dýchania) + rekvizity, štvorec s okrajom 12 %. */
    fun ramec(v: Vzor): Ramec {
        val xs = ArrayList<Double>()
        val ys = ArrayList<Double>()
        val vzorky = 24
        for (i in 0 until vzorky) {
            val t = telo(v, pozaVCase(v, v.cyklus.toDouble() * i / vzorky))
            for (b in listOf(t.bok, t.krk, t.hlava)) {
                xs += b.x
                ys += b.y
            }
            for (r in t.koncatiny.values) {
                for (b in listOf(r.koren, r.klb, r.koniec, r.spicka)) {
                    xs += b.x
                    ys += b.y
                }
            }
            val h = v.rozmery.hlava
            xs += t.hlava.x - h
            xs += t.hlava.x + h
            ys += t.hlava.y - h
            ys += t.hlava.y + h
        }
        if (v.rekvizity.any { it.typ == "zem" }) ys += -0.03
        for (b in bodyRekvizit(v)) {
            xs += b.x
            ys += b.y
        }
        val x0 = xs.min()
        val x1 = xs.max()
        val y0 = ys.min()
        val y1 = ys.max()
        val strana = max(max(x1 - x0, y1 - y0), 1.0) * 1.12
        return Ramec((x0 + x1) / 2, (y0 + y1) / 2, strana)
    }
}
