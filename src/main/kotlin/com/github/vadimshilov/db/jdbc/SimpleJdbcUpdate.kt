package com.github.vadimshilov.db.jdbc

import com.github.vadimshilov.db.Connection

class SimpleJdbcUpdate(val tableName : String) {

    val connection = Connection.connection

    private companion object {
        const val UPDATE_QUERY_FORMAT = "UPDATE %s SET %s WHERE %s"
    }

    fun execute(args : Map<String, Any>, condition : Map<String, Any>) {
        val setClause = mutableSetOf<String>()
        val valueList = mutableListOf<Any>()

        args.forEach { key, value ->
            setClause.add(key + " = ?")
            valueList.add(value)
        }

        val whereClause = mutableListOf<String>()
        condition.forEach { key, value ->
            whereClause.add(key + " = ?")
            valueList.add(value)
        }

        val sql = String.format(UPDATE_QUERY_FORMAT,
                tableName, setClause.joinToString(", "), whereClause.joinToString(" AND "))

        val stmt = connection.prepareStatement(sql)

        for ((i, value) in valueList.withIndex()) {
            setStatementValue(stmt, i + 1, valueList[i])
        }

        stmt.execute()
    }

}