package fr.olegueyan.algomix.infrastructure.persistence.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CollectionEntity::class,
        AlgorithmSheetEntity::class,
        AlgorithmEntity::class,
        ScrambleEntity::class,
        TagEntity::class,
        SheetTagEntity::class,
        ScrambleTagEntity::class,
        TimerEntryEntity::class,
        SyncMetadataEntity::class,
        OutboxEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AlgomixDatabase : RoomDatabase() {
    abstract fun localPersistenceDao(): LocalPersistenceDao

    companion object {
        private const val DATABASE_NAME = "algomix.db"

        fun create(context: Context): AlgomixDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AlgomixDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
