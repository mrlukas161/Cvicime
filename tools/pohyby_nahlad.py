#!/usr/bin/env python3
"""
Kontaktný hárok pohybov: načíta app/src/main/assets/pohyby.json, prepočíta pózy
ROVNAKOU kinematikou ako appka (ui/komponenty/pohyb/Kostra.kt) a zapíše HTML s SVG
- každý vzor jeden riadok: kľúčové pózy + medzipózy + cviky, ktoré ho používajú (s pokynmi),
aby sa dalo bez telefónu skontrolovať, či pohyb sedí s popisom.

Použitie:
    python3 tools/pohyby_nahlad.py [--vystup build/pohyby-nahlad.html] [--len klik,drep]

Bez závislostí (len štandardná knižnica). Ak treba PNG, otvor HTML v prehliadači
alebo ho vyfoť headless Chromiom (--screenshot).

Konvencie (musia sedieť s Kotlinom):
- svet: y hore, podlaha y = 0, postava pozerá doprava (+x), výška postoja ~1.0,
- uhly segmentov v stupňoch v súradniciach sveta: 0 = dopredu, 90 = hore, -90 = dole,
- končatina = [uhol1, uhol2(, uhol3)] alebo {"ik": [x, y], "o": +1/-1, "c": uhol3}
  (2-kĺbová inverzná kinematika na cieľ; o = +1 kĺb vľavo od smeru koreň→cieľ),
- dĺžky segmentov sú pevné (sekcia "kostra"), takže sa končatiny nikdy nenaťahujú.
"""
import argparse
import html
import json
import math
import os
import sys

KOREN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
POHYBY = os.path.join(KOREN, "app", "src", "main", "assets", "pohyby.json")
KATALOG = os.path.join(KOREN, "app", "src", "main", "assets", "katalog.json")

KONCATINY = ("nP", "nZ", "rP", "rZ")


# ------------------------------------------------------------------ kinematika

def smer(uhol):
    r = math.radians(uhol)
    return math.cos(r), math.sin(r)


def posun(bod, uhol, dlzka):
    dx, dy = smer(uhol)
    return bod[0] + dx * dlzka, bod[1] + dy * dlzka


def uhol_k(a, b):
    return math.degrees(math.atan2(b[1] - a[1], b[0] - a[0]))


def ik(koren, ciel, l1, l2, o):
    """2-kĺbová IK. Vráti (uhol1, uhol2). Nedosiahnuteľný cieľ = končatina vystretá k nemu."""
    dx, dy = ciel[0] - koren[0], ciel[1] - koren[1]
    d = math.hypot(dx, dy)
    dmin = abs(l1 - l2) + 1e-4
    dmax = l1 + l2 - 1e-4
    d = min(max(d, dmin), dmax)
    zaklad = math.degrees(math.atan2(dy, dx))
    cos_a = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)
    a = math.degrees(math.acos(min(1.0, max(-1.0, cos_a))))
    u1 = zaklad + (1 if o >= 0 else -1) * a
    klb = posun(koren, u1, l1)
    u2 = uhol_k(klb, ciel)
    return u1, u2


def najblizsi(uhol, k):
    """Uhol posunutý o násobok 360 čo najbližšie k uhlu k (aby interpolácia nešla dlhou cestou)."""
    while uhol - k > 180:
        uhol -= 360
    while uhol - k < -180:
        uhol += 360
    return uhol


