package com.github.vadimshilov.db.repository

import com.github.vadimshilov.db.Connection
import com.github.vadimshilov.db.domain.Song
import com.github.vadimshilov.db.jdbc.SimpleJdbcInsert
import com.github.vadimshilov.db.jdbc.SimpleJdbcUpdate
import com.github.vadimshilov.db.jdbc.doInTransaction
import com.github.vadimshilov.util.DateUtil
import java.sql.ResultSet

object SongRepository {

    private val TABLE_NAME = "song"
    private val HISTORY_TABLE_NAME = "history"

    fun save(songs : List<Song>) {
        val googleIdSongMap = songs.map {it.googleId}.toSet()
        val dbSongMap = findAllByGoogleIds(googleIdSongMap).map {it.googleId to it}.toMap()
        doInTransaction {
            for (song in songs) {
                saveOne(song, dbSongMap.get(song.googleId))
            }
        }
    }

    fun findAll() : List<Song> {
        val sql = "SELECT " +
              "song.id, song.google_id, song.name, album_id, rate, playcount, album_google_id, artist_id, ord " +
              "FROM song INNER JOIN artist ON artist.load_date <= song.load_date AND artist.id = song.artist_id " +
              "ORDER BY ord - playcount"
        val connection = Connection.connection
        val stmt = connection.prepareStatement(sql)
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableListOf<Song>()
        while (resultSet.next()) {
            result.add(createSongByResultSet(resultSet))
        }
        resultSet.close()
        return result
    }

    fun findPlayCountHistory(fromDate : Int, toDate : Int) : Map<Int, Int> {
        val connection = Connection.connection
        val sql = "SELECT song_id, sum(playcount_delta) FROM history GROUP BY song_id " +
                "HAVING event_date <= ? and event_date > ?"
        val stmt = connection.prepareStatement(sql)
        stmt.setInt(1, toDate)
        stmt.setInt(2, fromDate)
        stmt.execute()
        val resultSet = stmt.resultSet
        val result = mutableMapOf<Int, Int>()
        while (resultSet.next()) {
            val songId = resultSet.getInt(1)
            val playcount = resultSet.getInt(2)
            result.put(songId, playcount)
        }
        return result
    }

    private fun saveOne(song : Song, dbSong : Song?) {
        if (dbSong == null) {
            val songInsert = SimpleJdbcInsert(TABLE_NAME)
            song.id = songInsert.execute(createValueMap(song))
            if (song.playcount != 0) {
                insertIntoHistory(song.id!!, song.playcount)
            }
        } else {
            val songUpdate = SimpleJdbcUpdate(TABLE_NAME)
            songUpdate.execute(createValueMap(song), mapOf("id" to dbSong.id!!))
            var playcountDelta = song.playcount - dbSong.playcount
            if (playcountDelta > 0) {
                val connection = Connection.connection
                val sql = "SELECT playcount_delta FROM history WHERE song_id = ? AND event_date = ?"
                val stmt = connection.prepareStatement(sql)
                stmt.setInt(1, dbSong.id!!)
                stmt.setInt(2, DateUtil.getDateInDays())
                stmt.execute()
                val resultSet = stmt.resultSet
                if (resultSet.next()) {
                    playcountDelta += resultSet.getInt(1)
                    val historyUpdate = SimpleJdbcUpdate(HISTORY_TABLE_NAME)
                    historyUpdate.execute(mapOf("playcount_delta" to playcountDelta),
                            mapOf("song_id" to dbSong.id!!,
                                    "event_date" to DateUtil.getDateInDays())
                    )
                } else {
                    insertIntoHistory(dbSong.id!!, playcountDelta)
                }
                resultSet.close()
            }
        }
    }

    private fun insertIntoHistory(songId : Int, playcount : Int) {
        val historyInsert = SimpleJdbcInsert(HISTORY_TABLE_NAME)
        historyInsert.execute(mapOf(
                "song_id" to songId,
                "event_date" to DateUtil.getDateInDays(),
                "playcount_delta" to playcount
        ))
    }

    private fun createValueMap(song : Song) : Map<String, Any> {
        val result = mutableMapOf(
                "google_id" to song.googleId,
                "name" to song.name,
                "rate" to song.rate,
                "playcount" to song.playcount,
                "album_google_id" to song.albumGoogleId,
                "artist_id" to song.artistId,
                "ord" to song.ord
        )
        if (song.albumId != null) {
            result.put("album_id", song.albumId!!)
        }
        if (song.loadDate != null) {
            result.put("load_date", song.loadDate!!)
        }
        return result
    }

    private fun findAllByGoogleIds(googleIds : Collection<String>) : List<Song> {
        val connection = Connection.connection
        val sql = "SELECT id, google_id, name, album_id, rate, playcount, album_google_id, artist_id, ord " +
                  " FROM $TABLE_NAME WHERE google_id IN "
        val result = mutableListOf<Song>()
        var i = 0
        val googleIdList = googleIds.toList()
        while (i < googleIdList.size) {
            val j = minOf(i + 1000, googleIdList.size)
            val size = j - i
            val inClause = Array(size, { "?" }).joinToString(",", "(", ")")
            val stmt = connection.prepareStatement(sql + inClause)
            var k = 1
            while (i < j) {
                stmt.setString(k++, googleIdList[i++])
            }
            stmt.execute()
            val resultSet = stmt.resultSet
            while (resultSet.next()) {
                result.add(createSongByResultSet(resultSet))
            }
            resultSet.close()
        }
        return result
    }

    private fun createSongByResultSet(resultSet : ResultSet) : Song {
        val id = resultSet.getInt(1)
        val googleId = resultSet.getString(2)
        val name = resultSet.getString(3)
        val albumId = resultSet.getInt(4)
        val rate = resultSet.getInt(5)
        val playcount = resultSet.getInt(6)
        val albumGoogleId = resultSet.getString(7)
        val artistId = resultSet.getInt(8)
        val ord = resultSet.getInt(9)
        return Song(id, googleId, name, albumId, rate, playcount, albumGoogleId, artistId, ord)
    }

}