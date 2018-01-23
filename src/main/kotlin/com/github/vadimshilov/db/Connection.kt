package com.github.vadimshilov.db

import java.sql.DriverManager

class Connection private constructor() {

    private object Holder {
        val connection = initConnection()

        fun initConnection() : java.sql.Connection {
            val connection = DriverManager.getConnection("jdbc:sqlite:db.db")
            connection.autoCommit = false
            try {
                connection.createStatement().execute("commit")
            } catch (e : Exception) {
                e.printStackTrace()
            }
            Migration.updateDatabaseIfNecessary(connection)
            return connection
        }
    }

    companion object {
        val connection: java.sql.Connection by lazy { Holder.connection }
    }
}