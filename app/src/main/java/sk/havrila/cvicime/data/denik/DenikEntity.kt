package sk.havrila.cvicime.data.denik

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DENNÍK = všetko, čo používateľ vytvoril. Táto databáza sa NIKDY nesmie
 * migrovať deštruktívne - strata histórie tréningov by bola strata celej hodnoty appky.
 * Katalóg cvikov je zámerne v inej databáze, aby sa dal beztrestne prepisovať.
 */

@Entity(tableName = "profil")
data class ProfilEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "vaha_kg") val vahaKg: Double = 80.0,
    @ColumnInfo(name = "vyska_cm") val vyskaCm: Int = 180,
    /** kľúče náradia oddelené čiarkou */
    val naradie: String = "ziadne,podlozka",
    /** kontextová kotva pripomienky, napr. "keď uložím deti" */
    @ColumnInfo(name = "kotva_text") val kotvaText: String = "",
    /** čas, kedy sa kotva kontroluje, v minútach od polnoci; -1 = vypnuté */
    @ColumnInfo(name = "kotva_minuty") val kotvaMinuty: Int = -1,
    /** denný cieľ bielkovín v gramoch (dopočítaný z váhy, dá sa prepísať) */
    @ColumnInfo(name = "bielkoviny_ciel_g") val bielkovinyCielG: Int = 130,
    /** dátum, kedy používateľ začal - kvôli rámovaniu prvého mesiaca */
    @ColumnInfo(name = "zaciatok_epoch_den") val zaciatokEpochDen: Long = 0,
    @ColumnInfo(name = "onboarding_hotovy") val onboardingHotovy: Boolean = false,
    @ColumnInfo(name = "vstupny_test_hotovy") val vstupnyTestHotovy: Boolean = false,
)

@Entity(tableName = "ciel", indices = [Index(value = ["ucastnik_id", "kluc"], unique = true)])
data class CielEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0 = hlavný používateľ, inak [UcastnikEntity.id] (od verzie 2 databázy) */
    @ColumnInfo(name = "ucastnik_id", defaultValue = "0") val ucastnikId: Long = 0,
    /** viď [sk.havrila.cvicime.data.model.DruhCiela] */
    val druh: String,
    /**
     * Pri zručnosti = id rebríka (napr. "lsit").
     * Pri partii = kľúč partie (napr. "lytka").
     * Pri všeobecnom = kľúč ("funkcna_sila", "pohyblivost", "vytrvalost", "chudnutie").
     */
    val kluc: String,
    val nazov: String,
    val aktivny: Boolean = true,
    /** 1 = najvyššia priorita; ovplyvňuje pridelený týždenný objem */
    val priorita: Int = 2,
)

@Entity(tableName = "trening")
data class TreningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** začiatok v milisekundách */
    val zaciatok: Long,
    val koniec: Long? = null,
    /** deň ako epochDay - kvôli rýchlym týždenným dopytom */
    @ColumnInfo(name = "epoch_den") val epochDen: Long,
    /** minúty od polnoci - kvôli pravidlu "retest v rovnakú dennú dobu" */
    @ColumnInfo(name = "minuty_dna") val minutyDna: Int,
    @ColumnInfo(name = "pocit_pred") val pocitPred: Int? = null,
    @ColumnInfo(name = "pocit_po") val pocitPo: Int? = null,
    @ColumnInfo(name = "planovane_minuty") val planovaneMinuty: Int = 30,
    val rodinny: Boolean = false,
    val dokonceny: Boolean = false,
    val poznamka: String = "",
)

@Entity(
    tableName = "seria",
    indices = [Index("trening_id"), Index("cvik_id"), Index("epoch_den")],
)
data class SeriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trening_id") val treningId: Long,
    /** duplikované z tréningu kvôli rýchlemu týždennému súčtu objemu */
    @ColumnInfo(name = "epoch_den") val epochDen: Long,
    /** viď [sk.havrila.cvicime.data.model.Blok] */
    val blok: String,
    val poradie: Int,
    @ColumnInfo(name = "cvik_id") val cvikId: String,
    @ColumnInfo(name = "rebrik_id") val rebrikId: String,
    val uroven: Int,
    val meranie: String,
    /** partie a váhy skopírované z cviku v čase cvičenia (katalóg sa môže zmeniť) */
    val partie: String,
    @ColumnInfo(name = "plan_hodnota") val planHodnota: Int,
    @ColumnInfo(name = "skutocna_hodnota") val skutocnaHodnota: Int? = null,
    /** 0 / 1 / 3 - viď [sk.havrila.cvicime.data.model.Rir] */
    val rir: Int? = null,
    @ColumnInfo(name = "zataz_kg") val zatazKg: Double? = null,
    val preskocena: Boolean = false,
    /** kto sériu odcvičil: 0 = hlavný používateľ, inak [UcastnikEntity.id] */
    @ColumnInfo(name = "ucastnik_id", defaultValue = "0") val ucastnikId: Long = 0,
    /**
     * Varovný príznak tela počas série (režim Panvové dno: tlak, ťah alebo únik).
     * Séria s príznakom zablokuje postup v rebríku - viď [sk.havrila.cvicime.domain.Pravidla.maPostupitVyssie].
     */
    @ColumnInfo(defaultValue = "0") val priznak: Boolean = false,
)

