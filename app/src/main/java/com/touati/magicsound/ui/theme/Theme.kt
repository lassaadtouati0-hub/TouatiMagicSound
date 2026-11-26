package com.touati.magicsound.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TileBlueUnused = Color(0xFF1565C0)      // الأزرق: لم يُستخدم بعد
val TileGreenPlaying = Color(0xFF2E7D32)    // الأخضر: الأغنية تعمل حالياً
val TileRedFinished = Color(0xFFC62828)     // الأحمر: انتهى التشغيل أو تم إيقافه

val DarkBackground = Color(0xFF121417)
val DarkSurface = Color(0xFF1E2228)

private val AppColorScheme = darkColorScheme(
    primary = Color(0xFF42A5F5),
    secondary = Color(0xFF66BB6A),
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun TouatiMagicSoundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
