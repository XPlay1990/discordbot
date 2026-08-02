package com.jad.discordbot.configuration

import com.jad.discordbot.audio.AudioTrackScheduler
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import dev.lavalink.youtube.YoutubeAudioSourceManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.atomic.AtomicBoolean

@Configuration
class AudioConfiguration {
    private val playerManager = DefaultAudioPlayerManager()
    private val player: AudioPlayer = playerManager.createPlayer()
    private val audioTrackScheduler: AudioTrackScheduler = AudioTrackScheduler(player)

    init {
        player.addListener(audioTrackScheduler)
    }

    @Bean
    fun createAudioPlayerManager(): AudioPlayerManager {
        // This is an optimization strategy that Discord4J can utilize.
        // It is not important to understand
        playerManager.configuration.frameBufferFactory =
            AudioFrameBufferFactory { bufferDuration: Int, format: AudioDataFormat?, stopping: AtomicBoolean? ->
                NonAllocatingAudioFrameBuffer(
                    bufferDuration, format, stopping
                )
            }

        // Register maintained remote sources. The deprecated built-in YouTube
        // source is excluded in favour of Lavalink's dedicated implementation.
        AudioSourceManagers.registerRemoteSources(
            playerManager,
            com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java
        )
        playerManager.registerSourceManager(YoutubeAudioSourceManager())
        AudioSourceManagers.registerLocalSource(playerManager)

        return playerManager
    }

    @Bean
    fun createAudioPlayer(): AudioPlayer {
        // Create an AudioPlayer so Discord4J can receive audio data
        return player
    }

    @Bean
    fun createAudioTrackScheduler(): AudioTrackScheduler {
        // Create an AudioPlayer so Discord4J can receive audio data
        return audioTrackScheduler
    }
}
