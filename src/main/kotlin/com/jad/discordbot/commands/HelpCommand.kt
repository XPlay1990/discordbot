package com.jad.discordbot.commands

import com.jad.discordbot.commands.audio.PlayMusic
import com.jad.discordbot.configuration.BotConfiguration.Companion.DEFAULT_VOICE_CHANNEL_ID
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeoutException
import java.util.stream.Collectors


@Component
class HelpCommand(
    private val commands: List<Command>, private val playMusic: PlayMusic, private val randomMeme: RandomMeme
) : Command {
    override val commandList: Array<String>
        get() = arrayOf("help", "h")
    override val description: String
        get() = "Help command"
    override val priority: Int
        get() = 0

    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val embeddedFields = commands.stream().sorted { o1, o2 ->
                o1.priority.compareTo(o2.priority)
            }.map { element -> EmbedCreateFields.Field.of(element.commandList.joinToString(" / "), element.description, false) }
                .collect(Collectors.toList())

            val embed: EmbedCreateSpec = EmbedCreateSpec.builder().color(Color.YELLOW).title("Help")
                .description("Trigger with @R2D2 command\nList of possible commands:").addAllFields(embeddedFields)
                .image("https://sayingimages.com/wp-content/uploads/i-needs-help-help-meme.jpg")
                .timestamp(Instant.now()).footer("built by XPlay", null).build()

            val joinVoiceButton: Button = Button.danger("joinVoice", "Join voice")
            val playRandomButton: Button = Button.danger("playRandom", "Play random sound")
            val playRandomDEButton: Button = Button.danger("playRandomDE", "DE meme")
            val memeButton: Button = Button.primary("meme", "Meme")

            val tempListener: Mono<Void> = event.client.on(ButtonInteractionEvent::class.java) { event ->
                when (event.customId) {
                    "joinVoice" -> playMusic.joinVoiceChannel(
                        event.client.getChannelById(
                            Snowflake.of(
                                DEFAULT_VOICE_CHANNEL_ID
                            )
                        ).block()!! as VoiceChannel
                    )

                    "playRandom" -> playMusic.handleSubCommands(listOf("", "", "random"))
                    "playRandomDE" -> playMusic.handleSubCommands(listOf("", "", "randomDE"))
                    "meme" -> {
                        randomMeme.sendMeme(messageChannel)
                    }
                }
                return@on event.reply() // creates warning in log, "Message cannot be empty" -> Discord4j bug
            }.timeout(Duration.ofMinutes(30)) // Timeout after 30 minutes
                // Handle TimeoutException that will be thrown when the above times out
                .onErrorResume(TimeoutException::class.java) { _ -> Mono.empty() }.then() //Transform the flux to a mono

            messageChannel.createMessage().withEmbeds(embed)
                .withComponents(ActionRow.of(joinVoiceButton, playRandomButton, playRandomDEButton, memeButton))
                .then(tempListener).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}