package com.github.vadimshilov.db.jdbc

import com.github.vadimshilov.db.Connection

class SimpleJdbcInsert (val tableName : String) {

    val connection = Connection.connection
    private companion object {
        const val INSERT_QUERY_FORMAT = "INSERT INTO %s(%s) VALUES(%s)"
    }

    fun execute(args : Map<String, Any>) : Int? {
        val columnValues = mutableListOf<Any>()
        val columnNames = mutableListOf<String>()
        val questions = mutableListOf<String>()
        args.forEach {key, value ->
            columnNames.add(key)
            columnValues.add(value)
            questions.add("?")
        }
        val sql = String.format(INSERT_QUERY_FORMAT, tableName,
                columnNames.joinToString(","), questions.joinToString(","))
        val stmt = connection.prepareStatement(sql)
        for ((i, columnValue) in columnValues.withIndex()) {
            setStatementValue(stmt, i + 1, columnValue)
        }
        stmt.execute()
        val resultSet = stmt.generatedKeys
        if (resultSet.next()) {
            return resultSet.getInt(1)
        } else {
            return null
        }
    }
}