package com.example.musicplayer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp

@Composable
fun SaveAnimation(
    onFinish: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var showText by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    // 打亂顏色放在 Composable 層級
    val shuffledColors = remember {
        listOf(
            Color(0xFF0D47A1),
            Color(0xFF1565C0),
            Color(0xFF1976D2),
            Color(0xFF1E88E5),
            Color(0xFF2196F3),
            Color(0xFF42A5F5),
            Color(0xFF64B5F6),
            Color(0xFF90CAF9),
            Color(0xFF0A2472),
            Color(0xFF0E6BA8),
            Color(0xFF0288D1),
            Color(0xFF0277BD),
            Color(0xFF01579B),
            Color(0xFF29B6F6),
            Color(0xFF4FC3F7),
            Color(0xFF81D4FA),
            Color(0xFF1A237E),
            Color(0xFF283593),
            Color(0xFF3949AB),
            Color(0xFF5C6BC0),
        ).shuffled()
    }

    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(800, easing = FastOutSlowInEasing)) { value, _ ->
            progress = value
        }
        showText = true
        delay(600)
        leaving = true
        animate(1f, 0f, animationSpec = tween(600, easing = FastOutSlowInEasing)) { value, _ ->
            progress = value
        }
        onFinish()
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (showText && !leaving) 1f else 0f,
        animationSpec = tween(300)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lineCount = 5
            val lineWidth = w * 0.4f

            for (i in 0 until lineCount) {
                val ratio = i.toFloat() / (lineCount - 1)
                val startX = -lineWidth + (w + lineWidth * 2f) * ratio - h * 0.4f
                val startY = h + lineWidth

                val dist = (w + h) * 2.0f * progress
                val endX = startX + dist
                val endY = startY - dist

                drawLine(
                    color = shuffledColors[i % shuffledColors.size],
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Butt
                )
            }
        }

        Text(
            text = "已儲存",
            color = Color.White.copy(alpha = textAlpha),
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 100.dp, bottom = 200.dp)
        )
    }
}