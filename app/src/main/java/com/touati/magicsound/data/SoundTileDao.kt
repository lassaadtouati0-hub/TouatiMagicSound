package com.touati.magicsound.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundTileDao {
    @Query("SELECT * FROM sound_tiles ORDER BY positionOrder ASC")
    fun getAllTilesFlow(): Flow<List<SoundTileEntity>>

    @Query("SELECT * FROM sound_tiles ORDER BY positionOrder ASC")
    suspend fun getAllTilesList(): List<SoundTileEntity>

    @Query("SELECT COUNT(*) FROM sound_tiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiles: List<SoundTileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tile: SoundTileEntity): Long

    @Update
    suspend fun update(tile: SoundTileEntity)

    @Update
    suspend fun updateAll(tiles: List<SoundTileEntity>)

    @Delete
    suspend fun delete(tile: SoundTileEntity)

    @Query("UPDATE sound_tiles SET state = 0")
    suspend fun resetAllStatesToUnused()

    @Query("UPDATE sound_tiles SET state = 2 WHERE state = 1")
    suspend fun markPlayingAsFinished()
}
