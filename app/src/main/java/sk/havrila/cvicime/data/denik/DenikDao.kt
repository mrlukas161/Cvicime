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

    @Query("DELETE FROM ciel WHERE kluc = :kluc")
    suspend fun zmazCiel(kluc: String)

    @Query("SELECT * FROM ciel WHERE aktivny = 1 ORDER BY priorita, nazov")
    suspend fun aktivneCiele(): List<CielEntity>

    @Query("SELECT * FROM ciel WHERE aktivny = 1 ORDER BY priorita, nazov")
    fun aktivneCieleTok(): Flow<List<CielEntity>>

    @Query("SELECT * FROM ciel WHERE druh = :druh AND aktivny = 1")
    suspend fun cieleDruhu(druh: String): List<CielEntity>

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

    @Query("SELECT COUNT(*) FROM trening WHERE dokonceny = 1 AND epoch_den >= :od")
    suspend fun pocetTreningovOd(od: Long): Int

    // ---------- séria ----------

    @Insert
    suspend fun vlozSeriu(seria: SeriaEntity): Long

    @Update
    suspend fun aktualizujSeriu(seria: SeriaEntity)

    @Insert
    suspend fun vlozSeriaPartie(polozky: List<SeriaPartiaEntity>)

    @Query("SELECT * FROM seria WHERE trening_id = :treningId ORDER BY poradie")
    suspend fun serieTreningu(treningId: Long): List<SeriaEntity>

    @Query("SELECT * FROM seria WHERE trening_id = :treningId ORDER BY poradie")
    fun serieTreninguTok(treningId: Long): Flow<List<SeriaEntity>>

    @Query(
        """
        SELECT * FROM seria
        WHERE rebrik_id = :rebrikId AND skutocna_hodnota IS NOT NULL
        ORDER BY epoch_den DESC, id DESC LIMIT :limit
        """
    )
    suspend fun poslednéSerieRebrika(rebrikId: String, limit: Int = 12): List<SeriaEntity>

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
          AND s.blok IN ('sila', 'kondicia')
        GROUP BY sp.partia
        """
    )
    suspend fun objemPodlaPartii(odDna: Long, doDna: Long): List<ObjemPartie>

    /** Kedy bola ktorá partia naposledy zaťažená - vstup pre výber cviku na dnes. */
    @Query(
        """
        SELECT sp.partia AS partia, MAX(s.epoch_den) AS den
        FROM seria_partia sp
        JOIN seria s ON s.id = sp.seria_id
        WHERE s.skutocna_hodnota IS NOT NULL AND s.preskocena = 0
        GROUP BY sp.partia
        """
    )
    suspend fun poslednaZatazPartii(): List<PoslednaZataz>

    // ---------- test ----------

    @Insert
    suspend fun vlozTest(test: TestEntity): Long

    @Query("SELECT * FROM test WHERE rebrik_id = :rebrikId ORDER BY epoch_den")
    suspend fun testyRebrika(rebrikId: String): List<TestEntity>

    @Query("SELECT * FROM test ORDER BY epoch_den DESC")
    fun vsetkyTestyTok(): Flow<List<TestEntity>>

    @Query("SELECT * FROM test WHERE je_vstupny = 1 ORDER BY epoch_den LIMIT 1")
    suspend fun prvyVstupnyTest(): TestEntity?

    @Query("SELECT MAX(epoch_den) FROM test")
    suspend fun poslednyTestDen(): Long?

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
}

data class PocetPrekazok(val druh: String, val pocet: Int)
