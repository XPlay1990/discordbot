package com.jad.discordbot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.*


class AudioTrackScheduler(player: AudioPlayer) : AudioLoadResultHandler {
    private val queue: MutableList<AudioTrack> = Collections.synchronizedList(LinkedList())
    private val player: AudioPlayer

    init {
        this.player = player
    }

    override fun trackLoaded(audioTrack: AudioTrack) {
        player.playTrack(audioTrack)
    }

    override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
        queue.addAll(audioPlaylist.tracks)
    }

    override fun noMatches() {}
    override fun loadFailed(e: FriendlyException) {
        e.printStackTrace()
    }
}