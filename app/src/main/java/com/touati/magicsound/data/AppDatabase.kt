package com.touati.magicsound.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SoundTileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundTileDao(): SoundTileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "touati_magic_sound_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultTiles(database.soundTileDao())
                    }
                }
            }
        }

        suspend fun populateDefaultTiles(dao: SoundTileDao) {
            if (dao.getCount() == 0) {
                val defaultTiles = (1..20).map { index ->
                    SoundTileEntity(
                        number = index,
                        positionOrder = index - 1,
                        name = "صوت $index",
                        imageUri = null,
                        audioUri = null,
                        isLooping = false,
                        state = TileState.UNUSED.value
                    )
                }
                dao.insertAll(defaultTiles)
            }
        }
    }
}
