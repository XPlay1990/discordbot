package com.jad.discordbot.commands.audio

import com.jad.discordbot.audio.AudioTrackScheduler
import com.jad.discordbot.commands.Command
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.voice.VoiceConnection
import org.springframework.stereotype.Component

@Component
class PlayMusic(private val audioPlayer: AudioPlayer, private val audioPlayerManager: AudioPlayerManager) : Command {
    override val name: String
        get() = "play"
    override val description: String
        get() = "Plays the Audio for the provided Link"

    private var voiceConnection: VoiceConnection? = null

    override fun handle(event: MessageCreateEvent) {
        val scheduler = AudioTrackScheduler(audioPlayer)
        val content: String = event.message.content
        val command: List<String> = content.split(" ")
        audioPlayerManager.loadItem(command[2], AudioTrackScheduler(audioPlayer))
    }
}