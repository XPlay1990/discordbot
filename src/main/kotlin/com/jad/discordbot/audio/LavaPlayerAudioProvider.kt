package com.jad.discordbot.audio

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import discord4j.voice.AudioProvider
import java.nio.ByteBuffer

class LavaPlayerAudioProvider(player: AudioPlayer) :
    AudioProvider(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())) {
    private val player: AudioPlayer
    private val frame: MutableAudioFrame = MutableAudioFrame()

    init {
        // Allocate a ByteBuffer for Discord4J's AudioProvider to hold audio data for Discord
        // Set LavaPlayer's AudioFrame to use the same buffer as Discord4J's
        frame.setBuffer(buffer)
        this.player = player
    }

    override fun provide(): Boolean {
        // AudioPlayer writes audio data to the AudioFrame
        val didProvide: Boolean = player.provide(frame)
        if (didProvide) {
            buffer.flip()
        }
        return didProvide
    }
}