package com.github.vadimshilov.db.domain

import com.github.vadimshilov.util.DateUtil

data class Song(var id : Int?,
                val googleId : String,
                val name : String,
                var albumId : Int?,
                val rate : Int,
                val playcount : Int,
                val albumGoogleId : String,
                val artistId : Int,
                val ord : Int,
                val genre : Int?,
                val loadDate : Int = DateUtil.getDateInDays()) {
}