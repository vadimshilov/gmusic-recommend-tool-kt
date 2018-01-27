package com.github.vadimshilov.db.domain

data class Genre(var id : Int?,
                 val googleId : String,
                 val name : String,
                 val parentId : Int?) {
}