package com.jad.discordbot.commands

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageCreateFields
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

@Component
class HelpCommand(private val commands: List<Command>) : Command {
    override val name: String
        get() = "help"
    override val description: String
        get() = "Helping Hand"

    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage(
                "Type \"@JanBot command\" " +
                        "\r\nPossible Commands: \r\n${
                            commands.stream().map { command -> "${command.name} (${command.description})" }
                                .collect(Collectors.toList()).joinToString("\r\n")
                        }"
            ).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}