package com.github.vadimshilov.util.domain

import com.github.vadimshilov.db.domain.Song

data class SongGroup(
        val playcount : Int,
        val rate : Int,
        val songs : List<Song>
)