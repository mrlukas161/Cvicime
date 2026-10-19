package sk.havrila.cvicime.data.denik

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        UcastnikEntity::class,
        UrovenUcastnikaEntity::class,
    ],
    version = 3,
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
                    .addMigrations(MIGRACIA_1_2, MIGRACIA_2_3)
                    .build()
                    .also { instancia = it }
            }
    }
}

/**
 * 1 → 2: ďalší dospelí cvičiaci (0.7). Existujúce riadky dostanú ucastnik_id = 0,
 * teda patria hlavnému používateľovi - história sa nemení ani o riadok.
 * SQL musí presne zodpovedať tomu, čo by Room vygeneroval (overuje DenikMigraciaTest).
 */
val MIGRACIA_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ucastnik` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`meno` TEXT NOT NULL, `farba` INTEGER NOT NULL, `obmedzenia` TEXT NOT NULL, " +
                "`aktivny` INTEGER NOT NULL, `zaciatok_epoch_den` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `uroven_ucastnika` (`ucastnik_id` INTEGER NOT NULL, " +
                "`rebrik_id` TEXT NOT NULL, `uroven` INTEGER NOT NULL, `zmenene_epoch_den` INTEGER NOT NULL, " +
                "PRIMARY KEY(`ucastnik_id`, `rebrik_id`))"
        )
        db.execSQL("ALTER TABLE `seria` ADD COLUMN `ucastnik_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `test` ADD COLUMN `ucastnik_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ciel` ADD COLUMN `ucastnik_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("DROP INDEX IF EXISTS `index_ciel_kluc`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ciel_ucastnik_id_kluc` ON `ciel` (`ucastnik_id`, `kluc`)")
    }
}

/**
 * 2 → 3: príznak série (režim Panvové dno). Existujúce série dostanú priznak = 0 -
 * nič sa nemaže ani neprepisuje, postupy v histórii ostávajú platné.
 */
val MIGRACIA_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `seria` ADD COLUMN `priznak` INTEGER NOT NULL DEFAULT 0")
    }
}
