package com.example.musicplayer

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object MusicRepository {
    fun getSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val cursor = context.contentResolver.query(
            uri, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, "${MediaStore.Audio.Media.TITLE} ASC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val path = it.getString(pathCol)
                val songUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                )
                val albumArtUri = Uri.parse("content://media/external/audio/albumart/$id")
                
                // 自動配對歌詞：檢查同路徑下是否有同名的 .lrc 檔案
                val lyrics = try {
                    val lrcPath = path.substringBeforeLast(".") + ".lrc"
                    val lrcFile = File(lrcPath)
                    if (lrcFile.exists()) {
                        LrcParser.parse(lrcFile.readText())
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                songs.add(Song(
                    id = id,
                    title = it.getString(titleCol),
                    artist = it.getString(artistCol),
                    album = it.getString(albumCol),
                    duration = it.getLong(durationCol),
                    uri = songUri,
                    albumArtUri = albumArtUri,
                    path = path,
                    lyrics = lyrics
                ))
            }
        }
        android.util.Log.d("MusicRepo", "找到 ${songs.size} 首歌")
        return songs
    }

    fun getLrc(song: Song): List<LrcLine> {
        if (song.lyrics.isNotEmpty()) return song.lyrics
        val mp3Path = song.path
        val lrcPath = mp3Path.substringBeforeLast(".") + ".lrc"
        val lrcFile = File(lrcPath)
        if (!lrcFile.exists()) return emptyList()
        return LrcParser.parse(lrcFile.readText())
    }
}