class Vzor:
    """Jeden vzor pohybu. Logika zodpovedá Kotlinu v ui/komponenty/pohyb/Kostra.kt."""

    def __init__(self, d, kostra):
        self.id = d["id"]
        self.nazov = d.get("nazov", self.id)
        self.pohlad = d.get("pohlad", "bok")
        self.druh = d.get("druh", "opakovanie")
        self.rekvizity = d.get("rekvizity", [])
        self.posun = float(d.get("posun", 0.0))
        self.krivka = d.get("krivka", "plynula")
        self.dych = bool(d.get("dych", self.druh == "vydrz"))
        self.priemet = float(d.get("priemetTrupu", 1.0))
        self.oko = bool(d.get("oko", True)) and self.pohlad == "bok"
        self.k = kostra
        self.pozy = [self._normalizuj(p) for p in d["pozy"]]

    @property
    def zboku(self):
        return self.pohlad == "bok"

    # --- doplnenie chýbajúcich častí pózy
    def _normalizuj(self, p):
        p = dict(p)
        p.setdefault("hlava", p["trup"])
        p.setdefault("t", 1000)
        p.setdefault("drz", 0)
        p.setdefault("chrbat", 0.0)
        p.setdefault("lopatky", 0.0)
        for blizka, dalsia in (("nP", "nZ"), ("rP", "rZ")):
            if dalsia not in p:
                p[dalsia] = p[blizka] if self.zboku else self._zrkadlo(p[blizka], p["bok"][0], blizka[0] == "n")
        return p

    @staticmethod
    def _zrkadlo(k, os_x, noha):
        if isinstance(k, dict):
            z = dict(k)
            if "ik" in k:
                z["ik"] = [2 * os_x - k["ik"][0], k["ik"][1]]
                z["o"] = -k.get("o", 1 if noha else -1)
            if "na" in k:
                z["na"] = [2 * os_x - k["na"][0], k["na"][1]]
                z["u2"] = 180 - k["u2"]
            if "c" in k:
                z["c"] = 180 - k["c"]
            return z
        return [180 - u for u in k]

    # --- dĺžky a korene
    def dlzky(self, meno):
        k = self.k
        return (k["stehno"], k["holen"]) if meno[0] == "n" else (k["rameno"], k["predlaktie"])

    def krk_bod(self, bok, trup):
        return posun(bok, trup, self.k["trup"] * self.priemet)

    def koren(self, meno, bok, trup, lopatky):
        if meno[0] == "n":
            zaklad = bok
        else:
            zaklad = posun(bok, trup, self.k["trup"] * self.priemet + lopatky)
        if self.zboku:
            return zaklad
        strana = 1 if meno[1] == "P" else -1
        if meno[0] == "n":
            return zaklad[0] + strana * self.k["panva"], zaklad[1]
        return zaklad[0] + strana * self.k["plecia"], zaklad[1] - 0.03

    @staticmethod
    def o_pre(meno, k):
        return k.get("o", 1 if meno[0] == "n" else -1)

    def vyries(self, meno, k, koren):
        """Končatina -> (uhol1, uhol2, uhol3 alebo None)."""
        l1, l2 = self.dlzky(meno)
        if isinstance(k, dict):
            if "ik" in k:
                u1, u2 = ik(koren, k["ik"], l1, l2, self.o_pre(meno, k))
                return u1, u2, k.get("c")
            return uhol_k(koren, k["na"]), k["u2"], k.get("c")
        return k[0], k[1], (k[2] if len(k) > 2 else None)

    def predvoleny_koniec(self, meno, u2):
        # chodidlo kolmo na holeň dopredu, dlaň v predĺžení predlaktia
        if meno[0] == "n":
            return u2 + 90 if self.zboku else -90
        return u2

    # --- čas
    def cyklus(self):
        return sum(p["drz"] + p["t"] for p in self.pozy)

    def poza_v_case(self, cas):
        """Interpolovaná póza v čase (ms)."""
        n = len(self.pozy)
        c = cas % max(1, self.cyklus())
        for i, p in enumerate(self.pozy):
            if c < p["drz"]:
                return self._zmiesaj(p, p, 0.0)
            c -= p["drz"]
            if c < p["t"] or i == n - 1:
                u = 0.0 if p["t"] <= 0 else min(1.0, c / p["t"])
                return self._zmiesaj(p, self.pozy[(i + 1) % n], self._krivka(u))
            c -= p["t"]
        return self._zmiesaj(self.pozy[0], self.pozy[0], 0.0)

    def _krivka(self, u):
        if self.krivka == "linearna":
            return u
        return u * u * (3 - 2 * u)

    def _zmiesaj(self, a, b, e):
        def m(x, y):
            return x + (y - x) * e

        bok = (m(a["bok"][0], b["bok"][0]), m(a["bok"][1], b["bok"][1]))
        v = {"bok": bok, "trup": m(a["trup"], b["trup"]), "hlava": m(a["hlava"], b["hlava"]),
             "chrbat": m(a["chrbat"], b["chrbat"]), "lopatky": m(a["lopatky"], b["lopatky"])}
        for meno in KONCATINY:
            ka, kb = a[meno], b[meno]
            if isinstance(ka, dict) and isinstance(kb, dict) and ("ik" in ka) == ("ik" in kb):
                kluc = "ik" if "ik" in ka else "na"
                k = {kluc: [m(ka[kluc][0], kb[kluc][0]), m(ka[kluc][1], kb[kluc][1])]}
                if kluc == "ik":
                    k["o"] = self.o_pre(meno, ka)
                else:
                    k["u2"] = m(ka["u2"], kb["u2"])
                if "c" in ka and "c" in kb:
                    k["c"] = m(ka["c"], kb["c"])
                v[meno] = k
                continue
            ua = list(self.vyries(meno, ka, self.koren(meno, a["bok"], a["trup"], a["lopatky"])))
            ub = list(self.vyries(meno, kb, self.koren(meno, b["bok"], b["trup"], b["lopatky"])))
            for j in range(2):
                if isinstance(ka, dict):
                    ua[j] = najblizsi(ua[j], ub[j])
                elif isinstance(kb, dict):
                    ub[j] = najblizsi(ub[j], ua[j])
            u3a = ua[2] if ua[2] is not None else self.predvoleny_koniec(meno, ua[1])
            u3b = ub[2] if ub[2] is not None else self.predvoleny_koniec(meno, ub[1])
            v[meno] = [m(ua[0], ub[0]), m(ua[1], ub[1]), m(u3a, u3b)]
        return v

    def dych_v_case(self, poza, cas):
        if not self.dych:
            return poza
        s = math.sin(2 * math.pi * cas / 4000.0)
        p = dict(poza)
        p["trup"] = poza["trup"] + 1.2 * s
        p["bok"] = (poza["bok"][0], poza["bok"][1] + 0.004 * s)
        return p

    def kostra(self, poza):
        """Body postavy pre pózu (dopredná kinematika z boku)."""
        k = self.k
        bok, trup = poza["bok"], poza["trup"]
        krk = self.krk_bod(bok, trup)
        hlava = posun(krk, poza["hlava"], k["krk"])
        body = {"bok": bok, "krk": krk, "hlava": hlava}
        for meno in KONCATINY:
            l1, l2 = self.dlzky(meno)
            koren = self.koren(meno, bok, trup, poza["lopatky"])
            u1, u2, u3 = self.vyries(meno, poza[meno], koren)
            if u3 is None:
                u3 = self.predvoleny_koniec(meno, u2)
            klb = posun(koren, u1, l1)
            koniec = posun(klb, u2, l2)
            spicka = posun(koniec, u3, k["chodidlo"] if meno[0] == "n" else k["dlan"])
            body[meno] = (koren, klb, koniec, spicka)
        return body

