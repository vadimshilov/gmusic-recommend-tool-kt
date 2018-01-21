package com.github.vadimshilov

import com.github.felixgail.gplaymusic.api.GPlayMusic
import com.github.felixgail.gplaymusic.util.TokenProvider
import com.github.vadimshilov.controller.DataLoader
import com.github.vadimshilov.controller.RecommendationTool
import java.time.LocalDate

fun main(args: Array<String>) {
    val authToken = TokenProvider.provideToken("", "", "");
    val api = GPlayMusic.Builder().setAuthToken(authToken).build()

    DataLoader.loadSongData(api)
    RecommendationTool.createRecommendedPlaylist(api)
}