/**
 * Rozpad série na partie. Zámerne samostatná tabuľka a nie parsovanie reťazca -
 * len tak sa dá týždenný objem na partiu spočítať jedným SQL dopytom.
 * Váha 1.0 = priama séria, 0.5 = nepriama (frakčná).
 */
@Entity(tableName = "seria_partia", indices = [Index("seria_id"), Index("partia")])
data class SeriaPartiaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "seria_id") val seriaId: Long,
    val partia: String,
    val vaha: Double,
)

@Entity(tableName = "test", indices = [Index("rebrik_id"), Index("epoch_den")])
data class TestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "epoch_den") val epochDen: Long,
    /**
     * Minúty od polnoci. Sila kolíše počas dňa, takže retest má zmysel len
     * v podobnú dennú dobu ako pôvodný test - appka na to používateľa upozorní.
     */
    @ColumnInfo(name = "minuty_dna") val minutyDna: Int,
    @ColumnInfo(name = "rebrik_id") val rebrikId: String,
    @ColumnInfo(name = "cvik_id") val cvikId: String,
    val uroven: Int,
    val meranie: String,
    val hodnota: Int,
    /** true = prvý (vstupný) test, false = retest */
    @ColumnInfo(name = "je_vstupny") val jeVstupny: Boolean = false,
    @ColumnInfo(name = "ucastnik_id", defaultValue = "0") val ucastnikId: Long = 0,
)

/** Aktuálna úroveň používateľa v každom rebríku. Mení ju engine pri splnení cieľa. */
@Entity(tableName = "uroven_rebrika")
data class UrovenRebrikaEntity(
    @PrimaryKey @ColumnInfo(name = "rebrik_id") val rebrikId: String,
    val uroven: Int,
    @ColumnInfo(name = "zmenene_epoch_den") val zmeneneEpochDen: Long,
)

@Entity(tableName = "dieta")
data class DietaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meno: String,
    val vek: Int,
    /** ARGB farba pruhu dieťaťa v rodinnom režime */
    val farba: Int,
    val aktivne: Boolean = true,
)

@Entity(tableName = "ucast", indices = [Index("trening_id"), Index("dieta_id")])
data class UcastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trening_id") val treningId: Long,
    @ColumnInfo(name = "dieta_id") val dietaId: Long,
    /** koľko krokov ukazovateľa plnenia dieťa spravilo */
    @ColumnInfo(name = "splnene_kroky") val splneneKroky: Int = 0,
)

/**
 * Odomknuté zvieratko. Odomyká sa za ÚČASŤ, nie za výkon - zámerne,
 * aby odmena signalizovala kompetenciu a nie výmenný obchod.
 * Raz odomknuté sa nikdy neodoberá.
 */
@Entity(tableName = "zviera", indices = [Index(value = ["dieta_id", "zviera_kluc"], unique = true)])
data class ZvieraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dieta_id") val dietaId: Long,
    @ColumnInfo(name = "zviera_kluc") val zvieraKluc: String,
    @ColumnInfo(name = "odomknute_epoch_den") val odomknuteEpochDen: Long,
)

@Entity(tableName = "denny_zaznam")
data class DennyZaznamEntity(
    @PrimaryKey @ColumnInfo(name = "epoch_den") val epochDen: Long,
    @ColumnInfo(name = "bielkoviny_splnene") val bielkovinySplnene: Boolean = false,
    val poznamka: String = "",
)

@Entity(tableName = "miera", indices = [Index("epoch_den")])
data class MieraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "epoch_den") val epochDen: Long,
    /** "lytko", "predlaktie", "hrudnik", "pas", "vaha" */
    val typ: String,
    val hodnota: Double,
)

/** Odpoveď na otázku "čo prekážalo?" po vynechanom tréningu - vstup pre riešenie prekážok. */
@Entity(tableName = "prekazka")
data class PrekazkaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "epoch_den") val epochDen: Long,
    /** "cas", "bolest", "nechcelo_sa", "prec", "ine" */
    val druh: String,
)

/**
 * Ďalší dospelý cvičiaci (napr. partnerka). Hlavný používateľ má id 0 a v tejto tabuľke NIE JE -
 * jeho údaje (profil, uroven_rebrika) ostávajú tam, kde boli pred verziou 2.
 * Deti sú zámerne inde ([DietaEntity]) - majú vlastnú pedagogiku, nie progresný engine.
 */
@Entity(tableName = "ucastnik")
data class UcastnikEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meno: String,
    /** ARGB farba avatara */
    val farba: Int,
    /** kľúče obmedzení oddelené čiarkou - viď [sk.havrila.cvicime.data.model.Obmedzenie] */
    val obmedzenia: String = "",
    val aktivny: Boolean = true,
    @ColumnInfo(name = "zaciatok_epoch_den") val zaciatokEpochDen: Long = 0,
)

/** Úroveň ďalšieho cvičiaceho v rebríku (obdoba [UrovenRebrikaEntity] pre hlavného používateľa). */
@Entity(tableName = "uroven_ucastnika", primaryKeys = ["ucastnik_id", "rebrik_id"])
data class UrovenUcastnikaEntity(
    @ColumnInfo(name = "ucastnik_id") val ucastnikId: Long,
    @ColumnInfo(name = "rebrik_id") val rebrikId: String,
    val uroven: Int,
    @ColumnInfo(name = "zmenene_epoch_den") val zmeneneEpochDen: Long,
)
