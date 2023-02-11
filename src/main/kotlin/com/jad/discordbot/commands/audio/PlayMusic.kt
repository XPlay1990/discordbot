package com.jad.discordbot.commands.audio

import com.jad.discordbot.audio.CustomAudioLoadResultHandler
import com.jad.discordbot.audio.LavaPlayerAudioProvider
import com.jad.discordbot.commands.Command
import com.jad.discordbot.configuration.BotConfiguration.Companion.DEFAULT_VOICE_CHANNEL_ID
import com.jad.discordbot.util.RandomFileSelector
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.VoiceChannelJoinSpec
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class PlayMusic(
    @Value("\${resources.sounds.path}") private val soundPath: String = "",
    private val audioPlayer: AudioPlayer,
    private val audioPlayerManager: AudioPlayerManager,
    private val customAudioLoadResultHandler: CustomAudioLoadResultHandler,
    private val randomFileSelector: RandomFileSelector
) : Command {
    private val subCommands =
        listOf("next", "list", "volume +100", "tot", "vier", "körbe", "random", "fluffy", "garat", "balls")
    override val commandList: Array<String>
        get() = arrayOf("play")
    override val description: String
        get() = "Plays the Audio for the provided Link\n@R2D2 play https://www.youtube.com/watch?v=dQw4w9WgXcQ\n\nSubcommands:\n${
            subCommands.joinToString(
                " | "
            )
        }\n@R2D2 play next"
    override val priority: Int
        get() = 2

    private var voiceConnection: VoiceConnection? = null

    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500))
    override fun handle(event: MessageCreateEvent) {
        val content: String = event.message.content
        val command: List<String> = content.split(" ")

        handleJoinVoiceChannel(event)

        if (command[2] == "list") {
            event.message.channel.block()!!.createMessage(
                customAudioLoadResultHandler.audioTrackScheduler.describePlayList()
            ).subscribe()
            return
        }

        if (handleSubCommands(command)) return

        logger.info("Loading audio item: ${command[2]}")
        audioPlayerManager.loadItem(command[2], customAudioLoadResultHandler)
    }

    @Async
    fun handleSubCommands(
        command: List<String>
    ): Boolean {
        if (command[2] == "stop") {
            logger.info("Stopping current audio")
            audioPlayer.stopTrack()
            customAudioLoadResultHandler.audioTrackScheduler.clear()
            return true
        }

        if (command[2] == "volume") {
            if (command.size == 4) {
                if (command[3].startsWith("+") || command[3].startsWith("-")) {
                    audioPlayer.volume += command[3].toInt()
                    return true
                }

                audioPlayer.volume = command[3].toInt()
            }
            return true
        }

        if (command[2] == "next") {
            customAudioLoadResultHandler.audioTrackScheduler.nextTrack()
            return true
        }

        if (command[2] == "tot") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("${soundPath}/wc3/Units/Human/Peasant/PeasantYesAttack4.wav").path,
                customAudioLoadResultHandler
            )
            return true
        }
        if (command[2] == "vier") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("${soundPath}/wc3/Units/Critters/VillagerKid/VillagerCWhat3.wav").path,
                customAudioLoadResultHandler
            )
            return true
        }
        if (command[2] == "körbe") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("${soundPath}/wc3/Units/Human/GyroCopter/GyrocopterDeath1.wav").path,
                customAudioLoadResultHandler
            )
            return true
        }
        if (command[2] == "random") {
            if (command.size == 4) {
                val randomSoundFileList = randomFileSelector.getRandomSoundFileList(command[3].toInt())
                randomSoundFileList.forEach { element ->
                    audioPlayerManager.loadItem(element.path, customAudioLoadResultHandler)
                }
                return true
            }

            val randomSoundFile = randomFileSelector.getRandomSoundFile()
            audioPlayerManager.loadItem(randomSoundFile.path, customAudioLoadResultHandler)
            return true
        }
        if (command[2] == "randomDE") {
            val randomSoundFile = randomFileSelector.getRandomSoundFileDE()
            audioPlayerManager.loadItem(randomSoundFile.path, customAudioLoadResultHandler)
            return true
        }
        if (command[2] == "fluffy") {
            audioPlayerManager.loadItem("https://www.youtube.com/watch?v=5wb5HWVh6Fs", customAudioLoadResultHandler)
            return true
        }
        if (command[2] == "garat") {
            audioPlayerManager.loadItem("https://www.youtube.com/watch?v=qMPpnCvCZvw", customAudioLoadResultHandler)
            return true
        }
        if (command[2] == "balls") {
            audioPlayerManager.loadItem(
                ResourceUtils.getFile("${soundPath}/Duke_Nukem/I've Got Balls of Steel 2.mp3").path,
                customAudioLoadResultHandler
            )
            return true
        }

        return false
    }

    private fun handleJoinVoiceChannel(event: MessageCreateEvent) {
        if (event.message.channel.block()?.type?.name == "DM") {
            return
        }

        val voiceState = event.member.orElse(null)?.voiceState?.block()
        if (voiceState == null) {
            joinDefaultVoiceChannel(event.client)
            return
        }

        val voiceChannel: VoiceChannel? = voiceState.channel.block()
        if (voiceChannel == null) {
            logger.info("No channel found. Not connecting.")
            return
        }
        joinVoiceChannel(voiceChannel)
        return
    }

    fun joinDefaultVoiceChannel(client: GatewayDiscordClient) {
        joinVoiceChannel(client.getChannelById(Snowflake.of(DEFAULT_VOICE_CHANNEL_ID)).block()!! as VoiceChannel)
    }

    fun joinVoiceChannel(voiceChannel: VoiceChannel) {
        val provider: AudioProvider = LavaPlayerAudioProvider(audioPlayer)
        val voiceChannelJoinSpec = VoiceChannelJoinSpec.create().withProvider(provider)
        voiceChannel.join(voiceChannelJoinSpec).flatMap { connection ->
            //set connection for disconnect command
            voiceConnection = connection

            // The bot itself has a VoiceState; 1 VoiceState signals bot is alone
            val voiceStateCounter: Publisher<Boolean?> = voiceChannel.voiceStates.count().map { count -> 1L == count }

            // After 10 seconds, check if the bot is alone. This is useful if
            // the bot joined alone, but no one else joined since connecting
            val onDelay = Mono.delay(Duration.ofSeconds(10L)).filterWhen { _: Long? -> voiceStateCounter }
                .switchIfEmpty(Mono.never()).then()

            // As people join and leave `channel`, check if the bot is alone.
            // Note the first filter is not strictly necessary, but it does prevent many unnecessary cache calls
            val onEvent = voiceChannel.client.eventDispatcher.on(VoiceStateUpdateEvent::class.java).filter { event ->
                event.old.flatMap { obj: VoiceState -> obj.channelId }.map(voiceChannel.id::equals).orElse(false)
            }.filterWhen { _ -> voiceStateCounter }.next().then()
            Mono.firstWithSignal(onDelay, onEvent).then(connection.disconnect())
        }.subscribe()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}