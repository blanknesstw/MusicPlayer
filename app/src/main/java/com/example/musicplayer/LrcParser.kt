package com.example.musicplayer

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

        lrcContent.lines().forEach { line ->
            val match = regex.find(line) ?: return@forEach
            val (min, sec, ms, text) = match.destructured
            val timeMs = min.toLong() * 60000 +
                    sec.toLong() * 1000 +
                    ms.toLong() * (if (ms.length == 2) 10L else 1L)
            if (text.isNotBlank()) {
                lines.add(LrcLine(timeMs, text.trim()))
            }
        }

        return lines.sortedBy { it.timeMs }
    }

    fun findCurrentIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var index = 0
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i
            else break
        }
        return index
    }
}
