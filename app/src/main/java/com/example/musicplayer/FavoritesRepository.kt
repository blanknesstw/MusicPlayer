package com.example.musicplayer

import android.content.Context

object FavoritesRepository {
    private const val PREFS_NAME = "favorites"
    private const val KEY_FAVORITES = "favorite_paths"

    fun addFavorite(context: Context, songPath: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getFavoritePaths(context).toMutableSet()
        current.add(songPath)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun removeFavorite(context: Context, songPath: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getFavoritePaths(context).toMutableSet()
        current.remove(songPath)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun isFavorite(context: Context, songPath: String): Boolean {
        return getFavoritePaths(context).contains(songPath)
    }

    fun getFavoritePaths(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun getFavoriteSongs(context: Context, allSongs: List<Song>): List<Song> {
        val paths = getFavoritePaths(context)
        return allSongs.filter { it.path in paths }
    }
}