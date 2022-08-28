package com.jad.discordbot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CustomAudioLoadResultHandler(private val audioPlayer: AudioPlayer, val audioTrackScheduler: AudioTrackScheduler) :
    AudioLoadResultHandler {
    private var loadStatus: LoadStatus? = null

    enum class LoadStatus {
        TrackLoaded, PlaylistLoaded, NoMatches, LoadFailed, Nothing
    }

    init {
        audioPlayer.addListener(audioTrackScheduler)
    }

    override fun trackLoaded(audioTrack: AudioTrack) {
        logger.info("track ${audioTrack.info.title} loaded")
        audioTrackScheduler.play(audioTrack, true)
        loadStatus = LoadStatus.TrackLoaded
    }

    override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
        loadStatus = if (audioPlaylist.isSearchResult) {
            audioTrackScheduler.play(audioPlaylist.tracks[0], true)
            LoadStatus.TrackLoaded
        } else {
            audioTrackScheduler.play(audioPlaylist, true)
            LoadStatus.PlaylistLoaded
        }
    }

    override fun noMatches() {
        loadStatus = LoadStatus.NoMatches
    }

    override fun loadFailed(e: FriendlyException) {
        loadStatus = LoadStatus.LoadFailed
        logger.error("Audio load failed: ${e.message}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}