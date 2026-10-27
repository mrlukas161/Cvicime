package sk.havrila.cvicime.data.denik

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Výsledok týždenného súčtu objemu na partiu. */
data class ObjemPartie(val partia: String, val serie: Double)

/** Posledný deň, kedy bola partia zaťažená. */
data class PoslednaZataz(val partia: String, @androidx.room.ColumnInfo(name = "den") val den: Long)

@Dao
interface DenikDao {

    // ---------- profil ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozProfil(profil: ProfilEntity)

    @Query("SELECT * FROM profil WHERE id = 1")
    suspend fun profil(): ProfilEntity?

    @Query("SELECT * FROM profil WHERE id = 1")
    fun profilTok(): Flow<ProfilEntity?>

    // ---------- ciele ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozCiel(ciel: CielEntity): Long

    // Ciele, série, testy a úrovne majú od verzie 2 rozmer "kto" (ucastnik_id).
    // Predvolená 0 = hlavný používateľ, takže staré volania sa správajú presne ako predtým.

    @Query("DELETE FROM ciel WHERE kluc = :kluc AND ucastnik_id = :ucastnikId")
    suspend fun zmazCiel(kluc: String, ucastnikId: Long = 0)

    @Query("SELECT * FROM ciel WHERE aktivny = 1 AND ucastnik_id = :ucastnikId ORDER BY priorita, nazov")
    suspend fun aktivneCiele(ucastnikId: Long = 0): List<CielEntity>

    @Query("SELECT * FROM ciel WHERE aktivny = 1 AND ucastnik_id = :ucastnikId ORDER BY priorita, nazov")
    fun aktivneCieleTok(ucastnikId: Long = 0): Flow<List<CielEntity>>

    @Query("SELECT * FROM ciel WHERE druh = :druh AND aktivny = 1 AND ucastnik_id = :ucastnikId")
    suspend fun cieleDruhu(druh: String, ucastnikId: Long = 0): List<CielEntity>

    // ---------- tréning ----------

    @Insert
    suspend fun vlozTrening(trening: TreningEntity): Long

    @Update
    suspend fun aktualizujTrening(trening: TreningEntity)

    @Query("SELECT * FROM trening WHERE id = :id")
    suspend fun trening(id: Long): TreningEntity?

    @Query("SELECT * FROM trening WHERE dokonceny = 1 ORDER BY zaciatok DESC LIMIT :limit")
    fun posledneTreningyTok(limit: Int = 60): Flow<List<TreningEntity>>

    @Query("SELECT * FROM trening WHERE dokonceny = 1 ORDER BY zaciatok DESC LIMIT :limit")
    suspend fun posledneTreningy(limit: Int): List<TreningEntity>

    @Query("SELECT * FROM trening WHERE dokonceny = 1 ORDER BY zaciatok DESC LIMIT 1")
    suspend fun poslednyTrening(): TreningEntity?

    @Query("SELECT * FROM trening WHERE dokonceny = 0 ORDER BY zaciatok DESC LIMIT 1")
    suspend fun nedokoncenyTrening(): TreningEntity?

    /** Zmaže tréning aj s jeho sériami a ich rozpadom na partie (len prázdny, neodcvičený tréning). */
    @androidx.room.Transaction
    suspend fun zmazTrening(id: Long) {
        zmazPartieTreningu(id)
        zmazSerieTreningu(id)
        zmazTreningRiadok(id)
    }

    @Query("DELETE FROM seria_partia WHERE seria_id IN (SELECT id FROM seria WHERE trening_id = :id)")
    suspend fun zmazPartieTreningu(id: Long)

    @Query("DELETE FROM seria WHERE trening_id = :id")
    suspend fun zmazSerieTreningu(id: Long)

    @Query("DELETE FROM trening WHERE id = :id")
    suspend fun zmazTreningRiadok(id: Long)

    @Query("SELECT COUNT(*) FROM trening WHERE dokonceny = 1 AND epoch_den >= :od")
    suspend fun pocetTreningovOd(od: Long): Int

    // ---------- séria ----------

    @Insert
    suspend fun vlozSeriu(seria: SeriaEntity): Long

    @Update
    suspend fun aktualizujSeriu(seria: SeriaEntity)

    @Query("UPDATE seria SET priznak = :priznak WHERE id = :id")
    suspend fun nastavPriznak(id: Long, priznak: Boolean)

    @Insert
    suspend fun vlozSeriaPartie(polozky: List<SeriaPartiaEntity>)

    @Query("SELECT * FROM seria WHERE trening_id = :treningId ORDER BY poradie")
    suspend fun serieTreningu(treningId: Long): List<SeriaEntity>

    @Query("SELECT * FROM seria WHERE trening_id = :treningId ORDER BY poradie")
    fun serieTreninguTok(treningId: Long): Flow<List<SeriaEntity>>

    @Query(
        """
        SELECT * FROM seria
        WHERE rebrik_id = :rebrikId AND skutocna_hodnota IS NOT NULL AND ucastnik_id = :ucastnikId
        ORDER BY epoch_den DESC, id DESC LIMIT :limit
        """
    )
    suspend fun poslednéSerieRebrika(rebrikId: String, limit: Int = 12, ucastnikId: Long = 0): List<SeriaEntity>

