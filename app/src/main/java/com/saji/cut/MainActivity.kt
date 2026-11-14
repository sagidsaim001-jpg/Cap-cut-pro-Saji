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
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
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
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Brightness
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.TimestampWrapper
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Composition
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
    var additionalVideoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf<String?>(null) }
    var captionCues by remember { mutableStateOf<List<CaptionCue>>(emptyList()) }
    var captionFileName by remember { mutableStateOf<String?>(null) }
    var stickerUri by remember { mutableStateOf<Uri?>(null) }
    var stickerName by remember { mutableStateOf<String?>(null) }

    fun keepReadPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some document providers do not support persistable permissions.
        }
    }

    val newProjectPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { picked ->
        if (picked.isNotEmpty()) {
            picked.forEach(::keepReadPermission)
            videoUri = picked.first()
            videoName = queryDisplayName(context, picked.first()) ?: "New project"
            additionalVideoUris = picked.drop(1)
            audioUri = null
            audioName = null
        }
    }

    val addClipsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { picked ->
        if (picked.isNotEmpty()) {
            picked.forEach(::keepReadPermission)
            additionalVideoUris = (additionalVideoUris + picked).distinct()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            keepReadPermission(picked)
            audioUri = picked
            audioName = queryDisplayName(context, picked) ?: "Background audio"
        }
    }

    val captionPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            keepReadPermission(picked)
            val subtitleText = try {
                context.contentResolver.openInputStream(picked)?.bufferedReader()?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                ""
            }
            captionCues = parseSrt(subtitleText)
            captionFileName = queryDisplayName(context, picked) ?: "Subtitles.srt"
        }
    }

    val stickerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            keepReadPermission(picked)
            stickerUri = picked
            stickerName = queryDisplayName(context, picked) ?: "Image sticker"
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
                HomeScreen(onImport = { newProjectPicker.launch(arrayOf("video/*")) })
            } else {
                EditorScreen(
                    uri = videoUri!!,
                    title = videoName,
                    additionalVideoUris = additionalVideoUris,
                    audioUri = audioUri,
                    audioName = audioName,
                    captionCues = captionCues,
                    captionFileName = captionFileName,
                    stickerUri = stickerUri,
                    stickerName = stickerName,
                    onBack = {
                        videoUri = null
                        additionalVideoUris = emptyList()
                        audioUri = null
                        audioName = null
                        captionCues = emptyList()
                        captionFileName = null
                        stickerUri = null
                        stickerName = null
                    },
                    onAddVideo = { addClipsPicker.launch(arrayOf("video/*")) },
                    onAddAudio = { audioPicker.launch(arrayOf("audio/*")) },
                    onAddCaptions = { captionPicker.launch(arrayOf("*/*")) },
                    onAddSticker = { stickerPicker.launch(arrayOf("image/*")) }
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
    Filters("Filters"),
    Adjust("Adjust"),
    Speed("Speed"),
    Canvas("Canvas")
}

@OptIn(UnstableApi::class)
@Composable
private fun EditorScreen(
    uri: Uri,
    title: String,
    additionalVideoUris: List<Uri>,
    audioUri: Uri?,
    audioName: String?,
    captionCues: List<CaptionCue>,
    captionFileName: String?,
    stickerUri: Uri?,
    stickerName: String?,
    onBack: () -> Unit,
    onAddVideo: () -> Unit,
    onAddAudio: () -> Unit,
    onAddCaptions: () -> Unit,
    onAddSticker: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setVideoEffects(emptyList())
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
    var clipVolume by remember { mutableFloatStateOf(1f) }
    var musicVolume by remember { mutableFloatStateOf(0.75f) }
    var musicFadeInMs by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var overlayText by remember { mutableStateOf("") }
    var overlayTextSize by remember { mutableFloatStateOf(22f) }
    var overlayTextColor by remember { mutableStateOf("White") }
    var filterPreset by remember { mutableStateOf("Original") }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var aspectPreset by remember { mutableStateOf("Original") }
    var exportResolution by remember { mutableStateOf("1080p") }
    var stickerPosition by remember { mutableStateOf("Bottom right") }
    var stickerScale by remember { mutableFloatStateOf(0.28f) }
    var stickerOpacity by remember { mutableFloatStateOf(1f) }
    var splitMarkers = remember(uri) { mutableStateListOf<Long>() }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    val previewEffects = remember(
        filterPreset, brightness, contrast, saturation, rotation, zoom, aspectPreset, exportResolution,
        stickerUri, stickerPosition, stickerScale, stickerOpacity
    ) {
        buildVisualEffects(context, filterPreset, brightness, contrast, saturation, rotation, zoom, aspectPreset, exportResolution,
            stickerUri, stickerPosition, stickerScale, stickerOpacity)
    }

    SideEffect {
        player.volume = if (muted) 0f else clipVolume
        player.setPlaybackSpeed(speed)
        player.setVideoEffects(previewEffects)
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
    val activeCaption = captionCues.firstOrNull {
        playheadMs >= it.startMs && playheadMs < it.endMs
    }?.text.orEmpty()
    val previewOverlayText = if (overlayText.isNotBlank()) overlayText else activeCaption

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
                            val speedProvider = object : SpeedProvider {
                                override fun getSpeed(presentationTimeUs: Long): Float = speed
                                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
                            }

                            val videoEffects = previewEffects.toMutableList()
                            if (overlayText.isNotBlank()) {
                                val styledText = SpannableString(overlayText)
                                styledText.setSpan(
                                    ForegroundColorSpan(textColorInt(overlayTextColor)),
                                    0, styledText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                styledText.setSpan(
                                    RelativeSizeSpan(overlayTextSize / 22f),
                                    0, styledText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                styledText.setSpan(
                                    StyleSpan(Typeface.BOLD),
                                    0, styledText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                val textLayer = TextOverlay.createStaticTextOverlay(styledText)
                                videoEffects.add(OverlayEffect(listOf(textLayer)))
                            }

                            val cutPoints = (listOf(trimStartMs) +
                                splitMarkers.filter { it > trimStartMs && it < trimEndMs }.sorted() +
                                listOf(trimEndMs)).distinct()
                            val editedClips = mutableListOf<EditedMediaItem>()
                            for (index in 0 until (cutPoints.size - 1)) {
                                val segmentStart = cutPoints[index]
                                val segmentEnd = cutPoints[index + 1]
                                if (segmentEnd <= segmentStart) continue
                                val segment = MediaItem.Builder()
                                    .setUri(uri)
                                    .setClippingConfiguration(
                                        MediaItem.ClippingConfiguration.Builder()
                                            .setStartPositionMs(segmentStart)
                                            .setEndPositionMs(segmentEnd)
                                            .build()
                                    )
                                    .build()
                                val clipAudioProcessors = if (!muted && clipVolume < 0.999f) {
                                    listOf(GainProcessor(DefaultGainProvider.Builder(clipVolume).build()))
                                } else emptyList()
                                editedClips.add(
                                    EditedMediaItem.Builder(segment)
                                        .setRemoveAudio(muted)
                                        .setSpeed(speedProvider)
                                        .setEffects(Effects(clipAudioProcessors, videoEffects))
                                        .build()
                                )
                            }

                            additionalVideoUris.forEach { extraUri ->
                                val extraAudioProcessors = if (!muted && clipVolume < 0.999f) {
                                    listOf(GainProcessor(DefaultGainProvider.Builder(clipVolume).build()))
                                } else emptyList()
                                editedClips.add(
                                    EditedMediaItem.Builder(MediaItem.fromUri(extraUri))
                                        .setRemoveAudio(muted)
                                        .setSpeed(speedProvider)
                                        .setEffects(Effects(extraAudioProcessors, videoEffects))
                                        .build()
                                )
                            }

                            val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedClips)
                            val subtitleEffects = captionCues.mapNotNull { cue ->
                                if (cue.text.isBlank() || cue.endMs <= cue.startMs) {
                                    null
                                } else {
                                    val subtitleText = SpannableString(cue.text)
                                    subtitleText.setSpan(
                                        ForegroundColorSpan(textColorInt(overlayTextColor)),
                                        0, subtitleText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    subtitleText.setSpan(
                                        RelativeSizeSpan(overlayTextSize / 22f),
                                        0, subtitleText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    subtitleText.setSpan(
                                        StyleSpan(Typeface.BOLD),
                                        0, subtitleText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    val captionStartMs = ((cue.startMs - trimStartMs).coerceAtLeast(0L) / speed).toLong()
                                    val captionEndMs = ((cue.endMs - trimStartMs).coerceAtLeast(0L) / speed).toLong()
                                    if (captionEndMs <= captionStartMs) {
                                        null
                                    } else {
                                        TimestampWrapper(
                                            OverlayEffect(listOf(TextOverlay.createStaticTextOverlay(subtitleText))),
                                            captionStartMs * 1_000L,
                                            captionEndMs * 1_000L
                                        )
                                    }
                                }
                            }
                            val compositionEffects = Effects(emptyList(), subtitleEffects)
                            val composition = if (audioUri != null) {
                                val gainBuilder = DefaultGainProvider.Builder(musicVolume)
                                if (musicFadeInMs > 0f) {
                                    gainBuilder.addFadeAt(
                                        0L,
                                        (musicFadeInMs * 1000f).toLong().coerceAtLeast(1L),
                                        DefaultGainProvider.FADE_IN_LINEAR
                                    )
                                }
                                val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(audioUri))
                                    .setEffects(Effects(listOf(GainProcessor(gainBuilder.build())), emptyList()))
                                    .build()
                                val musicSequence = EditedMediaItemSequence
                                    .withAudioFrom(listOf(musicItem))
                                    .buildUpon()
                                    .setIsLooping(true)
                                    .build()
                                Composition.Builder(listOf(videoSequence, musicSequence))
                                    .setEffects(compositionEffects)
                                    .build()
                            } else {
                                Composition.Builder(listOf(videoSequence))
                                    .setEffects(compositionEffects)
                                    .build()
                            }

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
                                transformer.start(composition, outputFile.absolutePath)
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
            if (previewOverlayText.isNotBlank()) {
                Text(
                    previewOverlayText,
                    color = previewTextColor(overlayTextColor),
                    fontSize = overlayTextSize.sp,
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

            if (additionalVideoUris.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("VIDEO TRACKS", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        additionalVideoUris.forEachIndexed { index, clipUri ->
                            val clipLabel = queryDisplayName(context, clipUri) ?: ("Clip " + (index + 2))
                            Surface(
                                color = PanelLight,
                                shape = RoundedCornerShape(7.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30404B))
                            ) {
                                Text(
                                    (index + 2).toString() + " • " + clipLabel,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (audioName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AUDIO TRACK  •  " + audioName, color = Accent, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
                ToolButton("Filters", Icons.Default.Tune, selectedTool == EditorTool.Filters) { selectedTool = EditorTool.Filters }
                ToolButton("Adjust", Icons.Default.Crop, selectedTool == EditorTool.Adjust) { selectedTool = EditorTool.Adjust }
                ToolButton("Speed", Icons.Default.Speed, selectedTool == EditorTool.Speed) { selectedTool = EditorTool.Speed }
                ToolButton("Canvas", Icons.Default.Crop, selectedTool == EditorTool.Canvas) { selectedTool = EditorTool.Canvas }
            }

            when (selectedTool) {
                EditorTool.Edit -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = onAddVideo,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Accent.copy(alpha = 0.18f), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Add clips", fontSize = 12.sp)
                        }
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
                        Text("Mute original clip audio", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Switch(checked = muted, onCheckedChange = { muted = it })
                    }
                    Text("Original audio volume  " + (clipVolume * 100).toInt() + "%", color = Muted, fontSize = 10.sp)
                    Slider(value = clipVolume, onValueChange = { clipVolume = it }, valueRange = 0f..1f)
                    FilledTonalButton(
                        onClick = onAddAudio,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PanelLight, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (audioUri == null) "Add background music" else "Replace background music", fontSize = 12.sp)
                    }
                    if (audioName != null) {
                        Text("Music track: " + audioName, color = Accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Music volume  " + (musicVolume * 100).toInt() + "%", color = Muted, fontSize = 10.sp)
                        Slider(value = musicVolume, onValueChange = { musicVolume = it }, valueRange = 0f..1f)
                        Text("Music fade-in  " + musicFadeInMs.toInt() + " ms", color = Muted, fontSize = 10.sp)
                        Slider(value = musicFadeInMs, onValueChange = { musicFadeInMs = it }, valueRange = 0f..5000f)
                    }
                    Text("Background music is mixed with the video and loops to cover the final video duration. Volume and fade-in apply during export.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Text -> {
                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        label = { Text("Text / caption overlay") },
                        placeholder = { Text("Type a title or caption") }
                    )
                    Text("TEXT COLOR", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("White", "Yellow", "Cyan", "Pink", "Green").forEach { choice ->
                            EditorChoice(choice, overlayTextColor == choice) { overlayTextColor = choice }
                        }
                    }
                    Text("SIZE  " + overlayTextSize.toInt() + " sp", color = Muted, fontSize = 10.sp)
                    Slider(
                        value = overlayTextSize,
                        onValueChange = { overlayTextSize = it },
                        valueRange = 12f..52f,
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
                    )
                    FilledTonalButton(
                        onClick = onAddCaptions,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PanelLight, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Import SRT captions", fontSize = 12.sp)
                    }
                    if (captionFileName != null) {
                        Text(captionFileName + "  •  " + captionCues.size + " captions", color = Accent, fontSize = 10.sp)
                    }
                    Text("SRT subtitles follow their timecodes in preview and are burned into export.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Filters -> {
                    Text("FILTER PRESETS", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Original", "Warm", "Cool", "Vivid", "Mono", "Negative").forEach { choice ->
                            EditorChoice(choice, filterPreset == choice) { filterPreset = choice }
                        }
                    }
                    Text("Selected filter is applied to the live preview and final export.", color = Muted, fontSize = 10.sp)
                }
                EditorTool.Adjust -> {
                    Text("COLOR ADJUSTMENT", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Text("Brightness  " + (brightness * 100).toInt(), color = Muted, fontSize = 10.sp)
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -1f..1f)
                    Text("Contrast  " + (contrast * 100).toInt(), color = Muted, fontSize = 10.sp)
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -1f..1f)
                    Text("Saturation  " + saturation.toInt(), color = Muted, fontSize = 10.sp)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = -100f..100f)
                    TextButton(onClick = {
                        brightness = 0f
                        contrast = 0f
                        saturation = 0f
                        filterPreset = "Original"
                    }) { Text("Reset adjustments", color = Accent) }
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
                    Text("ASPECT RATIO", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Original", "9:16", "16:9", "1:1", "4:5", "4:3").forEach { choice ->
                            EditorChoice(choice, aspectPreset == choice) { aspectPreset = choice }
                        }
                    }
                    Text("EXPORT RESOLUTION", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("480p", "720p", "1080p", "Original").forEach { choice ->
                            EditorChoice(choice, exportResolution == choice) { exportResolution = choice }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rotation", color = Muted, fontSize = 12.sp)
                        Text(rotation.roundToInt().toString() + "°", color = Accent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { rotation = (rotation + 90f) % 360f }) { Text("Rotate 90°", color = Accent) }
                        TextButton(onClick = { rotation = 0f; zoom = 1f }) { Text("Reset", color = Color.White) }
                    }
                    Text("Zoom  " + String.format(Locale.US, "%.2fx", zoom), color = Muted, fontSize = 10.sp)
                    Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..2.5f)
                    Text("Aspect ratio, zoom, resolution and rotation are applied during preview and export.", color = Muted, fontSize = 10.sp)
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

@Composable
private fun EditorChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Accent.copy(alpha = 0.18f) else PanelLight,
        shape = RoundedCornerShape(9.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Accent) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = if (selected) Color.White else Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

private fun textColorInt(name: String): Int = when (name) {
    "Yellow" -> android.graphics.Color.YELLOW
    "Cyan" -> android.graphics.Color.CYAN
    "Pink" -> android.graphics.Color.rgb(255, 105, 180)
    "Green" -> android.graphics.Color.GREEN
    else -> android.graphics.Color.WHITE
}

private fun previewTextColor(name: String): Color = when (name) {
    "Yellow" -> Color(0xFFFFEB3B)
    "Cyan" -> Color(0xFF4DEBFF)
    "Pink" -> Color(0xFFFF69B4)
    "Green" -> Color(0xFF62F28B)
    else -> Color.White
}

@OptIn(UnstableApi::class)
private fun buildVisualEffects(
    filterPreset: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Float,
    zoom: Float,
    aspectPreset: String,
    exportResolution: String
): MutableList<androidx.media3.common.Effect> {
    val effects = mutableListOf<androidx.media3.common.Effect>()

    when (filterPreset) {
        "Mono" -> effects.add(RgbFilter.createGrayscaleFilter())
        "Negative" -> effects.add(RgbFilter.createInvertedFilter())
        "Warm" -> effects.add(
            RgbAdjustment.Builder()
                .setRedScale(1.12f)
                .setGreenScale(1.02f)
                .setBlueScale(0.88f)
                .build()
        )
        "Cool" -> effects.add(
            RgbAdjustment.Builder()
                .setRedScale(0.90f)
                .setGreenScale(1.02f)
                .setBlueScale(1.12f)
                .build()
        )
    }

    if (brightness != 0f) effects.add(Brightness(brightness))
    if (contrast != 0f) effects.add(Contrast(contrast))

    val presetSaturation = if (filterPreset == "Vivid") 24f else 0f
    val totalSaturation = (saturation + presetSaturation).coerceIn(-100f, 100f)
    if (totalSaturation != 0f) {
        effects.add(HslAdjustment.Builder().adjustSaturation(totalSaturation).build())
    }

    if (rotation != 0f || zoom != 1f) {
        effects.add(
            ScaleAndRotateTransformation.Builder()
                .setScale(zoom, zoom)
                .setRotationDegrees(rotation)
                .build()
        )
    }

    val aspectRatio = when (aspectPreset) {
        "9:16" -> 9f / 16f
        "16:9" -> 16f / 9f
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "4:3" -> 4f / 3f
        else -> 0f
    }
    if (aspectRatio > 0f) {
        effects.add(
            Presentation.createForAspectRatio(
                aspectRatio,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        )
    }

    val outputHeight = when (exportResolution) {
        "480p" -> 480
        "720p" -> 720
        "1080p" -> 1080
        else -> 0
    }
    if (outputHeight > 0) effects.add(Presentation.createForHeight(outputHeight))
    return effects
}

private data class CaptionCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

private fun parseSrtTimestamp(value: String): Long? {
    val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})").find(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: return null
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    val seconds = match.groupValues[3].toLongOrNull() ?: return null
    val millis = match.groupValues[4].toLongOrNull() ?: return null
    return (((hours * 60L + minutes) * 60L + seconds) * 1_000L) + millis
}

private fun parseSrt(source: String): List<CaptionCue> {
    if (source.isBlank()) return emptyList()
    return source.replace("\r", "")
        .trim()
        .split(Regex("\\n\\s*\\n"))
        .mapNotNull { block ->
            val lines = block.lines()
            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex < 0) return@mapNotNull null
            val timeParts = lines[timeLineIndex].split("-->")
            if (timeParts.size != 2) return@mapNotNull null
            val start = parseSrtTimestamp(timeParts[0]) ?: return@mapNotNull null
            val end = parseSrtTimestamp(timeParts[1]) ?: return@mapNotNull null
            val caption = lines.drop(timeLineIndex + 1).joinToString("\n").trim()
            if (caption.isBlank() || end <= start) null else CaptionCue(start, end, caption)
        }
        .sortedBy { it.startMs }
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
