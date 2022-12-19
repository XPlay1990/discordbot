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
    init {
        val startTime = AtomicReference<Long>()
        val botMention = "<@${client.self.block()!!.id.asString()}>"

        client.on(MessageCreateEvent::class.java).map { messageCreateEvent: MessageCreateEvent ->
            startTime.set(System.nanoTime())
            messageCreateEvent
        }.filter { messageCreateEvent: MessageCreateEvent ->
            messageCreateEvent.message.author.map { user -> !user.isBot }.orElse(false)
        }.filter { messageCreateEvent: MessageCreateEvent ->
            messageCreateEvent.message.content.startsWith(botMention)
        }.map { messageCreateEvent: MessageCreateEvent ->
            val foundCommand = commands.stream().filter { command: Command ->
                command.commandList.contains(
                    messageCreateEvent.message.content.replace(
                        botMention, ""
                    ).trim().lowercase().split(" ")[0]
                )
            }.findAny().orElse(null) ?: return@map

            logger.info("foundCommand: ${foundCommand.commandList}")
            foundCommand.handle(messageCreateEvent)
            logger.info("Time taken : ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime.get())} milliseconds.")
        }.subscribe()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}