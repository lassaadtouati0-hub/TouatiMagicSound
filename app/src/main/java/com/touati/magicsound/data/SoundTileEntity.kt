package com.touati.magicsound.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TileState(val value: Int) {
    UNUSED(0),   // الأزرق: لم يُستخدم بعد
    PLAYING(1),  // الأخضر: الأغنية تعمل حالياً
    FINISHED(2); // الأحمر: انتهى التشغيل أو تم إيقافه

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: UNUSED
    }
}

@Entity(tableName = "sound_tiles")
data class SoundTileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: Int,
    val positionOrder: Int,
    val name: String,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val isLooping: Boolean = false,
    val state: Int = TileState.UNUSED.value
)