# ------------------------------------------------------------------ rámec a kreslenie

def body_rekvizit(v):
    """Body rekvizít, ktoré sa započítajú do rámca."""
    for r in v.rekvizity:
        t = r["typ"]
        if t == "stena":
            yield r["x"], r.get("y1", 0.0)
            yield r["x"], r.get("y2", 0.9)
        elif t == "stolicka":
            x = r["x"]
            yield x - 0.13, 0.0
            yield x + 0.13, 0.26 + (0.24 if r.get("operadlo", 0) else 0.0)
        elif t == "schod":
            yield r["x1"], 0.0
            yield r["x2"], r.get("h", 0.1)
        elif t == "hrazda":
            yield r["x"], r["y"] + 0.03
            yield r["x"], r["y"] - 0.03
        elif t == "bradla":
            yield r["x1"], r["y"]
            yield r["x2"], r["y"]
        elif t == "kruhy":
            yield r["x"], r["y"] + 0.12
        elif t == "podlozka":
            yield r["x1"], 0.0
            yield r["x2"], 0.0


def ramec(v):
    """Štvorcový výrez sveta (cx, cy, strana) cez všetky kľúčové pózy aj medzipózy."""
    xs, ys = [], []
    cyk = v.cyklus()
    vzorky = 24
    for i in range(vzorky):
        b = v.kostra(v.poza_v_case(cyk * i / vzorky))
        for kluc, hodnota in b.items():
            body = hodnota if kluc in KONCATINY else (hodnota,)
            for (x, y) in body:
                xs.append(x)
                ys.append(y)
        hx, hy = b["hlava"]
        r = v.k["hlava"]
        xs += [hx - r, hx + r]
        ys += [hy - r, hy + r]
    if any(r["typ"] == "zem" for r in v.rekvizity):
        ys.append(-0.03)
    for (x, y) in body_rekvizit(v):
        xs.append(x)
        ys.append(y)
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    strana = max(x1 - x0, y1 - y0, 1.0) * 1.12
    return (x0 + x1) / 2, (y0 + y1) / 2, strana


