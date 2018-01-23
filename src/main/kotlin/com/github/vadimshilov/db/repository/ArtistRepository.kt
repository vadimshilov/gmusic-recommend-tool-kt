package com.github.vadimshilov.db.repository

import com.github.vadimshilov.db.Connection
import com.github.vadimshilov.db.domain.Artist
import com.github.vadimshilov.db.jdbc.SimpleJdbcInsert
import com.github.vadimshilov.db.jdbc.SimpleJdbcUpdate
import com.github.vadimshilov.db.jdbc.doInTransaction
import java.sql.ResultSet

object ArtistRepository {

    private const val TABLE_NAME = "artist"
    private const val RELATED_ARTIST_TABLE_NAME = "related_artists"

    fun save(artist : Artist) {
        if (artist.id == null) {
            val dbArtist = findByGoogleId(artist.googleId)
            artist.id = dbArtist?.id
        }
        doInTransaction {
            saveInternal(artist)
        }
    }

    fun save(artists : List<Artist>) {
        val artistWithoutId = mutableMapOf<String, Artist>()
        artists
                .filter { artist -> artist.id == null }
                .forEach{ artist -> artistWithoutId.put(artist.googleId, artist)}
        for (dbArtist in findAllByGoogleIds(artistWithoutId.keys)) {
            artistWithoutId[dbArtist.googleId]?.id = dbArtist.id
        }
        doInTransaction {
            for (artist in artists) {
                saveInternal(artist)
            }
        }
    }

    fun getArtistBlacklist() : List<String> {
        val connection = Connection.connection
        val stmt = connection.prepareStatement("SELECT google_id FROM artist_blacklist")
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableListOf<String>()
        while (resultSet.next()) {
            result.add(resultSet.getString(1))
        }
        resultSet.close()
        return result
    }

    fun saveRelatedArtists(artist: String, relatedArtists : List<String>) {
        val connection = Connection.connection
        doInTransaction {
            val stmt = connection.prepareStatement("DELETE FROM $RELATED_ARTIST_TABLE_NAME WHERE google_id1 = ?")
            stmt.setString(1, artist)
            val jdbcInsert = SimpleJdbcInsert(RELATED_ARTIST_TABLE_NAME)
            relatedArtists
                    .mapIndexed { ord, relatedArtist ->
                        mapOf<String, Any>(
                                "google_id1" to artist,
                                "google_id2" to relatedArtist,
                                "ord" to ord
                        )
                    }
                    .forEach { jdbcInsert.execute(it) }
        }
    }

    fun getRelatedArtists() : Map<String, List<String>> {
        val connection = Connection.connection
        val result = mutableMapOf<String, MutableList<String>>()
        val stmt = connection.prepareStatement(
                "SELECT google_id1, google_id2 FROM $RELATED_ARTIST_TABLE_NAME ORDER BY ord")
        stmt.execute()
        val resultSet = stmt.resultSet
        while (resultSet.next()) {
            val googleId1 = resultSet.getString(1)
            val googleId2 = resultSet.getString(2)
            if (!result.containsKey(googleId1)) {
                result.put(googleId1, mutableListOf())
            }
            result[googleId1]!!.add(googleId2)
        }
        return result
    }

    fun getGoogleIdIdMap() : Map<String, Int> {
        val connection = Connection.connection
        val stmt = connection.prepareStatement("SELECT id, google_id FROM $TABLE_NAME")
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableMapOf<String, Int>()
        while (resultSet.next()) {
            val id = resultSet.getInt(1)
            val googleId = resultSet.getString(2)
            result.put(googleId, id)
        }
        resultSet.close()
        return result
    }

    fun findAll() : List<Artist> {
        val connection = Connection.connection
        val stmt = connection.prepareStatement(
                "SELECT id, google_id, name, load_albums_date, load_date FROM $TABLE_NAME")
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableListOf<Artist>()
        while (resultSet.next()) {
            result.add(createArtistByResultSet(resultSet))
        }
        return result
    }

    private fun saveInternal(artist : Artist) {
        val valueMap = createValueMap(artist)
        if (artist.id == null) {
            val jdbcInsert = SimpleJdbcInsert(TABLE_NAME)
            artist.id = jdbcInsert.execute(valueMap)
        } else {
            val jdbcUpdate = SimpleJdbcUpdate(TABLE_NAME)
            jdbcUpdate.execute(valueMap, mapOf("id" to artist.id!!))
        }
    }

    private fun createValueMap(artist: Artist) : Map<String, Any> {
        val valueMap = mutableMapOf<String, Any>()
        if (artist.id != null) {
            valueMap.put("id", artist.id!!)
        }
        valueMap.put("google_id", artist.googleId)
        valueMap.put("name" , artist.name)
        if (artist.loadAlbumsDate != null) {
            valueMap.put("load_albums_date", artist.loadAlbumsDate!!)
        }
        if (artist.loadDate != null) {
            valueMap.put("load_date", artist.loadDate!!)
        }
        return valueMap
    }

    private fun findByGoogleId(googleId : String) : Artist? {
        val list = findAllByGoogleIds(listOf(googleId))
        return if (list.isEmpty()) null else list[0]
    }

    private fun findAllByGoogleIds(googleIds : Collection<String>) : List<Artist> {
        val connection = Connection.connection
        val sql = "SELECT id, google_id, name, load_albums_date, load_date FROM artist WHERE google_id IN "
        val result = mutableListOf<Artist>()
        var i = 0
        val googleIdList = googleIds.toList()
        while (i < googleIdList.size) {
            val j = minOf(i + 1000, googleIdList.size)
            val size = j - i
            val inClause = Array<String>(size, { "?" }).joinToString(",", "(", ")")
            val stmt = connection.prepareStatement(sql + inClause)
            var k = 1
            while (i < j) {
                stmt.setString(k++, googleIdList[i++])
            }
            stmt.execute()
            var resultSet = stmt.resultSet
            while (resultSet.next()) {
                result.add(createArtistByResultSet(resultSet))
            }
            resultSet.close()
        }
        return result
    }

    private fun createArtistByResultSet(resultSet : ResultSet) : Artist {
        val id = resultSet.getInt(1)
        val googleId = resultSet.getString(2)
        val name = resultSet.getString(3)
        val loadAlbumsDate = resultSet.getInt(4)
        val loadDate = resultSet.getInt(5)
        return Artist(id, googleId, name, loadAlbumsDate, loadDate)
    }

}