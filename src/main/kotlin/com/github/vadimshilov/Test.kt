package com.github.vadimshilov

import com.github.felixgail.gplaymusic.util.TokenProvider
import com.github.felixgail.gplaymusic.api.GPlayMusic
import java.time.LocalDate


fun main(args: Array<String>) {
    val authToken = TokenProvider.provideToken("vadim.shilov@gmail.com", "ovyqtsrjwgwlhdxx", "3afb5b9dfe62f4a0");
    val api = GPlayMusic.Builder().setAuthToken(authToken).build()

//    api.promotedTracks.forEach{track -> print(track)}


    print (LocalDate.now().toEpochDay() - LocalDate.of(1,1,1).toEpochDay() + 1)
}