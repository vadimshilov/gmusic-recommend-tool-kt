package com.github.vadimshilov.db.jdbc

import com.github.vadimshilov.db.Connection
import java.sql.PreparedStatement

fun setStatementValue(statement : PreparedStatement, index : Int, value : Any) {
    when (value) {
        is Number -> statement.setInt(index, value.toInt())
        is String -> statement.setString(index, value)
        else -> statement.setString(index, value.toString())
    }
}

fun doInTransaction(callback : () -> Unit) {
    val connection = Connection.connection
    try {
        connection.createStatement().execute("begin")

        callback()

        connection.createStatement().execute("end")
    } catch (e : Exception) {
        connection.createStatement().execute("rollback")
        e.printStackTrace()
    }
}