    /**
     * Týždenný objem na svalovú partiu.
     * Nie je to uložený stĺpec - dopočítava sa zo sérií, takže sa nikdy nerozsynchronizuje.
     * Rátajú sa len odcvičené série zo silového a kondičného bloku
     * (rozohriatie, mobilita a skill sa do objemu nezapočítavajú - zámerne, sú nízkozáťažové).
     */
    @Query(
        """
        SELECT sp.partia AS partia, SUM(sp.vaha) AS serie
        FROM seria_partia sp
        JOIN seria s ON s.id = sp.seria_id
        WHERE s.epoch_den >= :odDna AND s.epoch_den <= :doDna
          AND s.skutocna_hodnota IS NOT NULL
          AND s.preskocena = 0
          AND s.blok IN ('sila', 'kondicia', 'kruh')
          AND s.ucastnik_id = :ucastnikId
        GROUP BY sp.partia
        """
    )
    suspend fun objemPodlaPartii(odDna: Long, doDna: Long, ucastnikId: Long = 0): List<ObjemPartie>

    /** Kedy bola ktorá partia naposledy zaťažená - vstup pre výber cviku na dnes. */
    @Query(
        """
        SELECT sp.partia AS partia, MAX(s.epoch_den) AS den
        FROM seria_partia sp
        JOIN seria s ON s.id = sp.seria_id
        WHERE s.skutocna_hodnota IS NOT NULL AND s.preskocena = 0 AND s.ucastnik_id = :ucastnikId
        GROUP BY sp.partia
        """
    )
    suspend fun poslednaZatazPartii(ucastnikId: Long = 0): List<PoslednaZataz>

    // ---------- test ----------

    @Insert
    suspend fun vlozTest(test: TestEntity): Long

    @Query("SELECT * FROM test WHERE rebrik_id = :rebrikId AND ucastnik_id = :ucastnikId ORDER BY epoch_den")
    suspend fun testyRebrika(rebrikId: String, ucastnikId: Long = 0): List<TestEntity>

    @Query("SELECT * FROM test WHERE ucastnik_id = :ucastnikId ORDER BY epoch_den DESC")
    fun vsetkyTestyTok(ucastnikId: Long = 0): Flow<List<TestEntity>>

    @Query("SELECT * FROM test WHERE ucastnik_id = :ucastnikId ORDER BY epoch_den")
    suspend fun vsetkyTesty(ucastnikId: Long = 0): List<TestEntity>

    @Query("SELECT * FROM miera ORDER BY epoch_den")
    suspend fun vsetkyMiery(): List<MieraEntity>

    /** Najlepšia séria daného rebríka po dňoch - podklad grafu sily. */
    @Query(
        """
        SELECT epoch_den AS den, MAX(skutocna_hodnota) AS hodnota FROM seria
        WHERE rebrik_id = :rebrikId AND skutocna_hodnota IS NOT NULL AND preskocena = 0
          AND blok IN ('sila', 'skill') AND ucastnik_id = :ucastnikId
        GROUP BY epoch_den ORDER BY epoch_den
        """
    )
    suspend fun grafRebrika(rebrikId: String, ucastnikId: Long = 0): List<BodGrafu>

    @Query("SELECT * FROM test WHERE je_vstupny = 1 AND ucastnik_id = :ucastnikId ORDER BY epoch_den LIMIT 1")
    suspend fun prvyVstupnyTest(ucastnikId: Long = 0): TestEntity?

    @Query("SELECT MAX(epoch_den) FROM test WHERE ucastnik_id = :ucastnikId")
    suspend fun poslednyTestDen(ucastnikId: Long = 0): Long?

    // ---------- úrovne v rebríkoch ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozUroven(uroven: UrovenRebrikaEntity)

    @Query("SELECT * FROM uroven_rebrika")
    suspend fun urovne(): List<UrovenRebrikaEntity>

    @Query("SELECT * FROM uroven_rebrika")
    fun urovneTok(): Flow<List<UrovenRebrikaEntity>>

    @Query("SELECT * FROM uroven_rebrika WHERE rebrik_id = :rebrikId")
    suspend fun uroven(rebrikId: String): UrovenRebrikaEntity?

    // ---------- deti ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozDieta(dieta: DietaEntity): Long

    @Query("SELECT * FROM dieta WHERE aktivne = 1 ORDER BY vek DESC")
    suspend fun deti(): List<DietaEntity>

    @Query("SELECT * FROM dieta WHERE aktivne = 1 ORDER BY vek DESC")
    fun detiTok(): Flow<List<DietaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozUcast(ucast: UcastEntity): Long

    @Query("SELECT * FROM ucast WHERE trening_id = :treningId")
    suspend fun ucastiTreningu(treningId: Long): List<UcastEntity>

