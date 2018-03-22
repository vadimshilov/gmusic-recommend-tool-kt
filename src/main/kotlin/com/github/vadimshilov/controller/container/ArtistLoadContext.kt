package com.github.vadimshilov.controller.container

import com.github.vadimshilov.db.domain.Artist
import com.github.vadimshilov.db.domain.Genre

data class ArtistLoadContext(val loadedArtists : MutableSet<String>,
                             val artistBlacklist : Set<String>,
                             val promotedArtist : Set<String>,
                             val artistData : Map<String, Artist>,
                             val genres : Map<String, Genre>) {
}