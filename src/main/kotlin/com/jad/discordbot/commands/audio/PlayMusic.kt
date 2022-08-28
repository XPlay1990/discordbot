package com.jad.discordbot.commands.audio

import com.jad.discordbot.audio.CustomAudioLoadResultHandler
import com.jad.discordbot.audio.LavaPlayerAudioProvider
import com.jad.discordbot.commands.Command
import com.jad.discordbot.util.RandomFileSelector
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import java.time.Duration

@Component
class PlayMusic(
    private val audioPlayer: AudioPlayer,
    private val audioPlayerManager: AudioPlayerManager,
    private val customAudioLoadResultHandler: CustomAudioLoadResultHandler
) : Command {
    private val subCommands = listOf("next", "list", "tod", "vier", "körbe", "random", "fluffy", "balls")
    override val name: String
        get() = "play"
    override val description: String
        get() = "Plays the Audio for the provided Link\n@JanBot play https://www.youtube.com/watch?v=dQw4w9WgXcQ\n\nSubcommands:\n${
            subCommands.joinToString(
                " "
            )
        }\n@JanBot play next"

    private var voiceConnection: VoiceConnection? = null

    override fun handle(event: MessageCreateEvent) {
        val content: String = event.message.content
        val command: List<String> = content.split(" ")

        if (command[2] == "stop") {
            logger.info("Stopping current audio")
            audioPlayer.stopTrack()
            return
        }

        joinVoice(event)

        if (command[2] == "next") {
            customAudioLoadResultHandler.audioTrackScheduler.nextTrack()
            return
        }

        if (command[2] == "list") {
            event.message.channel.block()!!.createMessage(
                customAudioLoadResultHandler.audioTrackScheduler.describePlayList()
            ).subscribe()
            return
        }

        if (command[2] == "tod") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("classpath:sounds/wc3/Units/Human/Peasant/PeasantYesAttack4.wav").path,
                customAudioLoadResultHandler
            )
            return
        }
        if (command[2] == "vier") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("classpath:sounds/wc3/Units/Critters/VillagerKid/VillagerCWhat3.wav").path,
                customAudioLoadResultHandler
            )
            return
        }
        if (command[2] == "körbe") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("classpath:sounds/wc3/Units/Human/GyroCopter/GyrocopterDeath1.wav").path,
                customAudioLoadResultHandler
            )
            return
        }
        if (command[2] == "random") {
            val randomSoundFile = RandomFileSelector.getRandomSoundFile()
            audioPlayerManager.loadItem(randomSoundFile.path, customAudioLoadResultHandler)
            return
        }
        if (command[2] == "fluffy") {
            audioPlayerManager.loadItem("https://www.youtube.com/watch?v=5wb5HWVh6Fs", customAudioLoadResultHandler)
            return
        }
        if (command[2] == "balls") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("classpath:sounds/Duke_Nukem/I've Got Balls of Steel 2.mp3").path,
                customAudioLoadResultHandler
            )
            return
        }


        audioPlayerManager.loadItem(command[2], customAudioLoadResultHandler)
    }

    private fun joinVoice(event: MessageCreateEvent) {
        val voiceState = event.member.get().voiceState.block()

        if (voiceState == null) {
            event.message.channel.block()!!.createMessage("You have to enter a Voice channel so I can follow!")
                .subscribe()
            return
        }

        val channel: VoiceChannel? = voiceState.channel.block()

        if (channel != null) {
            val provider: AudioProvider = LavaPlayerAudioProvider(audioPlayer)
            val voiceChannelJoinSpec = VoiceChannelJoinSpec.create().withProvider(provider)
            channel.join(voiceChannelJoinSpec).flatMap { connection ->
                //set connection for disconnect command
                voiceConnection = connection

                // The bot itself has a VoiceState; 1 VoiceState signals bot is alone
                val voiceStateCounter: Publisher<Boolean?> =
                    channel.voiceStates.count().map { count -> 1L == count }

                // After 10 seconds, check if the bot is alone. This is useful if
                // the bot joined alone, but no one else joined since connecting
                val onDelay = Mono.delay(Duration.ofSeconds(10L)).filterWhen { ignored: Long? -> voiceStateCounter }
                    .switchIfEmpty(Mono.never()).then()

                // As people join and leave `channel`, check if the bot is alone.
                // Note the first filter is not strictly necessary, but it does prevent many unnecessary cache calls
                val onEvent = channel.client.eventDispatcher.on(VoiceStateUpdateEvent::class.java).filter { event ->
                    event.old.flatMap { obj: VoiceState -> obj.channelId }.map(channel.id::equals).orElse(false)
                }.filterWhen { ignored -> voiceStateCounter }.next().then()
                Mono.firstWithSignal(onDelay, onEvent).then(connection.disconnect())
            }.subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}