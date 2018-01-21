package com.github.vadimshilov.db

import java.sql.Connection

object Migration {

    fun updateDatabaseIfNecessary(connection : Connection) {
        val stmt = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")
        stmt.setString(1, "db_version")
        stmt.execute()
        val resultSet = stmt.resultSet
        val tableExists = resultSet.next()
        resultSet.close()
        if (!tableExists) {
            createInitDatabase(connection)
        }
        while (true) {
            val version = getVersion(connection)
            if (version == 1) {
                versionTwo(connection)
            } else {
                break
            }
        }
    }

    private fun getVersion(connection : Connection) : Int {
        val stmt = connection.prepareStatement("SELECT version from db_version")
        stmt.execute()
        val resultSet = stmt.resultSet
        resultSet.next()
        return resultSet.getInt(1)
    }

    private fun createInitDatabase(connection: Connection) {
        with(connection) {
            prepareStatement("CREATE TABLE artist" +
                    "    (" +
                    "      id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "      google_id        VARCHAR," +
                    "      name             VARCHAR," +
                    "      load_albums_date BIGINT" +
                    "    );")
                    .execute()

            prepareStatement("create table album" +
                    "(" +
                    "id INTEGER primary key autoincrement," +
                    "google_id VARCHAR," +
                    "name VARCHAR," +
                    "artist_id INTEGER references artist (id) on update cascade on delete cascade" +
                    ");")
                    .execute()

            prepareStatement("CREATE TABLE artist_blacklist" +
                    "(" +
                    "  google_id VARCHAR" +
                    ");")
                    .execute()

            prepareStatement("CREATE TABLE db_version" +
                    "(" +
                    "  version INTEGER" +
                    ");")
                    .execute()

            prepareStatement("INSERT INTO db_version(version) VALUES(1)").execute()
            prepareStatement("CREATE TABLE history" +
                    "(" +
                    "  song_id         INT" +
                    "    REFERENCES song (id)" +
                    "      ON UPDATE CASCADE" +
                    "      ON DELETE CASCADE," +
                    "  event_date      INTEGER," +
                    "  playcount_delta INTEGER" +
                    ");")
                    .execute()

            prepareStatement("CREATE UNIQUE INDEX history_idx ON history (song_id, event_date);").execute()

            prepareStatement("CREATE TABLE related_artists" +
                    "(" +
                    "  google_id1 VARCHAR," +
                    "  google_id2 VARCHAR," +
                    "  ord        INT" +
                    ");")
                    .execute()

            prepareStatement("CREATE TABLE song" +
                    "(" +
                    "  id              INTEGER" +
                    "    PRIMARY KEY" +
                    "  AUTOINCREMENT," +
                    "  name            VARCHAR," +
                    "  album_id        INTEGER" +
                    "    REFERENCES album (id)" +
                    "      ON UPDATE CASCADE" +
                    "      ON DELETE CASCADE," +
                    "  rate            SMALLINT," +
                    "  google_id       VARCHAR," +
                    "  playcount       INT," +
                    "  album_google_id VARCHAR," +
                    "  artist_id       INTEGER" +
                    "    REFERENCES artist (id)" +
                    "      ON UPDATE CASCADE" +
                    "      ON DELETE CASCADE," +
                    "  ord             INT" +
                    ");")
                    .execute()
        }
    }

    private fun versionTwo(connection: Connection) {
        with(connection) {
            prepareStatement("ALTER TABLE song ADD COLUMN load_date BIGINT").execute()
            prepareStatement("ALTER TABLE artist ADD COLUMN load_date BIGINT").execute()
            prepareStatement("UPDATE artist SET load_date = 0").execute()
            prepareStatement("UPDATE song SET load_date = 0").execute()
            prepareStatement("UPDATE db_version SET version = 2").execute()
        }
    }

}