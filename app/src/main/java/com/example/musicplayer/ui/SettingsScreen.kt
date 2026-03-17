package com.example.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.musicplayer.LrcParser
import com.example.musicplayer.MusicRepository
import com.example.musicplayer.Song
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    song: Song?,
    musicPlayer: Player,
    lrcOffset: Long,
    onOffsetChange: (Long) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onPickBackground: () -> Unit,
    currentBgUri: String?,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val lrcLines = remember(song) {
        song?.let { MusicRepository.getLrc(it) } ?: emptyList()
    }
    var currentPosition by remember { mutableStateOf(0L) }
    var currentLrcIndex by remember { mutableStateOf(0) }
    var showSaveAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(musicPlayer) {
        while (true) {
            currentPosition = musicPlayer.currentPosition
            delay(100)
        }
    }

    LaunchedEffect(currentPosition, lrcOffset) {
        currentLrcIndex = LrcParser.findCurrentIndex(lrcLines, currentPosition + lrcOffset)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
        ) {
            // 頂部返回列
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 8.dp, end = 28.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Text(
                    text = "設定",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 內容區（可滾動）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 背景設定區
                Text(
                    text = "背景設定",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPickBackground,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = if (currentBgUri != null) "已設定背景 ✓" else "選擇背景影片或圖片",
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(32.dp))

// 備份還原區
                Text(
                    text = "備份與還原",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBackup,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Text("備份設定", color = Color.White)
                    }
                    Button(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Text("還原設定", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(32.dp))

                // 歌詞時間調整區
                Text(
                    text = "歌詞時間調整",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (lrcLines.isEmpty()) {
                        Text(
                            text = "沒有歌詞檔案",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = lrcLines.getOrNull(currentLrcIndex - 1)?.text ?: "",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = lrcLines.getOrNull(currentLrcIndex)?.text ?: "",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = lrcLines.getOrNull(currentLrcIndex + 1)?.text ?: "",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when {
                        lrcOffset > 0 -> "歌詞提早 $lrcOffset ms"
                        lrcOffset < 0 -> "歌詞延遲 ${-lrcOffset} ms"
                        else -> "歌詞時間正常"
                    },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onOffsetChange(lrcOffset - 500) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("-500ms") }
                    OutlinedButton(
                        onClick = { onOffsetChange(lrcOffset - 100) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("-100ms") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onOffsetChange(0L) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) { Text("重置", color = Color.White) }
                    OutlinedButton(
                        onClick = { onOffsetChange(lrcOffset + 100) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("+100ms") }
                    OutlinedButton(
                        onClick = { onOffsetChange(lrcOffset + 500) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("+500ms") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSave()
                        showSaveAnimation = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("儲存", color = Color.Black, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // 動畫覆蓋層
        if (showSaveAnimation) {
            SaveAnimation(onFinish = { showSaveAnimation = false })
        }
    }
}