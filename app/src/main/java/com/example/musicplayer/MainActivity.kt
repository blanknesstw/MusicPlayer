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
                        BackupManager.restore(context, path)
                    }
                }

                val bgPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
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

                // 切歌時載入對應的 offset、最愛狀態、背景
                LaunchedEffect(currentIndex, songList) {
                    val song = songList.getOrNull(currentIndex)
                    lrcOffset = song?.path?.let { loadOffset(context, it) } ?: 0L
                    isFavorite = song?.path?.let { FavoritesRepository.isFavorite(context, it) } ?: false
                    currentBgUri = song?.path?.let { loadBgUri(context, it) }
                }

                // 監聽 Media3 自動切歌（例如通知列按下一首）同步更新 UI 的 currentIndex
                DisposableEffect(player) {
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            val newIndex = player.currentMediaItemIndex
                            currentIndex = newIndex
                            // 同步歌曲資訊
                            val song = songList.getOrNull(newIndex)
                            lrcOffset = song?.path?.let { loadOffset(context, it) } ?: 0L
                            isFavorite = song?.path?.let { FavoritesRepository.isFavorite(context, it) } ?: false
                            currentBgUri = song?.path?.let { loadBgUri(context, it) }
                        }
                    }
                    player.addListener(listener)
                    onDispose { player.removeListener(listener) }
                }

                when (currentScreen) {
                    "list" -> SongListScreen(
                        onSongClick = { song, songs ->
                            val clickedIndex = songs.indexOf(song)

                            // 直接比較 player 實際載入的歌單
                            val currentPlayerPaths = (0 until player.mediaItemCount)
                                .map { player.getMediaItemAt(it).localConfiguration?.uri.toString() }
                            val newPaths = songs.map { it.uri.toString() }

                            when {
                                currentPlayerPaths != newPaths -> {
                                    // player 的歌單跟新歌單不同，重設
                                    songList = songs
                                    val mediaItems = songs.map { MediaItem.fromUri(it.uri) }
                                    player.setMediaItems(mediaItems, clickedIndex, 0L)
                                    player.prepare()
                                    player.playWhenReady = true
                                }
                                player.currentMediaItemIndex != clickedIndex -> {
                                    // 歌單一樣但點了不同的歌，跳過去
                                    songList = songs
                                    player.seekToDefaultPosition(clickedIndex)
                                    player.playWhenReady = true
                                }
                                else -> {
                                    songList = songs
                                }
                                // 歌單一樣且同一首歌：什麼都不做
                            }

                            currentIndex = clickedIndex
                            currentScreen = "player"
                        },
                        currentSong = songList.getOrNull(currentIndex),
                    )
                    "player" -> PlayerScreen(
                        song = songList.getOrNull(currentIndex),
                        musicPlayer = player,
                        onBack = { currentScreen = "list" },
                        onNext = { player.seekToNextMediaItem() },
                        onPrevious = { player.seekToPreviousMediaItem() },
                        isShuffling = isShuffling,
                        onShuffleToggle = {
                            isShuffling = !isShuffling
                            player.shuffleModeEnabled = isShuffling
                        },
                        repeatMode = repeatMode,
                        onRepeatToggle = {
                            repeatMode = (repeatMode + 1) % 3
                            player.repeatMode = when (repeatMode) {
                                1 -> Player.REPEAT_MODE_ALL
                                2 -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        },
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