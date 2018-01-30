package com.github.vadimshilov.util

import com.github.vadimshilov.db.domain.Song
import com.github.vadimshilov.util.domain.SongGroup

object SongUtil {

    fun createGroup(songs : List<Song>) : List<SongGroup> {
        val songMap = mutableMapOf<String, MutableList<Song>>()
        val songOrd = mutableMapOf<String, Int>()
        for (song in songs) {
            var added = false
            for ((name, list) in songMap) {
                if (isPrefix(name, song.name) || song.name == name) {
                    list.add(song)
                    added = true
                    break
                }
                if (isPrefix(song.name, name)) {
                    list.add(song)
                    songMap[song.name] = list
                    songMap.remove(name)
                    songOrd[song.name] = songOrd[name]!!
                    songOrd.remove(name)
                    added = true
                    break
                }
            }
            if (!added) {
                songMap[song.name] = mutableListOf(song)
                songOrd[song.name] = songOrd.size
            }
        }

        return songMap.entries
                .sortedBy { songOrd[it.key] }
                .map { it.value }
                .map { songs ->
                    val playcount = songs.map { it.playcount }.sum()
                    var rate = 0
                    for (song in songs) {
                        if (song.rate == 1) {
                            rate = 1
                            break
                        } else if (song.rate == 5) {
                            rate = 5
                        }
                    }
                    SongGroup(playcount, rate, songs)
                }
    }

    private fun isPrefix(prefix : String, name : String) : Boolean {
        return name.startsWith(prefix + " (") ||
                name.startsWith(prefix + "(") ||
                name.startsWith(prefix + " [") ||
                name.startsWith(prefix + "[")
    }

}