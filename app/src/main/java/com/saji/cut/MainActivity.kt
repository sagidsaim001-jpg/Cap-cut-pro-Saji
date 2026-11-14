package com.saji.cut

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableString
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val Background = Color(0xFF080A0E)
private val Panel = Color(0xFF141922)
private val PanelLight = Color(0xFF1B222D)
private val Accent = Color(0xFF32E0B8)
private val Muted = Color(0xFF8B95A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 10, 14)
        window.navigationBarColor = android.graphics.Color.rgb(8, 10, 14)
        setContent { SajiCutApp() }
    }
}

@Composable
private fun SajiCutApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("Untitled project") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some document providers do not offer persistable grants.
            }
            videoUri = picked
            videoName = queryDisplayName(context, picked) ?: "New video"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF06110E),
            secondary = Color(0xFF6AA8FF),
            background = Background,
            surface = Panel,
            onSurface = Color(0xFFF5F7FA)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            if (videoUri == null) {
                HomeScreen(onImport = { videoPicker.launch(arrayOf("video/*")) })
            } else {
                EditorScreen(
                    uri = videoUri!!,
                    title = videoName,
                    onBack = { videoUri = null },
                    onAddVideo = { videoPicker.launch(arrayOf("video/*")) }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("SC", color = Color(0xFF07120F), fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("SAJI CUT", fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 1.4.sp)
                Text("MOBILE VIDEO STUDIO", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("1.0", color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text("Make every frame\nmean something.", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
        Text(
            "A timeline editor for your videos, clips and creative ideas.",
            color = Muted,
            fontSize = 14.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clickable { onImport() },
            color = Panel,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26323D))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Background, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("New project", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Import a video from your device", color = Muted, fontSize = 12.sp)
            }
        }

        Text("WORKSPACE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Recent projects", fontWeight = FontWeight.SemiBold)
                    Text("Your imported video opens directly in the editor.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Text(
            "SAJI CUT is an independent editor project. It uses a familiar mobile timeline workflow.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

private enum class EditorTool(val title: String) {
    Edit("Edit"),
    Audio("Audio"),
    Text("Text"),
    Speed("Speed"),
    Canvas("Canvas")
}

@OptIn(UnstableApi::class)
@Composable
private fun EditorScreen(
    uri: Uri,
    title: String,
    onBack: () -> Unit,
    onAddVideo: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var playheadMs by remember(uri) { mutableLongStateOf(0L) }
    var trimStartMs by remember(uri) { mutableLongStateOf(0L) }
    var trimEndMs by remember(uri) { mutableLongStateOf(0L) }
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(EditorTool.Edit) }
    var speed by remember { mutableFloatStateOf(1f) }
    var muted by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var overlayText by remember { mutableStateOf("") }
    var splitMarkers = remember(uri) { mutableStateListOf<Long>() }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    SideEffect {
        player.volume = if (muted) 0f else 1f
        player.setPlaybackSpeed(speed)
    }

    LaunchedEffect(player) {
        while (isActive) {
            val actualDuration = player.duration
            if (actualDuration != C.TIME_UNSET && actualDuration > 0 && actualDuration != durationMs) {
                durationMs = actualDuration
                trimEndMs = actualDuration
            }
            playheadMs = player.currentPosition.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(140)
        }
    }

    val thumbnails by produceState(initialValue = emptyList<Bitmap>(), uri) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val sourceDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 1_000L
                (0..5).mapNotNull { index ->
                    val timeUs = (sourceDuration * index / 5L) * 1_000L
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame == null) null else {
                        val scaled = Bitmap.createScaledBitmap(frame, 150, 88, true)
                        if (scaled !== frame) frame.recycle()
                        scaled
                    }
                }
            } catch (_: Exception) {
                emptyList()
            } finally {
                try { retriever.release() } catch (_: Exception) { }
            }
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1_000L)
    val currentTime = formatTime(playheadMs)
    val totalTime = formatTime(durationMs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("SAJI CUT", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)
                Text(title, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = {
                    if (!exporting) {
                        if (durationMs <= 0L || trimEndMs <= trimStartMs) {
                            exportMessage = "Video is still loading. Try again in a moment."
                        } else {
                            exporting = true
                            exportProgress = 0
                            val clip = MediaItem.Builder()
                                .setUri(uri)
                                .setClippingConfiguration(
                                    MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(trimStartMs)
                                        .setEndPositionMs(trimEndMs)
                                        .build()
                                )
                                .build()

                            val speedProvider = object : SpeedProvider {
                                override fun getSpeed(presentationTimeUs: Long): Float = speed
                                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
                            }

                            val videoEffects = mutableListOf<androidx.media3.common.Effect>()
                            if (rotation != 0f) {
                                videoEffects.add(
                                    ScaleAndRotateTransformation.Builder()
                                        .setRotationDegrees(rotation)
                                        .build()
                                )
                            }
                            if (overlayText.isNotBlank()) {
                                val textLayer = TextOverlay.createStaticTextOverlay(SpannableString(overlayText))
                                videoEffects.add(OverlayEffect(listOf(textLayer)))
                            }

                            val editBuilder = EditedMediaItem.Builder(clip)
                                .setRemoveAudio(muted)
                                .setSpeed(speedProvider)
                            if (videoEffects.isNotEmpty()) {
                                editBuilder.setEffects(Effects(emptyList(), videoEffects))
                            }
                            val editedItem = editBuilder.build()
                            val outputFile = File(context.cacheDir, "saji_cut_" + System.currentTimeMillis() + ".mp4")
                            val transformer = Transformer.Builder(context)
                                .addListener(object : Transformer.Listener {
                                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                                        scope.launch {
                                            val destination = withContext(Dispatchers.IO) {
                                                saveExportToMovies(context, outputFile)
                                            }
                                            exporting = false
                                            exportProgress = 100
                                            exportMessage = if (destination != null) {
                                                "Export complete. Saved to " + destination
                                            } else {
                                                "Export completed, but the video could not be added to Movies."
                                            }
                                        }
                                    }

                                    override fun onError(
                                        composition: androidx.media3.transformer.Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        exporting = false
                                        exportMessage = "Export failed: " + (exportException.message ?: "Unsupported video format")
                                    }
                                })
                                .build()

                            try {
                                transformer.start(editedItem, outputFile.absolutePath)
                                scope.launch {
                                    val holder = ProgressHolder()
                                    while (exporting) {
                                        val state = transformer.getProgress(holder)
                                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                            exportProgress = holder.progress
                                        }
                                        delay(450)
                                    }
                                }
                            } catch (error: Exception) {
                                exporting = false
                                exportMessage = error.message ?: "Could not start export."
                            }
                        }
                    }
                },
                enabled = !exporting,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (exporting) "Exporting" else "Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        this.player = player
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { it.player = player }
            )
            if (overlayText.isNotBlank()) {
                Text(
                    overlayText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                    isPlaying = player.isPlaying
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play or pause",
                    tint = Accent,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(totalTime, color = Muted, fontSize = 11.sp)
        }

        Slider(
            value = playheadMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
            onValueChange = {
                playheadMs = it.toLong()
                player.seekTo(playheadMs)
            },
            valueRange = 0f..safeDuration.toFloat(),
            modifier = Modifier.padding(horizontal = 15.dp).height(25.dp),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = PanelLight
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(currentTime + " / " + totalTime, color = Color.White, fontSize = 10.sp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0E13))
                    .border(1.dp, Color(0xFF303B49), RoundedCornerShape(10.dp))
            ) {
                if (thumbnails.isNotEmpty()) {
                    Row(Modifier.fillMaxSize()) {
                        thumbnails.forEach { frame ->
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = "Video frame",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Text("VIDEO CLIP", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                }

                splitMarkers.forEach { marker ->
                    val fraction = (marker.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .offset(x = (fraction * 320f).dp)
                            .background(Color(0xFFFFC857))
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = (playheadMs.toFloat() / safeDuration.toFloat() * 320f).dp)
                        .background(Color.White)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("IN", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = trimStartMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                    onValueChange = {
                        trimStartMs = it.toLong().coerceAtMost(trimEndMs - 500L).coerceAtLeast(0L)
                        if (playheadMs < trimStartMs) {
                            playheadMs = trimStartMs
                            player.seekTo(trimStartMs)
                        }
                    },
                    valueRange = 0f..safeDuration.toFloat(),
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
                )
                Text("OUT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = trimEndMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                    onValueChange = {
                        trimEndMs = it.toLong().coerceAtLeast(trimStartMs + 500L).coerceAtMost(safeDuration)
                    },
                    valueRange = 0f..safeDuration.toFloat(),
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Edit", Icons.Default.ContentCut, selectedTool == EditorTool.Edit) { selectedTool = EditorTool.Edit }
                ToolButton("Audio", Icons.Default.MusicNote, selectedTool == EditorTool.Audio) { selectedTool = EditorTool.Audio }
                ToolButton("Text", Icons.Default.TextFields, selectedTool == EditorTool.Text) { selectedTool = EditorTool.Text }
                ToolButton("Speed", Icons.Default.Speed, selectedTool == EditorTool.Speed) { selectedTool = EditorTool.Speed }
                ToolButton("Canvas", Icons.Default.Crop, selectedTool == EditorTool.Canvas) { selectedTool = EditorTool.Canvas }
            }

            when (selectedTool) {
                EditorTool.Edit -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = {
                                if (playheadMs > trimStartMs && playheadMs < trimEndMs) {
                                    if (splitMarkers.none { kotlin.math.abs(it - playheadMs) < 350L }) {
                                        splitMarkers.add(playheadMs)
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PanelLight, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Split", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { if (splitMarkers.isNotEmpty()) splitMarkers.removeAt(splitMarkers.lastIndex) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PanelLight, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Undo split", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { rotation = (rotation + 90f) % 360f },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PanelLight, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Rotate", fontSize = 12.sp)
                        }
                    }
                    Text(
                        "Split adds an edit point to the timeline. Trim, rotate and split markers are kept while this editor session stays open.",
                        color = Muted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
                EditorTool.Audio -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Mute original audio", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Switch(checked = muted, onCheckedChange = { muted = it })
                    }
                    Text("Original sound is included in export unless mute is enabled.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Text -> {
                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Text overlay") },
                        placeholder = { Text("Type a title or caption") }
                    )
                    Text("Text is shown on the preview and burned into the exported video.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Speed -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1f, 1.5f, 2f, 3f).forEach { rate ->
                            val chosen = speed == rate
                            Surface(
                                color = if (chosen) Accent else PanelLight,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.clickable {
                                    speed = rate
                                    player.setPlaybackSpeed(rate)
                                }
                            ) {
                                Text(
                                    rate.toString() + "x",
                                    color = if (chosen) Background else Color.White,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Text("Speed changes are applied to the exported MP4.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Canvas -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rotation", color = Muted, fontSize = 12.sp)
                        Text(rotation.roundToInt().toString() + "°", color = Accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { rotation = 0f }) { Text("Reset", color = Color.White) }
                    }
                    Text("Current export keeps the source frame size and applies rotation.", color = Muted, fontSize = 10.sp)
                }
            }
        }

        if (exporting) {
            Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rendering MP4", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(exportProgress.toString() + "%", color = Accent, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { exportProgress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = Accent,
                    trackColor = PanelLight
                )
            }
        }
    }

    if (exportMessage != null) {
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("SAJI CUT") },
            text = { Text(exportMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { exportMessage = null }) { Text("OK", color = Accent) }
            },
            containerColor = Panel
        )
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Accent.copy(alpha = 0.16f) else PanelLight,
        shape = RoundedCornerShape(11.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.6f)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Accent else Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (selected) Color.White else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val safe = timeMs.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe / 1_000L) % 60L
    val millis = (safe % 1_000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
private fun saveExportToMovies(context: Context, source: File): String? {
    if (!source.exists()) return null
    val fileName = "SAJI_CUT_" + System.currentTimeMillis() + ".mp4"

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SAJI CUT")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val destination = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            try {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not open Movies output stream")
                val ready = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                context.contentResolver.update(destination, ready, null, null)
                "Movies/SAJI CUT/" + fileName
            } catch (error: Exception) {
                context.contentResolver.delete(destination, null, null)
                throw error
            }
        } else {
            val folder = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SAJI CUT")
            if (!folder.exists()) folder.mkdirs()
            val destination = File(folder, fileName)
            source.copyTo(destination, overwrite = true)
            destination.absolutePath
        }
    } catch (_: Exception) {
        null
    }
}
