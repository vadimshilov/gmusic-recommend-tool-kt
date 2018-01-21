package com.github.vadimshilov.db.domain

data class Artist(var id : Int?,
                  val googleId : String,
                  val name : String,
                  var loadAlbumsDate : Int?,
                  var loadDate : Int?) {
}