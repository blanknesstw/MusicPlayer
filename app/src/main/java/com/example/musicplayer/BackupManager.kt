package com.example.musicplayer

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    fun backup(context: Context): Boolean {
        return try {
            val json = JSONObject()

            // 最愛
            val favorites = FavoritesRepository.getFavoritePaths(context)
            val favArray = JSONArray()
            favorites.forEach { favArray.put(it) }
            json.put("favorites", favArray)

            // 歌詞偏移
            val lrcPrefs = context.getSharedPreferences("lrc_offsets", Context.MODE_PRIVATE)
            val lrcJson = JSONObject()
            lrcPrefs.all.forEach { (k, v) -> lrcJson.put(k, v) }
            json.put("lrc_offsets", lrcJson)

            // 背景 URI
            val bgPrefs = context.getSharedPreferences("bg_uris", Context.MODE_PRIVATE)
            val bgJson = JSONObject()
            bgPrefs.all.forEach { (k, v) -> bgJson.put(k, v) }
            json.put("bg_uris", bgJson)

            // 寫入檔案
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "MusicPlayer_backup.json"
            )
            file.writeText(json.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restore(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val json = JSONObject(file.readText())

            // 還原最愛
            val favArray = json.getJSONArray("favorites")
            val favSet = mutableSetOf<String>()
            for (i in 0 until favArray.length()) {
                favSet.add(favArray.getString(i))
            }
            context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
                .edit().putStringSet("favorite_paths", favSet).apply()

            // 還原歌詞偏移
            val lrcJson = json.getJSONObject("lrc_offsets")
            val lrcEditor = context.getSharedPreferences("lrc_offsets", Context.MODE_PRIVATE).edit()
            lrcJson.keys().forEach { key ->
                lrcEditor.putLong(key, lrcJson.getLong(key))
            }
            lrcEditor.apply()

            // 還原背景 URI
            val bgJson = json.getJSONObject("bg_uris")
            val bgEditor = context.getSharedPreferences("bg_uris", Context.MODE_PRIVATE).edit()
            bgJson.keys().forEach { key ->
                bgEditor.putString(key, bgJson.getString(key))
            }
            bgEditor.apply()

            true
        } catch (e: Exception) {
            false
        }
    }
}