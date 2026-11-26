package com.touati.magicsound.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.touati.magicsound.data.*
import com.touati.magicsound.service.PlaybackController
import com.touati.magicsound.util.UriUtils
import kotlinx.coroutines.*
import org.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dao: SoundTileDao, playbackController: PlaybackController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles by dao.getAllTilesFlow().collectAsState(initial = emptyList())
    val playbackState by playbackController.playbackState.collectAsState()
    val prefs = remember { context.getSharedPreferences("touati_magic_prefs", Context.MODE_PRIVATE) }
    var allowDestructive by remember { mutableStateOf(prefs.getBoolean("allow_destructive", false)) }
    var allowRestore by remember { mutableStateOf(prefs.getBoolean("allow_restore", false)) }
    var showSettings by remember { mutableStateOf(false) }
    var editingTile by remember { mutableStateOf<SoundTileEntity?>(null) }
    var showNewShow by remember { mutableStateOf(false) }
    var pendingJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (dao.getCount() == 0) {
            dao.insertAll((1..20).map { i ->
                SoundTileEntity(number = i, name = "مؤثر $i", audioUri = null, imageUri = null, state = TileState.UNUSED.value, isLooping = false, positionOrder = i)
            })
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            playbackController.clearError()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                dao.getAllTilesList().forEach { t ->
                    arr.put(JSONObject().apply {
                        put("number", t.number); put("name", t.name)
                        put("audioUri", t.audioUri ?: ""); put("imageUri", t.imageUri ?: "")
                        put("isLooping", t.isLooping); put("positionOrder", t.positionOrder)
                    })
                }
                val root = JSONObject().apply { put("app", "TouatiMagicSound"); put("version", 1); put("tiles", arr) }
                context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) }
                withContext(Dispatchers.Main) { Toast.makeText(context, "تم الحفظ", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) {}
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val txt = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!txt.isNullOrBlank()) withContext(Dispatchers.Main) { pendingJson = txt }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B1224),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF101B34)).statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Touati Magic Sound", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showNewShow = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)), shape = RoundedCornerShape(10.dp)) {
                        Text("عرض جديد", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "الإعدادات", tint = Color.White) }
                }
            }
        }
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            items(tiles, key = { it.id }) { tile ->
                SoundTileCard(
                    tile = tile,
                    playbackState = playbackState,
                    onTileClick = {
                        if (tile.audioUri.isNullOrBlank()) editingTile = tile
                        else scope.launch { dao.markPlayingAsFinished(); playbackController.playTile(tile) }
                    },
                    onPauseClick = { playbackController.pause() },
                    onResumeClick = { playbackController.resume() },
                    onRestartClick = { playbackController.restart() },
                    onSeek = { playbackController.seekTo(it) },
                    onToggleLoop = {
                        scope.launch {
                            val loop = !tile.isLooping
                            dao.update(tile.copy(isLooping = loop))
                            if (playbackState.activeTileId == tile.id) playbackController.updateRepeatMode(loop)
                        }
                    }
                )
            }
        }
    }
    if (showNewShow) {
        AlertDialog(
            onDismissRequest = { showNewShow = false },
            title = { Text("عرض جديد") },
            text = { Text("إيقاف الصوت وإعادة كل المربعات للأزرق؟") },
            confirmButton = { TextButton(onClick = { scope.launch { playbackController.stopAll(); dao.resetAllStatesToUnused(); showNewShow = false } }) { Text("نعم") } },
            dismissButton = { TextButton(onClick = { showNewShow = false }) { Text("إلغاء") } }
        )
    }

    editingTile?.let { tile ->
        EditTileDialog(
            tile = tile, allowDestructive = allowDestructive, onDismiss = { editingTile = null },
            onSave = { scope.launch { dao.update(it); editingTile = null } },
            onDelete = { scope.launch { if (playbackState.activeTileId == it.id) playbackController.stopAll(); dao.delete(it); editingTile = null } }
        )
    }

    if (showSettings) {
        SettingsDialog(
            tiles = tiles, allowDestructive = allowDestructive, allowRestore = allowRestore,
            onToggleDestructive = { allowDestructive = it; prefs.edit().putBoolean("allow_destructive", it).apply() },
            onToggleRestore = { allowRestore = it; prefs.edit().putBoolean("allow_restore", it).apply() },
            onEditTile = { showSettings = false; editingTile = it },
            onAddTile = {
                scope.launch {
                    val n = (tiles.maxOfOrNull { it.number } ?: 0) + 1
                    dao.insert(SoundTileEntity(number = n, name = "مؤثر $n", audioUri = null, imageUri = null, state = TileState.UNUSED.value, isLooping = false, positionOrder = n))
                }
            },
            onSetCount = { target ->
                scope.launch {
                    val list = dao.getAllTilesList()
                    if (target > list.size) {
                        val start = (list.maxOfOrNull { it.number } ?: 0) + 1
                        dao.insertAll((0 until target - list.size).map { idx ->
                            SoundTileEntity(number = start + idx, name = "مؤثر ${start + idx}", audioUri = null, imageUri = null, state = TileState.UNUSED.value, isLooping = false, positionOrder = start + idx)
                        })
                    } else if (target < list.size && allowDestructive) {
                        list.takeLast(list.size - target).forEach { dao.delete(it) }
                    }
                }
            },
            onBackup = { backupLauncher.launch("TouatiMagicSound.json") },
            onRestore = { if (allowRestore) restoreLauncher.launch(arrayOf("application/json", "*/*")) else Toast.makeText(context, "فعّل السماح بالاسترجاع أولاً", Toast.LENGTH_SHORT).show() },
            onDismiss = { showSettings = false }
        )
    }

    pendingJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingJson = null },
            title = { Text("استرجاع JSON") },
            text = { Text("استبدال القائمة الحالية؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val arr = JSONObject(json).getJSONArray("tiles")
                            val list = (0 until arr.length()).map { i ->
                                val o = arr.getJSONObject(i)
                                SoundTileEntity(number = o.optInt("number", i + 1), name = o.optString("name", "مؤثر ${i + 1}"), audioUri = o.optString("audioUri").ifBlank { null }, imageUri = o.optString("imageUri").ifBlank { null }, state = TileState.UNUSED.value, isLooping = o.optBoolean("isLooping"), positionOrder = o.optInt("positionOrder", i + 1))
                            }
                            withContext(Dispatchers.Main) { playbackController.stopAll() }
                            dao.getAllTilesList().forEach { dao.delete(it) }
                            dao.insertAll(list)
                            withContext(Dispatchers.Main) { pendingJson = null; showSettings = false }
                        } catch (_: Exception) { withContext(Dispatchers.Main) { pendingJson = null } }
                    }
                }) { Text("نعم") }
            },
            dismissButton = { TextButton(onClick = { pendingJson = null }) { Text("إلغاء") } }
        )
    }
}
@Composable
private fun EditTileDialog(tile: SoundTileEntity, allowDestructive: Boolean, onDismiss: () -> Unit, onSave: (SoundTileEntity) -> Unit, onDelete: (SoundTileEntity) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(tile.name) }
    var num by remember { mutableStateOf(tile.number.toString()) }
    var audio by remember { mutableStateOf(tile.audioUri) }
    var img by remember { mutableStateOf(tile.imageUri) }
    var loop by remember { mutableStateOf(tile.isLooping) }
    val audioPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { UriUtils.takePersistablePermission(ctx, it); audio = it.toString() } }
    val imgPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { UriUtils.takePersistablePermission(ctx, it); img = it.toString() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ضبط #${tile.number}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = num, onValueChange = { num = it.filter { c -> c.isDigit() } }, label = { Text("الرقم") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Button(onClick = { audioPick.launch(arrayOf("audio/*")) }, enabled = audio.isNullOrBlank() || allowDestructive, modifier = Modifier.fillMaxWidth()) { Text(if (audio.isNullOrBlank()) "اختر صوتاً" else "تغيير الصوت") }
                OutlinedButton(onClick = { imgPick.launch(arrayOf("image/*")) }, enabled = img.isNullOrBlank() || allowDestructive, modifier = Modifier.fillMaxWidth()) { Text(if (img.isNullOrBlank()) "اختر صورة" else "تغيير الصورة") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("تكرار (Loop)"); Switch(checked = loop, onCheckedChange = { loop = it }) }
                if (allowDestructive) TextButton(onClick = { onDelete(tile) }, modifier = Modifier.fillMaxWidth()) { Text("حذف المربع", color = Color.Red) }
            }
        },
        confirmButton = { TextButton(onClick = { val n = num.toIntOrNull() ?: tile.number; onSave(tile.copy(name = name.ifBlank { "مؤثر $n" }, number = n, audioUri = audio, imageUri = img, isLooping = loop)) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun SettingsDialog(
    tiles: List<SoundTileEntity>, allowDestructive: Boolean, allowRestore: Boolean,
    onToggleDestructive: (Boolean) -> Unit, onToggleRestore: (Boolean) -> Unit,
    onEditTile: (SoundTileEntity) -> Unit, onAddTile: () -> Unit, onSetCount: (Int) -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit
) {
    var countTxt by remember { mutableStateOf(tiles.size.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإعدادات") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddTile, modifier = Modifier.fillMaxWidth()) { Text("إضافة مربع (+1)") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = countTxt, onValueChange = { countTxt = it.filter { c -> c.isDigit() } }, label = { Text("العدد") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = { countTxt.toIntOrNull()?.takeIf { it in 1..200 }?.let(onSetCount) }) { Text("تطبيق") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("السماح بالحذف/الاستبدال", fontSize = 13.sp); Switch(checked = allowDestructive, onCheckedChange = onToggleDestructive) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("السماح باسترجاع JSON", fontSize = 13.sp); Switch(checked = allowRestore, onCheckedChange = onToggleRestore) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBackup, modifier = Modifier.weight(1f)) { Text("نسخ احتياطي", fontSize = 12.sp) }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("استرجاع", fontSize = 12.sp) }
                }
                Divider()
                tiles.forEach { t -> TextButton(onClick = { onEditTile(t) }, modifier = Modifier.fillMaxWidth()) { Text("#${t.number} - ${t.name}", modifier = Modifier.fillMaxWidth()) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