class Platno:
    """SVG kreslenie v súradniciach sveta do štvorca velkost x velkost."""

    def __init__(self, v, velkost):
        self.v = v
        self.cx, self.cy, self.strana = ramec(v)
        self.vel = velkost
        self.prvky = []
        self.hrubka = velkost * 0.035

    def p(self, b):
        s = self.vel / self.strana
        return (self.vel / 2 + (b[0] - self.cx) * s, self.vel / 2 - (b[1] - self.cy) * s)

    def ciara(self, a, b, farba, hrubka=None, priehl=1.0):
        (x1, y1), (x2, y2) = self.p(a), self.p(b)
        self.prvky.append(
            f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{farba}" '
            f'stroke-width="{(hrubka or self.hrubka):.1f}" stroke-linecap="round" opacity="{priehl}"/>')

    def kruh(self, c, r, farba, plny=False, hrubka=None, priehl=1.0):
        x, y = self.p(c)
        rr = r * self.vel / self.strana
        if plny:
            self.prvky.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{rr:.1f}" fill="{farba}" opacity="{priehl}"/>')
        else:
            self.prvky.append(
                f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{rr:.1f}" fill="none" stroke="{farba}" '
                f'stroke-width="{(hrubka or self.hrubka):.1f}" opacity="{priehl}"/>')

    def krivka(self, a, kontrola, b, farba, hrubka=None):
        (x1, y1), (qx, qy), (x2, y2) = self.p(a), self.p(kontrola), self.p(b)
        self.prvky.append(
            f'<path d="M{x1:.1f},{y1:.1f} Q{qx:.1f},{qy:.1f} {x2:.1f},{y2:.1f}" fill="none" stroke="{farba}" '
            f'stroke-width="{(hrubka or self.hrubka):.1f}" stroke-linecap="round"/>')

    def svg(self, pozadie="#f5f1ea"):
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.vel}" height="{self.vel}" '
                f'viewBox="0 0 {self.vel} {self.vel}"><rect width="100%" height="100%" rx="10" fill="{pozadie}"/>'
                + "".join(self.prvky) + "</svg>")


