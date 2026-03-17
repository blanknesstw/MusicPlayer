package com.example.musicplayer.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.musicplayer.Song
import kotlinx.coroutines.delay
import com.example.musicplayer.LrcParser
import com.example.musicplayer.MusicRepository
import com.example.musicplayer.LrcLine
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.MusicNote
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun PlayerScreen(
    song: Song?,
    musicPlayer: Player,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    isShuffling: Boolean,
    onShuffleToggle: () -> Unit,
    repeatMode: Int,
    onRepeatToggle: () -> Unit,
    lrcOffset: Long,
    onSettingsClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    bgUri: String?,
) {    val context = LocalContext.current

    // 背景影片 player
    val bgPlayer = remember(bgUri) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (bgUri != null) {
                android.net.Uri.parse(bgUri)
            } else {
                android.net.Uri.parse("android.resource://${context.packageName}/raw/bg_ayanami")
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            this.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    // 釋放資源
    DisposableEffect(bgPlayer) {
        onDispose {
            bgPlayer.release()
        }
    }

    // 進度條狀態
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    // 歌詞
    val lrcLines = remember(song) {
        song?.let { MusicRepository.getLrc(it) } ?: emptyList()
    }
    // 背景切換動畫
    var bgAlpha by remember { mutableStateOf(1f) }
    val animatedBgAlpha by animateFloatAsState(
        targetValue = bgAlpha,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(song) {
        bgAlpha = 0f
        delay(500)  // 等背景 player 準備好
        bgAlpha = 1f
    }
    var currentLrcIndex by remember { mutableStateOf(0) }

// 更新當前歌詞行
    LaunchedEffect(currentPosition) {
        currentLrcIndex = LrcParser.findCurrentIndex(lrcLines, currentPosition + lrcOffset)
    }

    // 每秒更新進度
    LaunchedEffect(musicPlayer) {
        while (true) {
            if (!isSeeking) {
                currentPosition = musicPlayer.currentPosition
                duration = musicPlayer.duration.coerceAtLeast(0L)
                isPlaying = musicPlayer.isPlaying
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        // 背景影片
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = bgPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = {
                it.player = bgPlayer
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(animatedBgAlpha)
        )

        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 內容層
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // 左上角封面 + 歌名
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2A1A4E), shape = RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    AsyncImage(
                        model = song?.albumArtUri,
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = song?.title ?: "未知歌曲",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = song?.artist ?: "未知歌手",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 設定按鈕
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "設定", tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 歌詞區
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()

            LaunchedEffect(currentLrcIndex) {
                if (lrcLines.isNotEmpty() && currentLrcIndex >= 0) {
                    listState.animateScrollToItem(
                        index = (currentLrcIndex - 2).coerceAtLeast(0)
                    )
                }
            }

            if (lrcLines.isEmpty()) {
                Text(
                    text = "沒有歌詞檔案",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 80.dp)
                ) {
                    items(lrcLines.size) { index ->
                        val isCurrent = index == currentLrcIndex
                        Text(
                            text = lrcLines[index].text,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.25f),
                            fontSize = if (isCurrent) 22.sp else 16.sp,
                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 進度條
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = {
                        isSeeking = true
                        currentPosition = (it * duration).toLong()
                    },
                    onValueChangeFinished = {
                        musicPlayer.seekTo(currentPosition)
                        isSeeking = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${currentPosition / 60000}:${String.format("%02d", (currentPosition % 60000) / 1000)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${duration / 60000}:${String.format("%02d", (duration % 60000) / 1000)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 控制按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffleToggle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "隨機",
                        tint = if (isShuffling) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首",
                        tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(
                    onClick = {
                        if (musicPlayer.isPlaying) musicPlayer.pause()
                        else musicPlayer.play()
                        isPlaying = musicPlayer.isPlaying
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White, shape = CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暫停",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首",
                        tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onRepeatToggle) {
                    Icon(
                        when (repeatMode) {
                            2 -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "循環",
                        tint = when (repeatMode) {
                            0 -> Color.White.copy(alpha = 0.3f)
                            else -> Color.White
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "最愛",
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}