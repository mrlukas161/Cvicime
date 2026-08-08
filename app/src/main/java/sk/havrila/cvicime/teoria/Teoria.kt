package sk.havrila.cvicime.teoria

/**
 * Teoretická časť appky. Obsah vychádza z rešerše meta-analýz (2017–2026),
 * ktorá prešla adversariálnym overením — a preto je pri každom článku poctivo
 * uvedené, či ide o dokázané zistenie alebo o prax trénerov bez štúdií.
 */
data class Clanok(
    val id: String,
    val emoji: String,
    val titulok: String,
    /** DOKAZANE / PRAX / ZMIESANE - zobrazuje sa ako štítok. */
    val zaklad: String,
    val perex: String,
    val telo: String,
)

object Teoria {

    const val DOKAZANE = "podložené štúdiami"
    const val PRAX = "prax trénerov"
    const val ZMIESANE = "čiastočne podložené"

    val clanky: List<Clanok> = listOf(
        Clanok(
            id = "prvy-mesiac",
            emoji = "🧠",
            titulok = "Prvý mesiac rastie nervový systém, nie sval",
            zaklad = DOKAZANE,
            perex = "Prečo ti čísla letia hore, ale zrkadlo mlčí.",
            telo = "V prvých dvoch až štyroch týždňoch tréningu je prakticky celý nárast sily " +
                "neurálny: mozog sa učí zapájať viac svalových vlákien naraz, rýchlejšie a v lepšom " +
                "poradí. Merateľné zväčšenie svalu sa v štúdiách objavuje až okolo šiesteho týždňa.\n\n" +
                "Praktický dôsledok: keď po dvoch týždňoch spravíš o štyri kliky viac, je to skutočný " +
                "pokrok — len zatiaľ býva v nervoch, nie v centimetroch. Preto ti appka v prvom mesiaci " +
                "ukazuje opakovania a výdrže, nie obvody. Zrkadlo príde, len neskôr než čísla.\n\n" +
                "Najčastejší dôvod, prečo ľudia prestanú cvičiť, je práve tretí týždeň: čísla rastú, " +
                "telo sa nemení, a človek usúdi, že „to nefunguje\". Funguje — presne v tomto poradí.",
        ),
        Clanok(
            id = "objem",
            emoji = "📦",
            titulok = "Objem: hlavný ovládač rastu",
            zaklad = DOKAZANE,
            perex = "Počet tvrdých sérií za týždeň rozhoduje viac než čokoľvek iné.",
            telo = "Najväčšia meta-regresia k tejto otázke (Pelland 2025; 67 štúdií, vyše 2000 ľudí) " +
                "ukazuje: rast svalu stúpa s počtom tvrdých sérií na partiu za týždeň — ale s klesajúcim " +
                "výnosom. Už ~4 série týždenne vyvolajú merateľný rast; okolo 8–12 sérií je výborné pásmo; " +
                "nad ~18 sérií pribúda už len málo a rastie únava.\n\n" +
                "Preto appka počíta týždenné série na každú partiu, štartuje nízko (8) a dvíha pomaly. " +
                "Séria sa počíta ako „tvrdá\", keď je dotiahnutá blízko zlyhania — nie rozcvičkové kolo.\n\n" +
                "Sila (na rozdiel od objemu svalu) reaguje viac na frekvenciu a blízkosť k maximálnej " +
                "záťaži než na hromadenie sérií — aj preto sú v pláne ťažšie variácie, nie nekonečné opakovania.",
        ),
        Clanok(
            id = "zlyhanie",
            emoji = "🛑",
            titulok = "Do zlyhania netreba",
            zaklad = DOKAZANE,
            perex = "„Ešte 1–2 v zálohe\" buduje rovnako — a zregeneruješ rýchlejšie.",
            telo = "Meta-analýzy porovnávajúce tréning do úplného zlyhania s tréningom tesne pred ním " +
                "nenašli pri sile žiadny rozdiel a pri raste svalu nanajvýš triviálny náskok zlyhania — " +
                "vykúpený výrazne dlhšou regeneráciou.\n\n" +
                "Preto sa ťa appka po sérii pýta „koľko si mal ešte v sebe?\". Cieľ v silovom bloku je " +
                "1–2 opakovania v zálohe. Do zlyhania choď nanajvýš občas pri poslednej sérii — nie preto, " +
                "že by to bolo lepšie, ale aby si vedel svoj strop odhadovať presnejšie.\n\n" +
                "Pozor na odhad: začiatočníci sa v rezerve mýlia priemerne o ~1 opakovanie. Preto je " +
                "hlavným signálom pre appku skutočný počet opakovaní a rezerva je len korekcia.",
        ),
        Clanok(
            id = "pauzy",
            emoji = "⏱️",
            titulok = "Prečo sú pauzy 2 minúty",
            zaklad = ZMIESANE,
            perex = "Krátka pauza znie tvrdšie — ale znamená menší prírastok.",
            telo = "Do ~60 sekúnd je dôkaz jasný: príliš krátke pauzy zmenšujú výkon v ďalšej sérii " +
                "natoľko, že klesá celkový objem práce, a s ním aj rast. Nad 90 sekúnd sa dôkazy " +
                "prekrývajú — 2 minúty sú pragmatická voľba, nie dokázané optimum.\n\n" +
                "Kolotočové appky ťa ženú v 30-sekundovom cykle, lebo z toho máš pocit smrti — a pocit " +
                "sa predáva. Rast ale vzniká z ťažkých sérií, nie z udýchania. Preto má silový blok " +
                "v tejto appke pauzy 2 minúty a kolotoč s krátkymi pauzami existuje len ako voliteľný " +
                "kondičný blok na konci.\n\n" +
                "Ak pauzu preskočíš, appka to dovolí — ale zaráta, že ďalšia séria bude slabšia.",
        ),
        Clanok(
            id = "variacie",
            emoji = "🪜",
            titulok = "Progresia = ťažšia variácia, nie viac opakovaní",
            zaklad = DOKAZANE,
            perex = "Kliky vyrobili rovnaký sval ako bench press. Podmienka: rovnaká tvrdosť.",
            telo = "V priamom porovnaní (Kikuchi 2017) vyrobili kliky prakticky rovnaký prírastok " +
                "svalu ako tlak na lavičke: prsia +18,3 % vs +19,4 %, triceps +9,5 % vs +10,3 %. " +
                "Rozhodujúca nebola pomôcka, ale blízkosť k zlyhaniu.\n\n" +
                "Háčik vlastnej váhy: keď zvládneš 25 klikov, ďalšie opakovania už budujú skôr " +
                "vytrvalosť než silu a sval. Riešenie je posunúť sa na ťažšiu variáciu — preto je celá " +
                "knižnica postavená ako rebríky stupňov a appka ťa posúva hore, keď splníš horný cieľ " +
                "opakovaní so zálohou.\n\n" +
                "Platí to aj naopak: keď stupeň nejde, klesnúť o stupeň nižšie nie je hanba, je to " +
                "presnejšie dávkovanie.",
        ),
        Clanok(
            id = "rozsah",
            emoji = "🤸",
            titulok = "Plný rozsah pohybu nahrádza strečing",
            zaklad = DOKAZANE,
            perex = "Hlboký drep je zároveň tréning pohyblivosti.",
            telo = "Silový tréning vykonávaný cez plný rozsah pohybu zväčšuje pohyblivosť rovnako " +
                "ako statický strečing — a k tomu pridáva silu (meta-analýza Wolf 2023, potvrdená RCT 2024).\n\n" +
                "Preto v appke nie je mobilita otravná samostatná sekcia: hlboký drep, plné príťahy " +
                "a kliky s plnou dráhou SÚ tvoj mobilitný program. Cielený strečing sa pridáva len na to, " +
                "čo ti reálne chýba k cieľom — ramená pre stojku, zadná strana stehien pre L-sit, " +
                "členky pre hlboký drep, zápästia pre oboje.\n\n" +
                "A pri lýtkach platí zvlášť: rast bol najväčší pri výponoch s hlbokým natiahnutím " +
                "v spodnej polohe — preto rebrík výponov stojí na schode, päta pod úrovňou špičky.",
        ),
        Clanok(
            id = "zrucnosti",
            emoji = "🎯",
            titulok = "Stojka a L-sit: prečo sú na začiatku tréningu",
            zaklad = PRAX,
            perex = "Zručnosť sa učí čerstvá hlava, nie unavená.",
            telo = "Poctivo: pre stojku ani L-sit neexistuje žiadna vedecká štúdia, ktorá by povedala, " +
                "koľko minút a ako často je optimálne. Všetky odporúčania pochádzajú z gymnastickej " +
                "praxe — tá sa ale desaťročia zhoduje na troch veciach:\n\n" +
                "1. Zručnosť sa trénuje na začiatku, kým si čerstvý. Unavené telo sa učí zlé vzory.\n" +
                "2. Nikdy do zlyhania. Pád zo stojky nič nenaučí; kontrolovaný zostup áno.\n" +
                "3. Radšej krátko a často než dlho a zriedka.\n\n" +
                "Preto má skill blok v pláne pevné miesto hneď po rozohriatí, malý objem a veľkú rezervu — " +
                "a nikdy sa neškrtá, ani keď máš len 15 minút. Je lacný na únavu a je to tvoj cieľ.\n\n" +
                "Pri meraní výdrží ešte jedna poctivosť: nevie sa, koľko sekúnd rozdielu je už skutočné " +
                "zlepšenie a koľko je len denné kolísanie. Appka preto pri výdržiach ukazuje trend, " +
                "nie víťazné vyhlásenia.",
        ),
        Clanok(
            id = "muscle-up",
            emoji = "💪",
            titulok = "Muscle-up má vstupnú bránu",
            zaklad = PRAX,
            perex = "Najprv ~10 príťahov a ~8 tlakov nad hrazdou. Skratka neexistuje.",
            telo = "Zhoda trénerov naprieč zdrojmi: pred prvým muscle-upom treba vedieť zhruba " +
                "10–12 čistých príťahov, 8–10 tlakových opakovaní (dipy alebo ich náhrada) a mať " +
                "kontrolu nad falošným úchopom.\n\n" +
                "Nie je to byrokracia — prechod cez hrazdu je najnamáhavejší bod celého pohybu " +
                "a slabé príťahy sa tam prejavia trhaním, kmitaním a preťaženými lakťami.\n\n" +
                "Appka ti preto muscle-up neponúkne v druhom týždni. Namiesto toho ukazuje, koľko ti " +
                "do brány chýba, a tréningy skladá tak, aby si sa k nej blížil: ťažšie príťahy, " +
                "vysoké príťahy, falošný úchop, negatívy. Na obrazovke Pokrok vidíš presne, " +
                "ktorá požiadavka ťa práve drží.",
        ),
        Clanok(
            id = "denna-doba",
            emoji = "🌗",
            titulok = "Sila kolíše počas dňa",
            zaklad = DOKAZANE,
            perex = "Preto sa retest robí v rovnakú dennú dobu ako prvý test.",
            telo = "Sila je večer prirodzene vyššia než ráno — telesná teplota aj nervový systém majú " +
                "denný rytmus. Meta-analýza zároveň ukázala, že adaptácia je špecifická na čas tréningu: " +
                "kto trénuje ráno, dvíha rannú silu na večernú úroveň.\n\n" +
                "Praktické dôsledky:\n" +
                "1. Ak si môžeš vybrať, podvečer je mierne výhodnejší. Rozdiel je malý — s tromi deťmi " +
                "podľa neho neplánuj život.\n" +
                "2. Dôležitejšie: retest maximálok rob v podobnú dennú dobu ako prvý test. Test ráno " +
                "a retest večer ukáže „pokrok\", ktorý je len kolísanie počas dňa. Appka si čas testu " +
                "pamätá a upozorní ťa, keby si sa meral v inú dobu.",
        ),
        Clanok(
            id = "bielkoviny",
            emoji = "🍳",
            titulok = "Bielkoviny: jedno číslo stačí",
            zaklad = DOKAZANE,
            perex = "1,6–2,2 g na kilo váhy denne. Viac už nič nepridá.",
            telo = "Meta-analýza 49 štúdií (Morton 2018): prírastok svalu rastie s príjmom bielkovín " +
                "len po hranicu ~1,6 g na kilogram telesnej váhy denne (horný odhad 2,2 g/kg). " +
                "Nad tým už nič nepribúda.\n\n" +
                "Načasovanie, „anabolické okno\" po tréningu ani presné rozdelenie do jedál nemajú " +
                "podľa dôkazov podstatný význam — rozhoduje denný súčet.\n\n" +
                "Preto appka nerieši jedálniček: ukazuje jedno číslo vypočítané z tvojej váhy a jedno " +
                "ťuknutie denne, či si ho zhruba trafil. Orientačne: 100 g tvarohu ~12 g, vajce ~7 g, " +
                "100 g kuracích pŕs ~23 g, šošovica na tanieri ~15 g.",
        ),
        Clanok(
            id = "deload",
            emoji = "😮‍💨",
            titulok = "Deload nie je trest",
            zaklad = PRAX,
            perex = "Keď dva tréningy po sebe končíš na doraz, treba uhnúť.",
            telo = "Publikované číselné kritériá pre deload neexistujú — sami autori prehľadov píšu, " +
                "že kvalitný výskum chýba. Zhoda praxe ale je: keď výkon opakovane stagnuje alebo klesá " +
                "a série končia bez zálohy, telo nestíha regenerovať a ďalšie tlačenie situáciu zhoršuje.\n\n" +
                "Appka to sleduje jednoducho: dva tréningy po sebe, v ktorých väčšina silových sérií " +
                "skončila na doraz, spustia jeden ľahší tréning (objem znížený o ~40 %). Nie je to trest " +
                "ani krok späť — po odľahčení sa výkon typicky vráti vyšší.\n\n" +
                "Rovnaká logika platí pre život s malými deťmi: po prebdenej noci je lepší krátky ľahký " +
                "tréning než hrdinský výkon, ktorý zaplatíš týždňom stagnácie.",
        ),
        Clanok(
            id = "deti",
            emoji = "🧒",
            titulok = "Deti: odmena nesmie byť sľúbená vopred",
            zaklad = DOKAZANE,
            perex = "Prečo ukazovateľ nikdy neukazuje, aké zviera sa odomkne.",
            telo = "Klasický experiment (Lepper 1973): škôlkarom, ktorí RADI kreslili, sľúbili diplom " +
                "za kreslenie. Po odovzdaní diplomov kreslili vo voľnej hre o polovicu menej než predtým — " +
                "odmena sľúbená vopred zabila radosť z činnosti. Tie isté deti, ktoré dostali diplom " +
                "NEČAKANE, kreslili ďalej rovnako rady.\n\n" +
                "Preto v rodinnom režime:\n" +
                "• pásik plnenia nikdy neukazuje, čo sa odomkne — zviera je prekvapenie,\n" +
                "• text pri odomknutí pomenúva, ČO dieťa dokázalo („prešiel si izbu ako medveď\"), " +
                "nie „vyhral si\",\n" +
                "• zviera sa nikdy neodoberá a deti sa nikdy neporovnávajú medzi sebou,\n" +
                "• najsilnejší motivátor si ty: deti, ktorých rodič cvičí s nimi, sa hýbu viac — " +
                "to je najlepšie doložený efekt z celej detskej literatúry.\n\n" +
                "A drž telefón v ruke ty. Obrazovka je odpaľovač cvičenia, nie zábava — WHO odporúča " +
                "deťom do 4 rokov najviac hodinu obrazovky denne.",
        ),
        Clanok(
            id = "navyk",
            emoji = "🔁",
            titulok = "Návyk drží kotva, nie vôľa",
            zaklad = DOKAZANE,
            perex = "„Keď uložím deti\" funguje lepšie než „o 20:00\".",
            telo = "Z výskumu zmeny správania vychádzajú ako najúčinnejšie tri veci: sledovanie vlastnej " +
                "aktivity (to robí appka za teba), spúšťač naviazaný na existujúcu rutinu (kotva) " +
                "a — najsilnejšie — riešenie prekážok namiesto pripomínania.\n\n" +
                "Kotva znamená: nie „cvičím o ôsmej\", ale „cvičím, keď uložím deti\". Rutina, ktorá " +
                "sa deje tak či tak, potiahne novú činnosť so sebou.\n\n" +
                "A keď vypadneš — čo sa pri troch deťoch stane — appka sa nepýta „prečo si zlyhal\", " +
                "ale „čo prekážalo\" a ponúkne menšiu verziu. Dva krátke tréningy sú viac než jeden " +
                "hrdinský plán, ktorý sa nikdy nestane. Zaujímavosť z meta-analýzy: série dní a odznaky " +
                "návyk nezlepšujú a niekedy ho po pretrhnutí zhoršia — preto ich táto appka nemá.",
        ),
    )

    fun podla(id: String): Clanok? = clanky.firstOrNull { it.id == id }
}
