package com.jad.discordbot.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

/**
 * A simple interface defining our slash command class contract.
 * a getName() method to provide the case-sensitive name of the command.
 * and a handle() method which will house all the logic for processing each command.
 */
interface Command {
    val commandList: Array<String>
    val description: String
    val priority: Int
    fun handle(event: MessageReceivedEvent)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}