def kresli(v, cas, velkost=150, farba="#1c1917", akcent="#d97706"):
    pl = Platno(v, velkost)
    poza = v.dych_v_case(v.poza_v_case(cas), cas)
    b = v.kostra(poza)
    tenka = pl.hrubka * 0.7
    lavy = pl.cx - pl.strana / 2
    pravy = pl.cx + pl.strana / 2

    # rekvizity za postavou
    for r in v.rekvizity:
        t = r["typ"]
        if t == "zem":
            pl.ciara((lavy, -0.005), (pravy, -0.005), akcent, tenka, 0.5)
            if v.posun:
                krok = 0.25
                faza = (cas / max(1, v.cyklus()) * v.posun) % krok
                x = lavy - faza + krok
                while x < pravy:
                    pl.ciara((x, -0.04), (x + 0.06, -0.04), akcent, tenka * 0.8, 0.45)
                    x += krok
        elif t == "stena":
            pl.ciara((r["x"], r.get("y1", 0.0)), (r["x"], r.get("y2", 1.3)), akcent, tenka * 1.2, 0.7)
        elif t == "stolicka":
            x, op = r["x"], r.get("operadlo", 0)
            sed = 0.26
            pl.ciara((x - 0.12, sed), (x + 0.12, sed), akcent, tenka)
            pl.ciara((x - 0.11, sed), (x - 0.11, 0.0), akcent, tenka * 0.8)
            pl.ciara((x + 0.11, sed), (x + 0.11, 0.0), akcent, tenka * 0.8)
            if op:
                xo = x + 0.11 * op
                pl.ciara((xo, sed), (xo, sed + 0.24), akcent, tenka)
        elif t == "schod":
            h = r.get("h", 0.1)
            x1, x2 = r["x1"], r["x2"]
            pl.ciara((x1, h), (x2, h), akcent, tenka)
            pl.ciara((x1, h), (x1, 0), akcent, tenka * 0.8)
            pl.ciara((x2, h), (x2, 0), akcent, tenka * 0.8)
        elif t == "bradla":
            y = r["y"]
            pl.ciara((r["x1"], y), (r["x2"], y), akcent, tenka)
            pl.ciara((r["x1"] + 0.04, y), (r["x1"] + 0.04, 0), akcent, tenka * 0.7, 0.7)
            pl.ciara((r["x2"] - 0.04, y), (r["x2"] - 0.04, 0), akcent, tenka * 0.7, 0.7)
        elif t == "kruhy":
            pl.ciara((r["x"], r["y"] + 0.04), (r["x"], r["y"] + 0.6), akcent, tenka * 0.5, 0.7)
        elif t == "podlozka":
            pl.ciara((r["x1"], 0.012), (r["x2"], 0.012), akcent, tenka * 1.4, 0.35)
        elif t == "hrazda":
            # z boku: prierez tyče (bodka) + jemný náznak tyče do hĺbky; spredu celá tyč
            dl = r.get("dlzka", 0.28)
            pl.ciara((r["x"] - dl / 2, r["y"]), (r["x"] + dl / 2, r["y"]), akcent, tenka, 1.0 if "dlzka" in r else 0.35)

    def koncatina(meno, priehl):
        koren, klb, koniec, spicka = b[meno]
        pl.ciara(koren, klb, farba, priehl=priehl)
        pl.ciara(klb, koniec, farba, priehl=priehl)
        if v.zboku or meno[0] == "r":
            pl.ciara(koniec, spicka, farba, pl.hrubka * 0.8, priehl=priehl)

    zadne = 0.5 if v.zboku else 1.0
    koncatina("nZ", zadne)
    koncatina("rZ", zadne)
    # trup (prípadne prehnutý chrbát)
    bok, krk = b["bok"], b["krk"]
    if abs(poza["chrbat"]) > 1e-4:
        sx, sy = smer(poza["trup"] + 90)
        stred = ((bok[0] + krk[0]) / 2 + sx * poza["chrbat"] * 2, (bok[1] + krk[1]) / 2 + sy * poza["chrbat"] * 2)
        pl.krivka(bok, stred, krk, farba)
    else:
        pl.ciara(bok, krk, farba)
    if v.zboku:
        if poza["lopatky"] > 0:
            pl.ciara(krk, b["rP"][0], farba)
    else:
        pl.ciara(b["nP"][0], b["nZ"][0], farba)
        pl.ciara(b["rP"][0], b["rZ"][0], farba)
    pl.ciara(krk, posun(krk, poza["hlava"], v.k["krk"] - v.k["hlava"]), farba)
    pl.kruh(b["hlava"], v.k["hlava"], farba, hrubka=pl.hrubka * 0.9)
    if v.oko:
        oko = posun(b["hlava"], poza["hlava"] - 90, v.k["hlava"] * 0.45)
        oko = posun(oko, poza["hlava"], v.k["hlava"] * 0.15)
        pl.kruh(oko, v.k["hlava"] * 0.16, farba, plny=True)
    koncatina("nP", 1.0)
    koncatina("rP", 1.0)

    # rekvizity pred postavou
    for r in v.rekvizity:
        t = r["typ"]
        if t == "hrazda" and "dlzka" not in r:
            pl.kruh((r["x"], r["y"]), 0.03, akcent, plny=True)
        elif t == "kruhy":
            pl.kruh((r["x"], r["y"]), 0.045, akcent, hrubka=tenka)
        elif t == "jednorucka":
            bod = r.get("bod", "rP")
            if bod == "bok":
                c = b["bok"]
            else:
                c = b[bod][2] if bod[0] == "n" else b[bod][3]
            pl.kruh(c, 0.042, akcent, plny=True)
        elif t == "uterak":
            pl.ciara((r["x"], r["y"]), (r["x"], r["y2"]), akcent, tenka * 0.9)
        elif t == "taska":
            c = b["rP"][3]
            pl.ciara((c[0] - 0.06, c[1] - 0.06), (c[0] + 0.06, c[1] - 0.06), akcent, pl.hrubka * 2.2)
        elif t == "pulz":
            faza = 0.5 - 0.5 * math.cos(2 * math.pi * (cas % max(1, v.cyklus())) / max(1, v.cyklus()))
            pl.kruh(b["bok"], 0.035 + 0.035 * faza, akcent, plny=True, priehl=0.35 + 0.4 * faza)
        elif t == "ciara":
            pl.ciara((r["x"], 0.0), (r["x"], 0.05), akcent, tenka, 0.8)
    return pl.svg()


