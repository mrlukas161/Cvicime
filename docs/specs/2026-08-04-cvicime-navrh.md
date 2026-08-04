# Cvičíme — návrh mobilnej cvičebnej aplikácie

**Dátum:** 04.08.2026
**Autor návrhu:** Claude (na zadanie Lukáša Havrilu)
**Stav:** návrh na schválenie
**Denník konverzácie:** `D:\Claude Cowork\Ulohy\2026-08-04_cvicebna-aplikacia-mobil.md`
**Podklady z rešerše:** `D:\Claude Cowork\Trening\docs\reserse\` (17 súborov, 2 workflowy, 17 agentov)

---

## 1. Čo staviame a prečo

Natívna Android aplikácia, ktorá **sama rozhodne, čo dnes cvičiť**, prevedie tréningom (odpočet aj počítanie opakovaní), zapíše výsledok, ukáže progres k trom zručnostiam (L-sit, stojka na rukách, muscle-up) a k vybraným svalovým partiám, pripomenie sa, keď návyk vypadáva, a umožní deťom cvičiť súbežne.

**Prečo vlastná appka a nie hotová z trhu:** komerčné appky sú stavané na pravidelný rozvrh a na pocit vyčerpania (krátke pauzy, kruhový režim). Zadanie je opačné — nepravidelný režim otca troch malých detí a programovanie podľa dôkazov, nie podľa pocitu. Ani jedna appka na trhu nemá skutočný rodinný súbežný režim.

### 1.1 Odsúhlasené rozhodnutia

| Otázka | Rozhodnutie |
|---|---|
| Platforma | Natívny Android, Kotlin + Jetpack Compose |
| Distribúcia | GitHub Actions → podpísané APK → in-app updater (rovnaký recept ako projekt Galéria+) |
| Dáta | 100 % lokálne (Room), žiadny účet, žiadny cloud, funguje offline |
| Prístup | **A — adaptívny tréner** (engine skladá tréning), nie hotové plány |
| Etapy | Tri balíky (E1 / E2 / E3), nie šesť |
| Ciele | **Univerzálne a nastaviteľné** — zručnosti + partie + všeobecné |
| Deti | **Rodinný súbežný tréning** — ten istý rebrík, iná úroveň |
| Motivácia | Viditeľný progres k cieľu · appka sama tlačí · rodinný rozmer. **Bez sérií dní a odznakov pre dospelého** |
| Náradie | Hrazda, jednoručky, podložka, vlastná váha (+ stolička, schod, stena) |

### 1.2 Používateľ a zariadenie

Lukáš Havrila, ~35 rokov, otec troch detí (6, 4, 2 roky). Xiaomi 15, HyperOS 3, Android 16. Nemá Android Studio — inštaluje hotové APK. Trénuje nepravidelne, doma.

---

## 2. Vedecký základ — a čo z neho neplatí

Rešerš prebehla v dvoch kolách: päť nezávislých rešeršov, potom **adversariálne overenie**, ktorého úlohou bolo pôvodné závery vyvrátiť. Overenie prinieslo **53 opráv** zo 144 záverov. Tie opravy sú tu dôležitejšie než pôvodné nálezy — bez nich by appka tvrdila veci, ktoré nie sú dokázané.

### 2.1 Čo obstálo

| Zistenie | Dôsledok pre appku | Zdroj |
|---|---|---|
| Prvé 4 týždne je nárast sily prakticky celý **neurálny**; merateľná hypertrofia až ~6. týždeň | V prvom mesiaci sa **nezobrazuje „rast svalu"** ako metrika — nepohla by sa a používateľ by odpadol. Zobrazujú sa opakovania a výdrže. Appka to otvorene vysvetlí | Moritani & deVries; Del Vecchio 2019 |
| Objem (tvrdé série/týždeň) je hlavný ovládač rastu, s **klesajúcim výnosom**; frekvencia pomáha najmä sile | Sleduje sa **týždenný objem na partiu**, štartuje nízko, dvíha sa pomaly, má strop | Pelland et al., *Sports Medicine* 2025 (67 štúdií / 2058 osôb) — overené verbatim z Tabuľky 3 |
| Zlyhanie **nie je potrebné**; pri hypertrofii nanajvýš triviálny náskok | Cieľ 1–3 opakovania v zálohe v silovom bloku, ďaleko od zlyhania v skill bloku | Grgic et al. 2021; Refalo et al. 2023 |
| Kliky vyrobili prakticky rovnaký prírastok ako bench pri rovnakej blízkosti k zlyhaniu (prsia +18,3 % vs +19,4 %) | Vlastná váha je plnohodnotná. Progresia = **ťažšia variácia**, nie viac opakovaní donekonečna → knižnica ako **rebríky úrovní** | Kikuchi & Nakazato 2017 |
| Plný rozsah ≥ čiastočný; silový tréning cez plný rozsah zvyšuje pohyblivosť **rovnako ako statický strečing** | Mobilita nie je samostatná otravná sekcia — hlboký drep a plné príťahy SÚ ten program | Wolf et al. 2023; Mortaş 2024 |
| Výpony **v stoji** ≫ v sede pre gastrocnemius (12,4 % vs 1,7 %) | Rebrík lýtok je postavený na stoji a hlbokom natiahnutí | Kinoshita et al., *Front. Physiol.* 2023 — čísla overené presne |
| Bielkoviny 1,6–2,2 g/kg | Jedno číslo na dashboarde a jedno ťuknutie. Žiadny jedálniček | Morton 2018 (49 štúdií / 1863 osôb) |
| Sila je večer vyššia; adaptácia je **špecifická na čas tréningu** | **Retest sa musí robiť v podobnú dennú dobu ako pôvodný test** — inak graf ukáže progres, ktorý je len kolísanie počas dňa | Grgic et al., *Chronobiology International* 2019 |
| Účinné techniky zmeny správania: sebamonitoring, plánovanie akcie, **spúšťač** a najmä **riešenie prekážok**; sociálne odmeny návyk niekedy oslabovali | Notifikácia viazaná na kontextovú kotvu; po vynechaní sa appka pýta **prekážku** a ponúkne menšiu verziu | JMIR 2024; meta-regresia návykov 2023 |

### 2.2 Čo overenie VYVRÁTILO alebo výrazne oslabilo

Toto je najdôležitejšia sekcia celého dokumentu.

| Pôvodné tvrdenie | Ako to je v skutočnosti | Dôsledok |
|---|---|---|
| **„Odmeňuj deti za účasť, nie za výkon — je to bezpečnejšie."** | **Nesprávne, a je to najhoršia z možností.** Deci, Koestner & Ryan (1999, 128 štúdií): odmena za samotné zapojenie **d = −0,40**, za dokončenie −0,36, za výkon −0,28. Odmena za účasť podkopala voľnočasovú motiváciu **najviac zo všetkých typov** | Detská mechanika sa navrhuje úplne inak — viď §6 |
| **„Zručnosti sa učia rozloženou praxou — preto krátko a často."** | Podpora **padla**. Pri komplexných úlohách je efekt rozloženej praxe triviálny (d = 0,07–0,11). Pre stojku ani L-sit **neexistuje žiadna recenzovaná intervenčná štúdia s dose-response**; parametre pochádzajú z komerčných blogov | Skill blok v appke ostáva (je to používateľov cieľ a stojí málo únavy), ale appka pri ňom **musí napísať, že ide o praxou overený postup, nie vedecky dokázaný**, a nesmie z týchto čísel odvodzovať automatické rozhodnutia |
| Pauzy 90–180 s sú jasne lepšie | Kredibilné intervaly všetkých kategórií sa **masívne prekrývajú** (0,14–1,1); autori označujú dôkazy nad 90 s ako *„equivocal"*. Nad 60 s výhoda je, ďalej to je otvorené | Default 120 s ostáva ako **pragmatická voľba**, nie ako „vedecky dokázané optimum" |
| RIR je spoľahlivý autoregulačný vstup | **Rozpor medzi zdrojmi.** Halperinova meta (12 štúdií, 414 osôb): podhodnotenie ~0,95 opakovania **nezávisle od skúseností**. Iný zdroj tvrdí 4–5 opakovaní u začiatočníkov, ale nedal sa načítať | **Engine nesmie stáť na RIR.** Hlavný signál = objektívne dosiahnuté opakovania; RIR je len korekcia ±1 |
| Objem dvíhať o +1 sériu za 2 týždne | Jediná priama štúdia (Enes 2024) pridávala **+4 až +6 sérií** za dvojtýždňový krok, na trénovaných mužoch. Náš krok je **zmenšenie bez opory v dátach** | Ostáva ako konzervatívna heuristika, ale **označená ako heuristika** a laditeľná v JSON |
| Deload má číselné kritériá | **Publikované číselné prahy neexistujú.** Autori prehľadu sami píšu o *„clear absence of high-quality experimental research"* | Deload ostáva ako heuristika, appka to nesmie prezentovať ako pravidlo |
| Progres vo výdržiach (L-sit, stojka) sa dá hlásiť | **Reliabilita izometrických výdrží je neznáma** — žiadna štúdia s SEM/CV. Nevieme, koľko sekúnd už nie je šum | Pri izometriách appka **nesmie tvrdiť** „zlepšil si sa"; zobrazí trend, nie výrok |
| Poradie stupňov v rebríkoch je overené | Poradie v L-site, stojke, muscle-upe a drepe je **konsenzus koučovacej praxe**, nie dokázaná optimálna postupnosť. Prahy postupu („3×8 pri RIR 2") nie sú overené štúdiami | Všetky prahy držať v `katalog.json` ako **laditeľné parametre**, nie natvrdo v kóde |
| Gamifikácia deti rozhýbe | Meta-analýza (JMIR Serious Games 2025, 16 RCT, 7472 detí): efekt na stredne/vysoko intenzívny pohyb len **SMD 0,15**. Vzorka 6–18 rokov — **pre deti 2–6 rokov dôkazy neexistujú vôbec** | Detská mechanika musí byť **vypnuteľná jedným prepínačom** a appka musí fungovať aj bez nej |
| Konkurenčná analýza je spoľahlivá | **Žiadna appka nebola fyzicky otestovaná**, Reddit bol pre nástroj zablokovaný, časť zdrojov sú marketingové blogy konkurentov | Nálezy o konkurencii brať ako orientačné |

**Záväzné pravidlo pre všetky texty v appke:** čo pochádza z koučovacej praxe a nie z dôkazov (skill blok, prahy postupu, deload), musí byť v UI označené ako **praxou overený postup, nie vedecky dokázaný**. Appka, ktorá tvrdí viac, než vie, stráca dôveru — a to je hlavný dôvod, prečo ľudia takéto appky vymažú.

---

## 3. Architektúra

### 3.1 Technológie

| Vrstva | Voľba | Verzia (overená 04.08.2026) |
|---|---|---|
| Jazyk / UI | Kotlin + Jetpack Compose (Material 3) | Kotlin 2.3.10, Compose BOM 2026.06.01 |
| Build | AGP + Gradle + KSP, JDK 17 | AGP 9.2.0, Gradle 9.4.1, KSP 2.3.7 |
| Dáta | Room | 2.8.4 |
| Navigácia | navigation-compose | 2.9.8 |
| Pozadie | WorkManager + AlarmManager | work 2.11.2 |
| Ostatné | activity-compose 1.13.0, lifecycle 2.11.0, core-ktx 1.17.0 | |
| SDK | compileSdk / targetSdk 36, minSdk 26 | |

Verzie AndroidX sú overené proti **Google Maven metadata**, verzie toolchainu proti **fungujúcemu buildu projektu Galéria+** — nie proti master vetvám repozitárov. (Poučenie z Galérie: master ≠ posledný release.)

**Minifikácia je v release builde vypnutá** (`isMinifyEnabled = false`, `isShrinkResources = false`). Dôvod: v predošlom projekte R8 rozbil knižnice, ktoré si hľadajú volajúcu triedu na zásobníku, a stálo to tri verzie. Pri verejnom vydaní zapnúť späť s presnými keep pravidlami.

### 3.2 Dve oddelené databázy

Toto je zásadné architektonické rozhodnutie:

| Databáza | Obsah | Migračná politika |
|---|---|---|
| `katalog.db` | Rebríky a cviky. Zdroj = `assets/katalog.json` | **Deštruktívna migrácia povolená.** Pri každej novej verzii sa celý katalóg prepíše. Žiadne používateľské dáta tu nie sú |
| `denik.db` | Tréningy, série, testy, ciele, úrovne, deti, zvieratá, miery | **Deštruktívna migrácia ZAKÁZANÁ.** Každá zmena schémy musí mať riadnu `@Migration`, aj keď je to otrava. Strata histórie = strata celej hodnoty appky |

Používateľské úrovne v rebríkoch sú v `denik.db` (`uroven_rebrika`), nie v katalógu — preto prepis knižnice nemôže zmazať progres.

### 3.3 Balíky

```
sk.havrila.cvicime
├── data
│   ├── model/       Enumy.kt          — Meranie, Partia, Naradie, Blok, DruhCiela, Rir, Skupina
│   ├── katalog/     KatalogEntity, KatalogDao, KatalogDatabase, KatalogLoader
│   ├── denik/       DenikEntity, DenikDao, DenikDatabase
│   ├── Prefs.kt     drobné nastavenia
│   └── Repozitar.kt jediný vstupný bod k dátam
├── domain
│   ├── Objem.kt     týždenné ciele objemu na partiu
│   ├── Pravidla.kt  progresia, deload, prahy reálnej zmeny
│   └── Generator.kt zostavenie dnešného tréningu (čistá funkcia)
├── trening/         TreningService (foreground), stav prehrávača
├── pripomienky/     BootReceiver, PripomienkaReceiver, plánovanie
└── ui/
    ├── theme/
    ├── obrazovky/   Domov, Onboarding, Test, Prehravac, Progres, Kniznica, Deti, Nastavenia, Historia
    └── komponenty/
