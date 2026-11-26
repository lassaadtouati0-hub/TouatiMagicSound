package com.touati.magicsound.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touati.magicsound.data.SoundTileDao
import com.touati.magicsound.data.SoundTileEntity
import com.touati.magicsound.data.TileState
import com.touati.magicsound.service.PlaybackController
import com.touati.magicsound.util.UriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dao: SoundTileDao,
    playbackController: PlaybackController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles by dao.getAllTilesFlow().collectAsState(initial = emptyList())
    val playbackState by playbackController.playbackState.collectAsState()

    val prefs = remember { context.getSharedPreferences("touati_magic_prefs", Context.MODE_PRIVATE) }
    var allowDestructive by remember { mutableStateOf(prefs.getBoolean("allow_destructive", false)) }
    var allowRestore by remember { mutableStateOf(prefs.getBoolean("allow_restore", false)) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingTile by remember { mutableStateOf<SoundTileEntity?>(null) }
    var showNewShowConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (dao.getCount() == 0) {
            val initialTiles = (1..20).map { i ->
                SoundTileEntity(
                    number = i,
                    name = "مؤثر $i",
                    audioUri = null,
                    imageUri = null,
                    state = TileState.UNUSED.value,
                    isLooping = false,
                    positionOrder = i
                )
            }
            dao.insertAll(initialTiles)
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            playbackController.clearError()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val currentTiles = dao.getAllTilesList()
                    val jsonArray = JSONArray()
                    currentTiles.forEach { t ->
                        val obj = JSONObject().apply {
                            put("number", t.number)
                            put("name", t.name)
                            put("audioUri", t.audioUri ?: "")
                            put("imageUri", t.imageUri ?: "")
                            put("isLooping", t.isLooping)
                            put("positionOrder", t.positionOrder)
                        }
                        jsonArray.put(obj)
                    }
                    val root = JSONObject().apply {
                        put("app", "TouatiMagicSound")
                        put("version", 1)
                        put("tiles", jsonArray)
                    }
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "فشل حفظ النسخة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!content.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            pendingRestoreJson = content
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "تعذر قراءة ملف النسخة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B1224),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101B34))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✨ Touati Magic Sound",
                    color = Color(0xFFF2C96D),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "لوحة تشغيل المؤثرات والموسيقى للعروض",
                    color = Color(0xFFAAB8D2),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = { showNewShowConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769CB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عرض جديد", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showSettingsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243653)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإعدادات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 175.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 12.dp,
                end = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tiles, key = { it.id }) { tile ->
                Column {
                    SoundTileCard(
                        tile = tile,
                        playbackState = playbackState,
                        onTileClick = { playbackController.playTile(tile) },
                        onPauseClick = { playbackController.pause() },
                        onResumeClick = { playbackController.resume() },
                        onRestartClick = { playbackController.restart(tile) },
                        onSeek = { pos -> playbackController.seekTo(pos) },
                        onToggleLoop = {
                            val newLoop = !tile.isLooping
                            scope.launch(Dispatchers.IO) {
                                dao.update(tile.copy(isLooping = newLoop))
                            }
                            playbackController.updateRepeatMode(tile.id, newLoop)
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