# ------------------------------------------------------------------ hárok

def nacitaj():
    with open(POHYBY, encoding="utf-8") as f:
        d = json.load(f)
    vzory = []
    for raw in d["pohyby"]:
        v = Vzor(raw, d["kostra"])
        v._raw = raw
        vzory.append(v)
    return d, vzory


def cviky_podla_pohybu():
    with open(KATALOG, encoding="utf-8") as f:
        k = json.load(f)
    mapa = {}
    for r in k["rebriky"]:
        for c in r["cviky"]:
            mapa.setdefault(c.get("pohyb"), []).append(c)
    return mapa


def casy_kluc(v):
    """Časy kľúčových póz (po ich výdrži) + medzipózy."""
    casy, t = [], 0
    for p in v.pozy:
        casy.append(("kľúč", t + p["drz"]))
        if p["t"] > 0:
            casy.append(("medzi", t + p["drz"] + p["t"] / 2))
        t += p["drz"] + p["t"]
    return casy


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--vystup", default=os.path.join(KOREN, "build", "pohyby-nahlad.html"))
    ap.add_argument("--len", default="", help="čiarkou oddelené id vzorov")
    ap.add_argument("--velkost", type=int, default=150)
    a = ap.parse_args()

    d, vzory = nacitaj()
    mapa = cviky_podla_pohybu()
    vyber = set(filter(None, a.len.split(",")))
    riadky = []
    for v in vzory:
        if vyber and v.id not in vyber:
            continue
        obrazky = []
        for druh, cas in casy_kluc(v):
            okraj = "2px solid #d97706" if druh == "kľúč" else "1px dashed #bbb"
            obrazky.append(f'<div class="o" style="border:{okraj}">{kresli(v, cas, a.velkost)}</div>')
        cviky = mapa.get(v.id, [])
        popis = "".join(
            f"<li><b>{html.escape(c['id'])}</b> {html.escape(c['nazov'])} – "
            f"<i>{html.escape(' / '.join(c.get('cue', [])))}</i></li>" for c in cviky)
        riadky.append(
            f'<section><h2>{html.escape(v.nazov)} <code>{v.id}</code> <small>{v.pohlad} · {v.druh} · '
            f'{v.cyklus()} ms</small></h2><div class="r">{"".join(obrazky)}</div>'
            f'<ul>{popis or "<li><em>žiadny cvik</em></li>"}</ul></section>')
    nepouzite = [v.id for v in vzory if v.id not in mapa]
    chybajuce = sorted(k for k in mapa if k not in {v.id for v in vzory})
    hlavicka = (f"<p>{len(vzory)} vzorov · nepoužité: {', '.join(nepouzite) or '–'} · "
                f"chýbajúce v pohyby.json: {', '.join(map(str, chybajuce)) or '–'}</p>")
    stranka = ("<!doctype html><meta charset=utf-8><title>Pohyby – kontaktný hárok</title><style>"
               "body{font:13px system-ui;margin:16px;background:#fff;color:#222}"
               "section{border-bottom:1px solid #ddd;padding:8px 0}h2{font-size:15px;margin:4px 0}"
               ".r{display:flex;flex-wrap:wrap;gap:6px}.o{line-height:0;border-radius:12px}"
               "ul{margin:4px 0 0 0;padding-left:18px;max-width:1100px}</style>"
               f"<h1>Pohyby – kontaktný hárok</h1>{hlavicka}{''.join(riadky)}")
    os.makedirs(os.path.dirname(a.vystup), exist_ok=True)
    with open(a.vystup, "w", encoding="utf-8") as f:
        f.write(stranka)
    print(f"Zapísané: {a.vystup} ({len(riadky)} vzorov)")
    if chybajuce:
        print("POZOR: cviky odkazujú na neexistujúce vzory:", chybajuce, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
