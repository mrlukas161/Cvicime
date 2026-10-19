package sk.havrila.cvicime.data.katalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Katalóg cvikov. Obsah pochádza z assets/katalog.json a pri každej novej verzii
 * appky sa celý prepíše, takže deštruktívna migrácia je tu bezpečná a zámerná.
 * Používateľské dáta v tejto databáze NIE SÚ - tie sú v denik.db.
 */
@Database(
    entities = [RebrikEntity::class, CvikEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class KatalogDatabase : RoomDatabase() {
    abstract fun dao(): KatalogDao

    companion object {
        @Volatile
        private var instancia: KatalogDatabase? = null

        fun ziskaj(context: Context): KatalogDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    KatalogDatabase::class.java,
                    "katalog.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
