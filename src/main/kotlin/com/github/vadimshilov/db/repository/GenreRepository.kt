package com.github.vadimshilov.db.repository

import com.github.vadimshilov.db.Connection
import com.github.vadimshilov.db.domain.Genre
import com.github.vadimshilov.db.jdbc.SimpleJdbcInsert
import com.github.vadimshilov.db.jdbc.SimpleJdbcUpdate

object GenreRepository {

    private const val TABLE_NAME = "genre";

    fun saveAll(genreList : List<Genre>) {
        val dbGenreMap = findAll().map { it.googleId to it } .toMap()
        genreList.forEach { genre -> saveOne(genre, dbGenreMap[genre.googleId]) }
    }

    private fun saveOne(genre : Genre, dbGenre : Genre?) {
        if (dbGenre == null) {
            val genreInsert = SimpleJdbcInsert(TABLE_NAME)
            genre.id = genreInsert.execute(createValueMap(genre))
        } else {
            val genreUpdate = SimpleJdbcUpdate(TABLE_NAME)
            genreUpdate.execute(createValueMap(genre), mapOf("id" to dbGenre.id!!))
            genre.id = dbGenre.id
        }
    }

    private fun createValueMap(genre : Genre) : Map<String, Any> {
        val result = mutableMapOf<String, Any>(
                "google_id" to genre.googleId,
                "name" to genre.name
        )
        if (genre.id != null) {
            result["id"] = genre.id!!
        }
        if (genre.parentId != null) {
            result["parent_id"] = genre.parentId
        }
        return result
    }

    fun findAll() : List<Genre> {
        val connection = Connection.connection
        val sql = "SELECT id, google_id, name, parent_id FROM $TABLE_NAME";
        val stmt = connection.prepareStatement(sql)
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableListOf<Genre>()
        while (resultSet.next()) {
            val id = resultSet.getInt(1)
            val googleId = resultSet.getString(2)
            val name = resultSet.getString(3)
            val parentId = resultSet.getInt(4)
            result.add(Genre(id, googleId, name, parentId))
        }
        return result
    }

}