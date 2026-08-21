package dev.raflos.cocina.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppStateEntity::class], version = 2, exportSchema = false)
abstract class CocinaDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao

    companion object {
        @Volatile private var instance: CocinaDatabase? = null

        fun get(context: Context): CocinaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CocinaDatabase::class.java,
                    "cocina.db",
                )
                    // La cache local es descartable: se puede volver a derivar de los archivos
                    // del repo, así que no vale la pena escribir migraciones reales.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