    /** Odomknutie je nevratné - IGNORE zabráni prepísaniu dátumu prvého odomknutia. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun odomkniZviera(zviera: ZvieraEntity): Long

    @Query("SELECT * FROM zviera WHERE dieta_id = :dietaId")
    suspend fun zvierataDietata(dietaId: Long): List<ZvieraEntity>

    @Query("SELECT * FROM zviera")
    fun vsetkyZvierataTok(): Flow<List<ZvieraEntity>>

    // ---------- denný záznam, miery, prekážky ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozDennyZaznam(zaznam: DennyZaznamEntity)

    @Query("SELECT * FROM denny_zaznam WHERE epoch_den = :den")
    suspend fun dennyZaznam(den: Long): DennyZaznamEntity?

    @Query("SELECT * FROM denny_zaznam WHERE epoch_den = :den")
    fun dennyZaznamTok(den: Long): Flow<DennyZaznamEntity?>

    @Insert
    suspend fun vlozMieru(miera: MieraEntity)

    @Query("SELECT * FROM miera ORDER BY epoch_den")
    fun mieryTok(): Flow<List<MieraEntity>>

    @Insert
    suspend fun vlozPrekazku(prekazka: PrekazkaEntity)

    @Query("SELECT druh, COUNT(*) AS pocet FROM prekazka WHERE epoch_den >= :od GROUP BY druh ORDER BY pocet DESC")
    suspend fun najcastejsiePrekazky(od: Long): List<PocetPrekazok>

    // ---------- štatistiky (0.6) - len čítanie, schéma sa nemení ----------

    /** Dni s dokončeným tréningom - týždenný pás na Domove, kalendár na Pokroku. */
    @Query("SELECT DISTINCT epoch_den FROM trening WHERE dokonceny = 1 AND epoch_den >= :odDna ORDER BY epoch_den")
    fun dniTreningovTok(odDna: Long): Flow<List<Long>>

    /** Počet dokončených tréningov a ich celkový čas. */
    @Query(
        """
        SELECT COUNT(*) AS pocet, COALESCE(SUM(koniec - zaciatok), 0) AS trvanieMs
        FROM trening WHERE dokonceny = 1 AND koniec IS NOT NULL AND epoch_den >= :odDna
        """
    )
    fun suhrnTreningovTok(odDna: Long = 0): Flow<SuhrnTreningov>

    @Query("SELECT COUNT(*) FROM seria WHERE skutocna_hodnota IS NOT NULL AND preskocena = 0 AND ucastnik_id = :ucastnikId")
    fun pocetSeriiTok(ucastnikId: Long = 0): Flow<Int>

    /** Najlepší výkon na každom cviku mimo daného tréningu - porovnanie pre osobný rekord. */
    @Query(
        """
        SELECT cvik_id AS cvikId, MAX(skutocna_hodnota) AS hodnota FROM seria
        WHERE skutocna_hodnota IS NOT NULL AND preskocena = 0 AND trening_id != :okremTreningu
          AND ucastnik_id = :ucastnikId
        GROUP BY cvik_id
        """
    )
    suspend fun maximaCvikov(okremTreningu: Long, ucastnikId: Long = 0): List<MaxCviku>

    /** Všetky odcvičené série chronologicky - história osobných rekordov sa počíta v Kotline. */
    @Query("SELECT * FROM seria WHERE skutocna_hodnota IS NOT NULL AND preskocena = 0 AND ucastnik_id = :ucastnikId ORDER BY epoch_den, id")
    suspend fun vsetkyOdcviceneSerie(ucastnikId: Long = 0): List<SeriaEntity>

    // ---------- ďalší dospelí cvičiaci (0.7) ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozUcastnika(ucastnik: UcastnikEntity): Long

    @Query("SELECT * FROM ucastnik WHERE aktivny = 1 ORDER BY id")
    suspend fun ucastnici(): List<UcastnikEntity>

    @Query("SELECT * FROM ucastnik WHERE aktivny = 1 ORDER BY id")
    fun ucastniciTok(): Flow<List<UcastnikEntity>>

    /** Aj neaktívni - Nastavenia ich musia vedieť znova zapnúť. */
    @Query("SELECT * FROM ucastnik ORDER BY id")
    fun vsetciUcastniciTok(): Flow<List<UcastnikEntity>>

    @Query("SELECT * FROM ucastnik WHERE id = :id")
    suspend fun ucastnik(id: Long): UcastnikEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ulozUrovenUcastnika(uroven: UrovenUcastnikaEntity)

    @Query("SELECT * FROM uroven_ucastnika WHERE ucastnik_id = :ucastnikId")
    suspend fun urovneUcastnika(ucastnikId: Long): List<UrovenUcastnikaEntity>

    @Query("SELECT * FROM uroven_ucastnika WHERE ucastnik_id = :ucastnikId")
    fun urovneUcastnikaTok(ucastnikId: Long): Flow<List<UrovenUcastnikaEntity>>
}

/** Súhrn dokončených tréningov. */
data class SuhrnTreningov(val pocet: Int, val trvanieMs: Long)

/** Najlepšia hodnota (opakovania alebo sekundy) dosiahnutá na cviku. */
data class MaxCviku(val cvikId: String, val hodnota: Int)

data class PocetPrekazok(val druh: String, val pocet: Int)

data class BodGrafu(val den: Long, val hodnota: Int)
