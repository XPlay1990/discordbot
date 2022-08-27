package com.jad.discordbot.listeners

import com.jad.discordbot.commands.Command
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Component
class CommandListener(commands: List<Command>, client: GatewayDiscordClient) {
    private val commands: Collection<Command>

    init {
        val startTime = AtomicReference<Long>()
        val botMention= "<@${client.self.block()!!.id.asString()}>"

        this.commands = commands
        client.on(MessageCreateEvent::class.java)
            .map { messageCreateEvent: MessageCreateEvent ->
                startTime.set(System.nanoTime())
                messageCreateEvent
            }
            .filter { messageCreateEvent: MessageCreateEvent ->
                messageCreateEvent.message.author.map { user -> !user.isBot }.orElse(false)
            }
            .filter { messageCreateEvent: MessageCreateEvent ->
                messageCreateEvent.message.content.startsWith(botMention)
            }
            .map { messageCreateEvent: MessageCreateEvent ->
                val foundCommand = commands.stream()
                    .filter { command: Command ->
                        messageCreateEvent.message.content.replace(
                            botMention,
                            ""
                        ).trim().lowercase().startsWith(command.name)
                    }
                    .findAny()
                    .orElse(null) ?: return@map

                logger.info("foundCommand: ${foundCommand.name}")
                foundCommand.handle(messageCreateEvent)
                logger.info("Time taken : ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime.get())} milliseconds.")
            }
            .blockLast()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}