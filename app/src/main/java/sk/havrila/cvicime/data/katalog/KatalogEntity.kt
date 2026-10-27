package sk.havrila.cvicime.data.katalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * KATALOG = kniznica rebrikov a cvikov.
 *
 * Zamerne v SAMOSTATNEJ databaze (katalog.db) oddelenej od denníka:
 * katalog sa pri kazdej novej verzii appky prepise z assets/katalog.json,
 * takze si moze dovolit destruktivnu migraciu. Historia treningov v denik.db
 * sa NIKDY nesmie stratit, preto tam destruktivna migracia povolena nie je.
 */
@Entity(tableName = "rebrik")
data class RebrikEntity(
    @PrimaryKey val id: String,
    val nazov: String,
    val popis: String,
    /** viď [sk.havrila.cvicime.data.model.Skupina] */
    val skupina: String,
    /** true = rebrík vedie ku konkrétnej zručnosti (L-sit, stojka, muscle-up) */
    @ColumnInfo(name = "je_zrucnost") val jeZrucnost: Boolean,
    val poradie: Int,
)

@Entity(
    tableName = "cvik",
    indices = [Index("rebrik_id"), Index(value = ["rebrik_id", "uroven"], unique = true)],
)
data class CvikEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "rebrik_id") val rebrikId: String,
    /** stupeň v rebríku, 1 = najľahší */
    val uroven: Int,
    val nazov: String,
    /** viď [sk.havrila.cvicime.data.model.Meranie] */
    val meranie: String,
    /** spodná hranica cieľa (opakovania alebo sekundy) */
    @ColumnInfo(name = "ciel_min") val cielMin: Int,
    /** horná hranica cieľa - po jej splnení vo VŠETKÝCH sériách sa postupuje o stupeň vyššie */
    @ColumnInfo(name = "ciel_max") val cielMax: Int,
    /** kľúče náradia oddelené čiarkou, prázdne = netreba nič */
    val naradie: String,
    /**
     * Partie a ich váha, napr. "chrbat:1,biceps:0.5".
     * Váha 1 = priama séria, 0.5 = nepriama (frakčná) - tak sa počíta aj v literatúre.
     */
    val partie: String,
    /** technické pokyny, oddelené znakom | */
    val cue: String,
    /** typické chyby, oddelené znakom | */
    val chyby: String,
    /** true = cvik je určený pre detskú vrstvu rodinného režimu */
    @ColumnInfo(name = "je_detsky") val jeDetsky: Boolean = false,
    /** kľúč zvieratka, ktoré sa odomkne (len pri detských cvikoch) */
    @ColumnInfo(name = "zviera") val zviera: String? = null,
    /**
     * Značky záťaže oddelené čiarkou ("skok", "tlak_brucho", "vis", "nad_hlavu").
     * Podľa nich engine vylučuje cviky pri obmedzeniach - viď [sk.havrila.cvicime.data.model.Obmedzenie].
     */
    @ColumnInfo(name = "znacky", defaultValue = "") val znacky: String = "",
    /**
     * Id vzoru animácie z assets/pohyby.json (napr. "klik-stena", "australsky-pritah").
     * Null = kreslí sa predvolený pohyb rebríka. Viď [sk.havrila.cvicime.ui.komponenty.pohyb.Pohyby].
     */
    @ColumnInfo(name = "pohyb") val pohyb: String? = null,
)