```

### 3.4 Beh na pozadí

Odpočet a hlas musia bežať pri zhasnutej obrazovke. Foreground service typu **`specialUse`** — zámerne:

- `dataSync` má na Androide 15 časový limit (6 h / 24 h),
- `health` by vyžadoval runtime oprávnenie `ACTIVITY_RECOGNITION`, ktoré na odpočítavanie sérií nepotrebujeme,
- appka sa nedistribuuje cez Play Store, takže zdôvodnenie typu pre recenziu netreba.

**HyperOS službu aj tak môže zabiť.** Preto sa stav tréningu perzistuje (`prefs.beziaciTreningId` + zapísané série) a po návrate do appky sa ponúkne pokračovanie. Overenie upozornilo, že samotné uloženie času konca nestačí, lebo FGS nebudí CPU — pri odpočte treba alarm alebo wake lock. `<toto treba otestovať na zariadení, T2>`

---

## 4. Dátový model

Entity sú implementované v `data/katalog/KatalogEntity.kt` a `data/denik/DenikEntity.kt`.

### 4.1 Katalóg

- **`rebrik`** — `id, nazov, popis, skupina, je_zrucnost, poradie`
- **`cvik`** — `id, rebrik_id, uroven, nazov, meranie, ciel_min, ciel_max, naradie, partie, cue, chyby, je_detsky, zviera`

`partie` je reťazec `"hrudnik:1.0,triceps:0.5"` — **1.0 = priama séria, 0.5 = nepriama (frakčná)**, presne ako sa objem klasifikuje v literatúre.

### 4.2 Denník

`profil` · `ciel` · `trening` · `seria` · `seria_partia` · `test` · `uroven_rebrika` · `dieta` · `ucast` · `zviera` · `denny_zaznam` · `miera` · `prekazka`

### 4.3 Kľúčový dopyt — týždenný objem na partiu

Objem **nie je uložený stĺpec**, dopočítava sa — tak sa nikdy nerozsynchronizuje. Preto existuje tabuľka `seria_partia` (rozpad série na partie) a nie parsovanie reťazca v SQL:

```sql
SELECT sp.partia AS partia, SUM(sp.vaha) AS serie
FROM seria_partia sp
JOIN seria s ON s.id = sp.seria_id
WHERE s.epoch_den >= :odDna AND s.epoch_den <= :doDna
  AND s.skutocna_hodnota IS NOT NULL
  AND s.preskocena = 0
  AND s.blok IN ('sila', 'kondicia')
