package com.github.vadimshilov

import com.github.felixgail.gplaymusic.api.GPlayMusic
import com.github.felixgail.gplaymusic.util.TokenProvider
import java.io.FileInputStream
import java.util.*

class ApiProvider private constructor() {

    private object Holder {

        private const val FILE_NAME = "config.properties"
        private const val EMAIL_PROPERTY = "login.email"
        private const val PASSWORD_PROPERTY = "login.password"
        private const val ANDROID_ID_PROPERTY = "3afb5b9dfe62f4a0"

        val api = initApi()

        fun initApi() : GPlayMusic {
            val properties = Properties()
            val inputStream = FileInputStream(FILE_NAME)
            properties.load(inputStream)
            val email = properties[EMAIL_PROPERTY]
            val password = properties[PASSWORD_PROPERTY]
            val androidId = properties[ANDROID_ID_PROPERTY]
            val authToken = TokenProvider.provideToken(email.toString(), password.toString(), androidId.toString())
            return GPlayMusic.Builder().setAuthToken(authToken).build()
        }
    }

    companion object {
        val api : GPlayMusic by lazy { Holder.api }
    }

}