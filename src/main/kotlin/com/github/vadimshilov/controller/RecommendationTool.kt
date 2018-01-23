package com.github.vadimshilov.controller

import com.github.felixgail.gplaymusic.api.GPlayMusic
import com.github.felixgail.gplaymusic.model.Playlist
import com.github.felixgail.gplaymusic.model.Track
import com.github.vadimshilov.db.domain.Song
import com.github.vadimshilov.db.repository.ArtistRepository
import com.github.vadimshilov.db.repository.SongRepository
import com.github.vadimshilov.util.DateUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.*

object RecommendationTool {

    fun createRecommendedPlaylist(api : GPlayMusic) {
        val songs = SongRepository.findAll()
        val songMap = mutableMapOf<Int, MutableList<Song>>()
        for (song in songs) {
            songMap.getOrPut(song.artistId, { mutableListOf() }).add(song)
        }
        var artistScore = mutableMapOf<Int, Double>()
        val relatedScore = mutableMapOf<Int, Double>()
        val historyPlaycount = getPlaycountHistory()
        val googleIdIdMap = ArtistRepository.getGoogleIdIdMap()
        val blackList = ArtistRepository.getArtistBlacklist()
                .map { googleIdIdMap[it] }
                .filter { it != null }
                .toSet()
        for ((artistId, songs) in songMap) {
            if (blackList.contains(artistId)) {
                continue
            }
            artistScore[artistId] = maxOf(calcOwnScore(songs, historyPlaycount), 0.0)
            relatedScore[artistId] = 0.0
            songs.removeIf{ it.rate == 1 }
        }

        val relatedArtists = ArtistRepository.getRelatedArtists()
        for ((artistId, links) in relatedArtists) {
            if (!googleIdIdMap.containsKey(artistId)) {
               continue
            }
            val id1 = googleIdIdMap[artistId]
            if (!artistScore.containsKey(id1)) {
                continue
            }
            var i = 0
            for (link in links) {
                if (!googleIdIdMap.containsKey(link)) {
                    continue
                }
                val id2 = googleIdIdMap[link]!!
                if (!relatedScore.containsKey(id2)) {
                    continue
                }
                var koef : Double
                if (i * 3 < links.size) {
                    koef = 1.0
                } else if (i * 3 < links.size * 2) {
                    koef = 1.5
                } else {
                    koef = 2.75
                }
                relatedScore[id2] = relatedScore[id2]!! + artistScore[id1]!! / koef / links.size / 1.5
                i++
            }
        }
        for ((artistId, score) in relatedScore) {
            artistScore[artistId] = artistScore[artistId]!! + score
        }
        val minScore = artistScore.values
                .filter { it > 0 }
                .min() ?: 0.0
        artistScore = artistScore
                .map { (key, value) -> key to (if (value < 0) minScore else  value) }
                .toMap()
                .toMutableMap()
        val totalScore = artistScore.values.sum()
        val choosenSongs = mutableSetOf<String>()
        var playList = mutableListOf<String>()
        val random = Random()
        printArtistScore(artistScore)

        while (choosenSongs.size < 1000) {
            var p = random.nextDouble() * totalScore
            var choosenArtist = 0
            for ((artistId, score) in artistScore) {
                choosenArtist = artistId
                p -= score
                if (p <= 0) {
                    break
                }
            }
            val artistSongs = songMap[choosenArtist]!!
            val ind = chooseSong(artistSongs, random) ?: continue
            val songId = artistSongs[ind].googleId
            artistSongs.removeAt(ind)
            if (!choosenSongs.contains(songId)) {
                choosenSongs.add(songId)
            }
        }
        val playlist =
                api.playlistApi.create(LocalDate.now().toString(), "", Playlist.PlaylistShareState.PRIVATE)
        var choosenSongList = choosenSongs.toMutableList()
        shuffleList(choosenSongList, random)
        choosenSongList = choosenSongList.subList(0, 300)
        api.playlistApi.addTracksToPlaylist(playlist, choosenSongList.map { api.trackApi.getTrack(it) }.toList())
    }

    private fun shuffleList(list : MutableList<String>, random: Random) {
        for (i in list.indices.reversed()) {
            if (i == 0) {
                break
            }
            val ind = random.nextInt(i + 1)
            val tmp = list[ind]
            list[ind] = list[i]
            list[i] = tmp
        }
    }

    private fun chooseSong(songs : List<Song>, random: Random) :Int? {
        if (songs.isEmpty()) {
            return null
        }
        val songMap = mutableMapOf<Int, Int>()
        val known = mutableListOf<Song>()
        val unknown = mutableListOf<Song>()

        for (i in songs.indices) {
            val song = songs[i]
            songMap[song.id!!] = i
            if (song.playcount >= 10 || song.rate == 5) {
                known.add(song)
            } else {
                unknown.add(song)
            }
        }
        val score = mutableListOf<BigInteger>()
        val songs : MutableList<Song>
        if (random.nextDouble() < 0.65 && unknown.size != 0 || known.size == 0) {
            var value = BigInteger.ONE
            for (i in unknown.indices) {
                score.add(value)
                value = value.multiply(BigInteger.valueOf(2))
            }
            score.reverse()
            songs = unknown
        } else {
            for (i in known.indices) {
                score.add(BigInteger.ONE)
            }
            songs = known
        }
        val totalScore = score.fold(BigInteger.ZERO, BigInteger::add)
        var p : BigInteger
        do {
            p = BigInteger(totalScore.bitLength(), random)
        } while (p > totalScore)
        for (i in songs.indices) {
            p = p.subtract(score[i])
            if (p <= BigInteger.ZERO) {
                return songMap[songs[i].id!!]
            }
        }
        return songs.size - 1
    }

    private fun printArtistScore(artistScore : Map<Int, Double>) {
        artistScore.toList()
                .sortedBy { it.second }
                .forEach{ println("${it.first} ${it.second}") }
    }

    private fun getPlaycountHistory(periods : Int = 4) : List<Map<Int, Int>> {
        val result = mutableListOf<Map<Int, Int>>()
        var toDate= DateUtil.getDateInDays()
        val diff = 28 // 4 weeks
        for (period in 1..periods) {
            val fromDate = toDate - diff
            result.add(SongRepository.findPlayCountHistory(fromDate, toDate))
            toDate = fromDate
        }
        return result
    }

    private fun calcOwnScore(songs : List<Song>, playcountHistory : List<Map<Int, Int>>) : Double {
        var result = 0
        var playcount = 0
        var historyPlaycount = 0.0
        for (song in songs) {
            if (song.rate != 1) {
                playcount += song.playcount
                if (song.rate == 5) {
                    result += 5
                }
            } else {
                result -= 5
            }
            var i = 1.0
            for (historyMap in playcountHistory) {
                historyPlaycount += historyMap.getOrDefault(song.id!!, 0) / i
                i++
            }
        }
        return result + Math.sqrt(playcount / 4.0) + historyPlaycount
    }

}