GROUP BY sp.partia
```

Rozohriatie, mobilita a skill sa do objemu **nezapočítavajú** — sú nízkozáťažové.

---

## 5. Engine — ako sa rozhodne „čo dnes"

Vstup: dostupný čas, náradie, aktuálne úrovne v rebríkoch, ciele, odcvičený objem tohto týždňa, kedy bola ktorá partia naposledy zaťažená, koľko týždňov už trénuje.

### 5.1 Pevné poradie blokov

| # | Blok | Pauza | Prečo práve tu |
|---|---|---|---|
| 1 | Rozohriatie | 0 s | Dynamické, plný rozsah |
| 2 | **Zručnosť** | 60 s | Kým je čerstvý, nikdy do zlyhania. **Neškrtá sa nikdy** — stojí málo únavy a je to jeho cieľ |
| 3 | **Sila** | **120 s** | 2–5 cvikov, cieľ 1–3 opakovania v zálohe |
| 4 | Kondičný kruh | 20 s | Voliteľný. **Tu a nikde inde** patrí kolotoč |
| 5 | Mobilita | 15 s | Cielene na to, čo test ukázal ako limit |

### 5.2 Rozpočet času

| Dostupné | Rozohriatie | Zručnosť | Sila | Kondícia | Mobilita |
|---|---|---|---|---|---|
| ≤ 15 min | 3 | 4 | 7 | — | 1 |
| ≤ 20 min | 3 | 5 | 10 | — | 2 |
| ≤ 30 min | 4 | 6 | 16 | — | 4 |
| ≤ 45 min | 5 | 7 | 24 | 5 | 4 |
| viac | 6 | 8 | 30 | 10 | 6 |

Pri málo času vypadne kondičný blok a uberú sa série. **Pauzy sa neskracujú nikdy** — radšej vypadne cvik.

### 5.3 Výber cvikov do silového bloku

1. Pre každú partiu sa spočíta **deficit** = týždenný cieľ − odcvičené.
2. Cieľ objemu: štart **8 sérií/týždeň**, +1 za každé 2 týždne, strop **16** pri prioritnej partii, **12** pri bežnej, **12 pri lýtkach** (nad ~9–12 sérií podľa štúdií nič nepribúda).
3. Skóre cviku = Σ (deficit partie × váha partie), s penalizáciou za nedávnu záťaž (dnes ×0,2; včera ×0,6).
4. Vyberú sa najvyššie skórujúce cviky z rôznych rebríkov, s dôrazom na pokrytie tlaku aj ťahu (inak vznikajú nerovnováhy).
5. Filter na dostupné náradie.

### 5.4 Progresia

- **O stupeň vyššie:** vo *všetkých* sériách dosiahnutý horný cieľ **a** aspoň v jednej ostalo niečo v zálohe.
- **O stupeň nižšie:** ani spodný cieľ sa nedosiahol v žiadnej sérii a všetky skončili na doraz.
- **Deload:** dva tréningy po sebe, kde ≥ 60 % silových sérií skončilo na doraz → objem ×0,6 na jeden tréning. Appka to **zdôvodní**, nie oznámi ako trest.

Hlavným signálom sú **objektívne opakovania**, RIR je len korekcia — pretože spoľahlivosť odhadu RIR u začiatočníka je sporná (§2.2).

### 5.5 Testy a retesty

Vstupný test (~15 min): kliky · príťahy (alebo výdrž vo visu, ak 0) · drep · plank/hollow · L-sit na zemi · stojka pri stene · predklon · zdvih rúk v ľahu na bruchu (mobilita ramien pre stojku).

Retest po **28 dňoch**, s pripomienkou. Ukladá sa **minúta dňa** — pri retestoch mimo tolerancie (±150 min) appka upozorní, že čísla nie sú porovnateľné.

Prah reálnej zmeny: opakovania ≥ 2, výdrž ≥ 3 s — **pri výdržiach je to odhad, reliabilita nie je známa** (§2.2), takže appka pri izometriách hlási trend, nie výrok.

---

## 6. Rodinný režim — prepracovaný podľa dôkazov

Pôvodný návrh („ukazovateľ plnenia → odmena za účasť") **rešerš vyvrátila**. Nasleduje opravená verzia.

### 6.1 Čo sa zmenilo a prečo

**Lepper, Greene & Nisbett (1973)** dali 51 predškolákom, ktorí už radi kreslili, certifikát so zlatou pečaťou — teda presne ten typ symbolickej odmeny, aký sme plánovali. Skupina, ktorej bol odznak **sľúbený vopred**, potom vo voľnej hre kreslila 8,59 % času oproti 16,73 % v kontrolnej skupine — takmer polovičný pokles. Skupina, ktorá ten istý certifikát dostala **nečakane**, sa nezhoršila.

Rozhodujúca nie je teda otázka „za účasť či za výkon", ale **či je odmena očakávaná**. A rozhodujúce je aj jej **funkčné rámovanie**: či ju dieťa prežíva ako *informáciu o kompetencii* („toto už vieš") alebo ako *riadenie* („urob toto, aby si dostal tamto").

### 6.2 Záväzné pravidlá detskej vrstvy

1. **Zvieratko sa nesmie sľubovať vopred.** Ukazovateľ plnenia nikdy nezobrazí, čo sa odomkne — ani meno, ani siluetu. Identita je do poslednej chvíle neznáma. Tým sa mechanika posunie z „očakávanej odmeny" (škodí) do „neočakávanej" (neškodí) pri zachovaní celého zážitku.
2. **Zvieratko = meno konkrétnej zručnosti, nie token.** Text pri odomknutí je opisný: *„Prešiel si celú izbu po štyroch ako medveď."* — nie *„Výborne! Získavaš Medveďa!"*. Zbierka je **mapa toho, čo dieťa vie**, nie skóre.
3. **Neodmeňovať to, čo deti robia aj tak samé** (behanie, skákanie, šplhanie) — tam mechanika len škodí. Každý detský cvik má príznak odmeniteľnosti, **default vypnutý**.
4. **Ukazovateľ podľa veku.** 2–3 roky: max **3 konkrétne obrázky**, ktoré sa dopĺňajú (nie abstraktný pruh, nie čísla — dvojročný nechápe duálnu reprezentáciu a nepozná čísla nad „jeden"). 4–5 rokov: 4–6 krokov. 6 rokov: až 10. **Percentá nikdy pod 6 rokov.**
5. **Načasovanie odmeny podľa veku.** 2 roky: **okamžite po cviku** (trojročné deti nezvládajú čakať na budúcu odmenu). 4 roky: koniec dnešného kola. 6 rokov: môže preklenúť 2–3 tréningy. **Viacdňové ukazovatele pod 4 roky sú zakázané.**
6. **Prvý krok zadarmo** za to, že dieťa prišlo — nepodmienený dar, ktorý podľa meta-analýzy nepodkopáva.
7. **Žiadne trestanie.** Žiadne série s resetom, žiadny úbytok, žiadne „stratil si Medveďa", žiadne červené „už 5 dní si necvičil". Odomknuté zviera je natrvalo.
8. **Žiadne porovnávanie medzi deťmi.** Každé dieťa má vlastnú oddelenú zbierku. Pri súrodencoch 2, 4 a 6 rokov je porovnanie systematicky nespravodlivé — dvojročný nemôže vyhrať nikdy. **Jediný spoločný ukazovateľ je rodinný** („koľkokrát sme cvičili spolu"), kde sú všetci na tej istej strane.
   > **Zmena oproti tomu, čo si vybral:** v otázke o motivácii si označil „súťaž a skóre s deťmi". Súťaž medzi deťmi navrhujem vypustiť; rodinné „my" skóre ostáva. Potrebujem k tomu tvoje slovo.
9. **Spoločné cvičenie je najsilnejší a najbezpečnejší motivátor** — a je zadarmo, bez rizika podkopania. Cvik sa počíta, len ak ho robia **obaja**. Appka nemá podporovať režim „otec drží telefón a dozerá".
10. **Voľba namiesto odmeny.** Každé detské kolo ponúkne aspoň dve reálne voľby (ktoré zviera dnes, v akom poradí, kto začne). Voľba je motivačne účinná a **nemá žiadne známe riziko podkopania** — na rozdiel od akéhokoľvek ukazovateľa.
11. **Odmena sa nesmie „rozšťaviť".** Animácia odomknutia max 2–3 s, žiadna interaktívna hra so zvieratkom, appka sa hneď vráti k pohybu. Čím atraktívnejší je symbol ako predmet, tým horšie funguje ako symbol.
12. **Čas dieťaťa pri obrazovke pod ~2 minúty na tréning.** Telefón drží otec. WHO odporúča deťom 2–4 roky najviac hodinu obrazovky denne — appka má byť odpaľovač, nie cieľ.
13. **Celá mechanika je vypnuteľná jedným prepínačom** a appka funguje aj bez nej. Dôkazy o gamifikácii u detí 2–6 rokov neexistujú.
14. **Zbierka dimenzovaná na mesiace** — ~25–30 zvierat, aby vydržala pol roka pri dvoch tréningoch týždenne.

---

## 7. Obrazovky

| Obrazovka | Účel |
|---|---|
| **Onboarding** | Ciele (zručnosti + partie na obrázku tela + všeobecné) · náradie · váha/výška · kontextová kotva pripomienky · deti |
| **Vstupný test** | 8 stanovíšť, krok za krokom; zvláda aj výsledok „ani raz" bez odrádzania |
| **Dnes** | Čo dnes · jedno veľké tlačidlo · stav cieľov · bielkoviny · týždeň v prehľade |
| **Prehrávač** | Režim na čas / na opakovania / pauza · otázka po sérii · tlačidlo „nemám čas / nevládzem" · prechod medzi blokmi |
| **Rodinný prehrávač** | Dva pruhy (ty / dieťa) · detský ukazovateľ · moment odomknutia |
| **Progres** | Postupové pásy k cieľom · pomenovanie **jednej brzdy** · grafy · týždenný objem na partiu · miery a fotky |
| **Knižnica** | Rebríky a stupne · detail cviku (pokyny, chyby, na akej úrovni si) |
| **Zbierka zvierat** | Oddelene pre každé dieťa |
| **História** | Zoznam tréningov |
| **Nastavenia** | Ciele · náradie · pripomienky · deti · export/záloha · kontrola aktualizácií |

**Ovládanie spotenými rukami a bez okuliarov:** veľké dotykové plochy, veľké číslo v strede, hlas a vibrácie, telefón čitateľný zo zeme na vzdialenosť ~1,5 m.

---

## 8. Etapy

| Balík | Obsah |
|---|---|
| **E1 — „Cvičím"** | Build pipeline + keystore + updater · dve databázy · knižnica cvikov · onboarding · vstupný test · engine · prehrávač (čas/opakovania/pauza) · zápis sérií · história |
| **E2 — „Vidím pokrok a appka ma drží"** | Postupové pásy · brzda · automatický posun o stupeň · deload · grafy · týždenný objem · retest s kontrolou dennej doby · notifikácie s kotvou · otázka na prekážku + skrátený tréning · bielkoviny |
| **E3 — „Rodina a doladenie"** | Rodinný režim · detské cviky · ukazovateľ podľa veku · zbierka zvierat · rodinný ukazovateľ · hlas · ilustrácie · export/záloha · miery |

E2 je zámerne pred E3: najväčšie riziko projektu nie je technické, ale to, že appka prestane byť používaná v šiestom týždni.

---

## 9. Riziká

| Riziko | Opatrenie |
|---|---|
| **Slovenský TTS na zariadení nemusí existovať** | Otestovať ako prvé (T1). Ak chýba, fallback na pípanie a vibrácie; hlas je doplnok, nie podmienka |
| **HyperOS zabije službu počas tréningu** | Perzistovaný stav + ponuka pokračovania · onboarding checklist (autostart, výnimka z optimalizácie batérie) · otestovať (T2) |
| **Build padne na Room + KSP + Compose** | Prvý commit = holý skeleton, ktorý sa musí zeleno postaviť, až potom obsah (T4) |
| **Detská mechanika podkope motiváciu detí** | Prepracované podľa §6 · vypnuteľné · spoločné cvičenie ako hlavný motor |
| **Appka bude tvrdiť veci, ktoré nie sú dokázané** | Každý text z koučovacej praxe označený ako praxou overený, nie dokázaný (§2.2) |
| **Dieťa vezme telefón a niečo pokazí** | Žiadne gestá na mazanie · mazanie len dlhým podržaním s dialógom · z detského pruhu nevedie cesta do nastavení |
| **Zvuk zobudí uspaté dieťa** | Notifikačné kanály `IMPORTANCE_LOW`, bez zvuku · prehrávač default len vibrácie |
| **Deti pri hrazde** | Potvrdenie dozoru dospelého pred blokom · pri detských cvikoch žiadne časovky (aby dieťa z únavy nevisiace nepustilo) · pod 4 roky hrazda nedostupná |
| **Strata podpisového kľúča** | `D:\Claude Cowork\Trening\keystore\` — zálohovať offline. Strata = koniec in-place aktualizácií |

---

## 10. Otvorené otázky pre Lukáša

1. **Súťaž medzi deťmi** — navrhujem vypustiť (§6.2 bod 8), ponechať len rodinné „my" skóre. Súhlasíš?
2. **Repozitár na GitHube** — zakladám ako **súkromný**. Ak ho chceš verejný, povedz.
3. **Ilustrácie cvikov** — v E1 budú cviky len textom (názov + 3 pokyny + chyby). Vlastná vektorová sada ~150 cvikov je samostatná dávka práce, patrí do E3. Súhlasíš, alebo ich chceš skôr?
4. **Meno appky** — pracovne **„Cvičíme"** (balík `sk.havrila.cvicime`). Zmena mena po prvom builde je otrava (nový balík = nová inštalácia), tak to povedz teraz.

---

## 11. Testy pred dokončením E1

| Kód | Test | Prečo |
|---|---|---|
| T1 | Slovenský TTS na Xiaomi 15 | Rozhoduje o hlasovej vrstve |
| T2 | Foreground service prežije zhasnutú obrazovku 20 min na HyperOS | Rozhoduje o použiteľnosti prehrávača |
| T3 | Inštalácia podpísaného APK z GitHub Release + in-app updater | Rozhoduje o celom spôsobe dodávania |
| T4 | Zelený build Room + KSP + Compose s uvedenými verziami | Blokuje všetko ostatné |
| T5 | Hrazda znesie muscle-up (typ uchytenia) | Jeden z troch cieľov |
