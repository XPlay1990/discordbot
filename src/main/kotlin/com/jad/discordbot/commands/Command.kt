package com.jad.discordbot.commands

import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * A simple interface defining our slash command class contract.
 * a getName() method to provide the case-sensitive name of the command.
 * and a handle() method which will house all the logic for processing each command.
 */
interface Command {
    val name: String
    val description: String
    val priority: Int
    fun handle(event: MessageCreateEvent)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}