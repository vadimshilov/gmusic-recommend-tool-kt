package com.github.vadimshilov.controller

import com.github.felixgail.gplaymusic.api.GPlayMusic
import com.github.felixgail.gplaymusic.model.Track
import com.github.vadimshilov.db.domain.Artist
import com.github.vadimshilov.db.domain.Song
import com.github.vadimshilov.db.repository.ArtistRepository
import com.github.vadimshilov.db.repository.SongRepository
import com.github.vadimshilov.util.DateUtil

object DataLoader {

    fun loadSongData(api : GPlayMusic) {
        val songs = api.promotedTracks
        val artists = mutableSetOf<String>()
        val likedSongs = mutableSetOf<String>()
        for (song in songs) {
            artists.add(song.artistId.get()[0])
            likedSongs.add(song.nid.get())
        }
        val loadedArtists = mutableSetOf<String>()
        val artistBlacklist = ArtistRepository.getArtistBlacklist().toSet()
        val artistData = ArtistRepository.findAll()
                .map { it.googleId to it }
                .toMap()

        for (id in artists) {

            loadArtist(api, if (id.startsWith('A')) id else "A" + id, loadedArtists, true, artistBlacklist,
                    artists, artistData)
        }

        val artistMap = ArtistRepository.getGoogleIdIdMap()
        for (song in songs) {
           var artistId = song.artistId.get()[0]
            if (!artistId.startsWith('A')) {
                artistId = 'A' + artistId
            }
            if (artistMap.containsKey(artistId)) {
                initTracks(listOf(song), Artist(artistMap.get(artistId), "", "", null, null), false)
            }
        }
    }

    private fun loadArtist(api : GPlayMusic, id : String, loadedArtists : MutableSet<String>, loadRelated : Boolean,
                           artistBlacklist : Set<String>, promotedArtist : Set<String>,
                           artistData : Map<String, Artist>) {
        if (loadedArtists.contains(id) || artistBlacklist.contains(id)) {
            return
        }
        loadedArtists.add(id)
        println("Retrieving artist with ID = $id")
        var iterations = 10
        var artist : com.github.felixgail.gplaymusic.model.Artist? = null
        while (artist == null && iterations > 0) {
            try {
                artist = api.getArtist(id, true, 1000, 20)
            } catch (e : Throwable) {
                iterations--
            }
        }
        if (artist == null) {
            return
        }
        println("Artist ${artist.name} retrieved")
        if (!artist.topTracks.isPresent) {
            return
        }
        val dbArtist = createDbArtist(artist)
        dbArtist.loadAlbumsDate = artistData[dbArtist.googleId]?.loadAlbumsDate
        ArtistRepository.save(dbArtist)
        val dbTracks = initTracks(artist.topTracks.get(), dbArtist, true)
        val trackGoogleIds = dbTracks
                .map { it.googleId }
                .toMutableSet()
        var loadAlbums = true
        val today = DateUtil.getDateInDays()
        if (shouldLoadAlbums(dbTracks) && artist.albums.isPresent) {
            if (dbArtist.loadAlbumsDate != null && today - dbArtist.loadAlbumsDate!! < 7) {
                loadAlbums = true
            }
            if (loadAlbums) {
                artist.albums.get()
                        .map { api.getAlbum(it.albumId, true) }
                        .filterNot { it.tracks.isPresent }
                        .map { initTracks(it.tracks.get(), dbArtist, false) }
                        .forEach { albumTracks ->
                            albumTracks
                                    .filter { trackGoogleIds.add(it.googleId) }
                                    .forEach{ track : Song -> dbTracks.add(track)}
                        }
                dbArtist.loadAlbumsDate = today
                ArtistRepository.save(dbArtist)
            }
        }
        if (loadAlbums) {
            dbArtist.loadDate = today
            ArtistRepository.save(dbArtist)
        }
        SongRepository.save(dbTracks)
        if ((loadRelated || shouldLoadRelatedArtists(dbTracks) || promotedArtist.contains(id)) &&
                artist.relatedArtists.isPresent) {
            val relatedIds = artist.relatedArtists.get()
                    .map { it.artistId.get() }
                    .toList()
            ArtistRepository.saveRelatedArtists(id, relatedIds)
            for (relatedId in relatedIds) {
                val artistId = if (relatedId.startsWith('A')) relatedId else 'A' + relatedId
                loadArtist(api, artistId, loadedArtists, false, artistBlacklist, promotedArtist, artistData)
            }
        }
    }

    private fun shouldLoadRelatedArtists(tracks : List<Song>) : Boolean {
        var totalPlaycount = tracks
                .map {it.playcount}
                .sum()
        println("Total playcount $totalPlaycount")
        return totalPlaycount >= 50
    }

    private fun shouldLoadAlbums(tracks : List<Song>) : Boolean  {
        if (tracks.isEmpty()) {
            return false
        }
        var knownTrackCount = 0
        for (track in tracks) {
            if (track.playcount >= 10 || track.rate == 5) {
                knownTrackCount++
            }
        }
        return knownTrackCount * 2 >= tracks.size
    }

    private fun createDbArtist(artist : com.github.felixgail.gplaymusic.model.Artist) : Artist {
        val googleId = artist.artistId.get()
        val name = artist.name
        return Artist(null, googleId, name, null, null)
    }

    private fun initTracks(tracks : List<Track>, artist : Artist, initOrd : Boolean) : MutableList<Song> {
        val dbTracks = mutableListOf<Song>()
        var i = 0
        for (track in tracks) {
            val googleId = track.storeId.get()
            val name = track.title
            val rate = track.rating
                    .map { it.toInt() }
                    .orElse(0)
            val playcount = track.playCount.orElse(0)
            val albumId = null
            val albumGoogleId = track.albumId
            val ord = if (initOrd) i else 2000000000
            dbTracks.add(Song(null, googleId, name, albumId, rate, playcount, albumGoogleId, artist.id!!, ord))
            i++
        }
        SongRepository.save(dbTracks)
        return dbTracks
    }

}