package sk.havrila.cvicime.data.katalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KatalogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun vlozRebriky(rebriky: List<RebrikEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun vlozCviky(cviky: List<CvikEntity>)

    @Query("DELETE FROM rebrik")
    suspend fun zmazRebriky()

    @Query("DELETE FROM cvik")
    suspend fun zmazCviky()

    @Transaction
    suspend fun prepisKatalog(rebriky: List<RebrikEntity>, cviky: List<CvikEntity>) {
        zmazCviky()
        zmazRebriky()
        vlozRebriky(rebriky)
        vlozCviky(cviky)
    }

    @Query("SELECT COUNT(*) FROM cvik")
    suspend fun pocetCvikov(): Int

    @Query("SELECT * FROM rebrik ORDER BY poradie, nazov")
    fun rebrikyTok(): Flow<List<RebrikEntity>>

    @Query("SELECT * FROM rebrik ORDER BY poradie, nazov")
    suspend fun rebriky(): List<RebrikEntity>

    @Query("SELECT * FROM rebrik WHERE id = :id")
    suspend fun rebrik(id: String): RebrikEntity?

    @Query("SELECT * FROM rebrik WHERE skupina = :skupina ORDER BY poradie")
    suspend fun rebrikyPodlaSkupiny(skupina: String): List<RebrikEntity>

    @Query("SELECT * FROM cvik WHERE rebrik_id = :rebrikId ORDER BY uroven")
    suspend fun cvikyRebrika(rebrikId: String): List<CvikEntity>

    @Query("SELECT * FROM cvik WHERE rebrik_id = :rebrikId ORDER BY uroven")
    fun cvikyRebrikaTok(rebrikId: String): Flow<List<CvikEntity>>

    @Query("SELECT * FROM cvik WHERE id = :id")
    suspend fun cvik(id: String): CvikEntity?

    @Query("SELECT * FROM cvik WHERE rebrik_id = :rebrikId AND uroven = :uroven")
    suspend fun cvik(rebrikId: String, uroven: Int): CvikEntity?

    @Query("SELECT MAX(uroven) FROM cvik WHERE rebrik_id = :rebrikId")
    suspend fun maxUroven(rebrikId: String): Int?

    @Query("SELECT * FROM cvik WHERE je_detsky = 1 ORDER BY rebrik_id, uroven")
    suspend fun detskeCviky(): List<CvikEntity>

    @Query("SELECT * FROM cvik WHERE je_detsky = 0")
    suspend fun vsetkyDospeleCviky(): List<CvikEntity>
}
