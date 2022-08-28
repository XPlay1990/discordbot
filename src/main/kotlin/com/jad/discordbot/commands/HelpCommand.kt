package com.jad.discordbot.commands

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.stream.Collectors


@Component
class HelpCommand(private val commands: List<Command>) : Command {
    override val name: String
        get() = "help"
    override val description: String
        get() = "Help command"
    override val priority: Int
        get() = 0

    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val embeddedFields = commands.stream()
                .sorted { o1, o2 ->
                    o1.priority.compareTo(o2.priority)
                }
                .map { element -> EmbedCreateFields.Field.of(element.name, element.description, false) }
                .collect(Collectors.toList())

            val embed: EmbedCreateSpec =
                EmbedCreateSpec.builder()
                    .color(Color.YELLOW)
                    .title("Help")
                    .description("Trigger with @R2D2 command\nList of possible commands:")
                    .addAllFields(embeddedFields)
                    .image("https://sayingimages.com/wp-content/uploads/i-needs-help-help-meme.jpg")
                    .timestamp(Instant.now())
                    .footer("built by XPlay", null)
                    .build()

            messageChannel.createMessage().withEmbeds(embed).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}