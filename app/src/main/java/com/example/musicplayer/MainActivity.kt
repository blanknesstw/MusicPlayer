package com.example.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.ui.PlayerScreen
import com.example.musicplayer.ui.SettingsScreen
import com.example.musicplayer.ui.SongListScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import android.content.ComponentName
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                val context = LocalContext.current
                var musicPlayer by remember { mutableStateOf<androidx.media3.common.Player?>(null) }

                // 連接 MediaSession
                LaunchedEffect(Unit) {
                    val sessionToken = SessionToken(
                        context,
                        ComponentName(context, MusicService::class.java)
                    )
                    controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                    controllerFuture?.addListener({
                        mediaController = controllerFuture?.get()
                        musicPlayer = mediaController
                    }, MoreExecutors.directExecutor())
                }

                val player = musicPlayer
                if (player == null) return@MusicPlayerTheme

                var currentScreen by remember { mutableStateOf("list") }
                var songList by remember { mutableStateOf<List<Song>>(emptyList()) }
                var currentIndex by remember { mutableStateOf(0) }
                var isShuffling by remember { mutableStateOf(false) }
                var repeatMode by remember { mutableStateOf(0) }
                var lrcOffset by remember { mutableStateOf(0L) }
                var isFavorite by remember { mutableStateOf(false) }
                var currentBgUri by remember { mutableStateOf<String?>(null) }
                val restoreLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        val path = it.path ?: return@let
                        val success = BackupManager.restore(context, path)
                        // 之後可以加通知
                    }
                }

                val bgPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        // 讓 app 永久存取這個檔案
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        val uriString = it.toString()
                        songList.getOrNull(currentIndex)?.path?.let { path ->
                            saveBgUri(context, path, uriString)
                            currentBgUri = uriString
                        }
                    }
                }

                // 切歌時更新 player
                LaunchedEffect(currentIndex, songList, currentScreen) {
                    if (currentScreen == "player") {
                        val song = songList.getOrNull(currentIndex)
                        song?.let {
                            if (player.currentMediaItem?.localConfiguration?.uri != it.uri) {
                                player.setMediaItem(MediaItem.fromUri(it.uri))
                                player.prepare()
                                player.playWhenReady = true
                            } else {
                                player.playWhenReady = true
                            }
                        }
                    }
                }

                // 切歌時載入 offset
                LaunchedEffect(currentIndex, songList) {
                    val song = songList.getOrNull(currentIndex)
                    lrcOffset = song?.path?.let { loadOffset(context, it) } ?: 0L
                    isFavorite = song?.path?.let { FavoritesRepository.isFavorite(context,
                        it) } ?: false
                    currentBgUri = song?.path?.let { loadBgUri(context, it) }
                    currentBgUri = song?.path?.let { loadBgUri(context, it) }
                }

                // 監聽播放結束
                DisposableEffect(player) {
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                when {
                                    repeatMode == 2 -> {
                                        player.seekTo(0)
                                        player.play()
                                    }
                                    isShuffling -> {
                                        currentIndex = (songList.indices - currentIndex).random()
                                    }
                                    repeatMode == 1 -> {
                                        currentIndex = if (currentIndex < songList.size - 1)
                                            currentIndex + 1 else 0
                                    }
                                    else -> {
                                        if (currentIndex < songList.size - 1) {
                                            currentIndex++
                                        } else {
                                            currentIndex = 0
                                            // 強制重新載入第一首
                                            val song = songList.getOrNull(0)
                                            song?.let {
                                                player.setMediaItem(MediaItem.fromUri(it.uri))
                                                player.prepare()
                                                player.playWhenReady = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    player.addListener(listener)
                    onDispose { player.removeListener(listener) }
                }

                when (currentScreen) {
                    "list" -> SongListScreen(
                        onSongClick = { song, songs ->
                            songList = songs
                            currentIndex = songs.indexOf(song)
                            currentScreen = "player"
                        },
                        currentSong = songList.getOrNull(currentIndex),
                    )
                    "player" -> PlayerScreen(
                        song = songList.getOrNull(currentIndex),
                        musicPlayer = player,
                        onBack = { currentScreen = "list" },
                        onNext = {
                            if (isShuffling) {
                                currentIndex = (songList.indices - currentIndex).random()
                            } else {
                                if (currentIndex < songList.size - 1) currentIndex++
                            }
                        },
                        onPrevious = {
                            if (currentIndex > 0) currentIndex--
                        },
                        isShuffling = isShuffling,
                        onShuffleToggle = { isShuffling = !isShuffling },
                        repeatMode = repeatMode,
                        onRepeatToggle = { repeatMode = (repeatMode + 1) % 3 },
                        lrcOffset = lrcOffset,
                        onSettingsClick = { currentScreen = "settings" },
                        isFavorite = isFavorite,
                        onFavoriteToggle = {
                            songList.getOrNull(currentIndex)?.path?.let {
                                if (FavoritesRepository.isFavorite(context, it)) {
                                    FavoritesRepository.removeFavorite(context, it)
                                    isFavorite = false
                                } else {
                                    FavoritesRepository.addFavorite(context, it)
                                    isFavorite = true
                                }
                            }
                        },
                        bgUri = currentBgUri,
                    )
                    "settings" -> SettingsScreen(
                        song = songList.getOrNull(currentIndex),
                        musicPlayer = player,
                        lrcOffset = lrcOffset,
                        onOffsetChange = { lrcOffset = it },
                        onSave = {
                            songList.getOrNull(currentIndex)?.path?.let {
                                saveOffset(context, it, lrcOffset)
                            }
                        },
                        onBack = { currentScreen = "player" },
                        onPickBackground = { bgPickerLauncher.launch("video/*") },
                        currentBgUri = currentBgUri,
                        onBackup = {
                            BackupManager.backup(context)
                        },
                        onRestore = {
                            restoreLauncher.launch("application/json")
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

fun saveOffset(context: Context, songPath: String, offset: Long) {
    context.getSharedPreferences("lrc_offsets", Context.MODE_PRIVATE)
        .edit()
        .putLong(songPath, offset)
        .apply()
}

fun saveBgUri(context: Context, songPath: String, uri: String) {
    context.getSharedPreferences("bg_uris", Context.MODE_PRIVATE)
        .edit()
        .putString(songPath, uri)
        .apply()
}

fun loadBgUri(context: Context, songPath: String): String? {
    return context.getSharedPreferences("bg_uris", Context.MODE_PRIVATE)
        .getString(songPath, null)
}

fun loadOffset(context: Context, songPath: String): Long {
    return context.getSharedPreferences("lrc_offsets", Context.MODE_PRIVATE)
        .getLong(songPath, 0L)
}