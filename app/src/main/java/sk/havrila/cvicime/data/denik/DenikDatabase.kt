package sk.havrila.cvicime.data.denik

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Denník používateľa - história tréningov, sérií, testov, cieľov, detí.
 *
 * TU NIKDY NEPOUŽÍVAŤ fallbackToDestructiveMigration.
 * Strata tejto databázy = strata celej hodnoty appky. Každá zmena schémy
 * musí mať riadnu @Migration, aj keď je to otrava.
 */
@Database(
    entities = [
        ProfilEntity::class,
        CielEntity::class,
        TreningEntity::class,
        SeriaEntity::class,
        SeriaPartiaEntity::class,
        TestEntity::class,
        UrovenRebrikaEntity::class,
        DietaEntity::class,
        UcastEntity::class,
        ZvieraEntity::class,
        DennyZaznamEntity::class,
        MieraEntity::class,
        PrekazkaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DenikDatabase : RoomDatabase() {
    abstract fun dao(): DenikDao

    companion object {
        @Volatile
        private var instancia: DenikDatabase? = null

        fun ziskaj(context: Context): DenikDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    DenikDatabase::class.java,
                    "denik.db",
                )
                    // Zámerne BEZ fallbackToDestructiveMigration - viď komentár vyššie.
                    .build()
                    .also { instancia = it }
            }
    }
}
