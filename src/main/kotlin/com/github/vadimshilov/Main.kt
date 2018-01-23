package com.github.vadimshilov

import com.github.vadimshilov.controller.DataLoader
import com.github.vadimshilov.controller.RecommendationTool

fun main(args: Array<String>) {
    val api = ApiProvider.api

    DataLoader.loadSongData(api)
    RecommendationTool.createRecommendedPlaylist(api)
}