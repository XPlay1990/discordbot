package com.jad.discordbot.commands.audio

import com.jad.discordbot.audio.LavaPlayerAudioProvider
import com.jad.discordbot.commands.Command
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
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
import reactor.core.publisher.Mono
import java.time.Duration


@Component
class JoinVoice(private val audioPlayer: AudioPlayer) : Command {
    override val name: String
        get() = "voice"
    override val description: String
        get() = "Joins the voice channel you are currently in"

    private var voiceConnection: VoiceConnection? = null

    override fun handle(event: MessageCreateEvent) {
        if (voiceConnection != null && voiceConnection!!.isConnected.block() == true) {
            logger.info("Disconnecting from voice")
            //disconnect if already connected
            voiceConnection!!.disconnect().block()
            return
        }

        val voiceState = event.member.get().voiceState.block()
        if (voiceState != null) {
            val channel: VoiceChannel? = voiceState.channel.block()

            if (channel != null) {
                val provider: AudioProvider = LavaPlayerAudioProvider(audioPlayer)
                val voiceChannelJoinSpec = VoiceChannelJoinSpec.create().withProvider(provider)
                channel.join(voiceChannelJoinSpec)
                    .flatMap { connection ->
                        //set connection for disconnect command
                        voiceConnection = connection

                        // The bot itself has a VoiceState; 1 VoiceState signals bot is alone
                        val voiceStateCounter: Publisher<Boolean?> = channel.voiceStates
                            .count()
                            .map { count -> 1L == count }

                        // After 10 seconds, check if the bot is alone. This is useful if
                        // the bot joined alone, but no one else joined since connecting
                        val onDelay = Mono.delay(Duration.ofSeconds(10L))
                            .filterWhen { ignored: Long? -> voiceStateCounter }
                            .switchIfEmpty(Mono.never())
                            .then()

                        // As people join and leave `channel`, check if the bot is alone.
                        // Note the first filter is not strictly necessary, but it does prevent many unnecessary cache calls
                        val onEvent = channel.client.eventDispatcher.on(VoiceStateUpdateEvent::class.java)
                            .filter { event ->
                                event.old.flatMap { obj: VoiceState -> obj.channelId }.map(channel.id::equals)
                                    .orElse(false)
                            }
                            .filterWhen { ignored -> voiceStateCounter }
                            .next()
                            .then()
                        Mono.firstWithSignal(onDelay, onEvent).then(connection.disconnect())
                    }
                    .subscribe()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}