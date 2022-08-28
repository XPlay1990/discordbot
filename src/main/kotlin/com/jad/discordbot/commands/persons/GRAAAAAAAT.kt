package com.jad.discordbot.commands.persons

import com.jad.discordbot.commands.Command
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateFields
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

@Component
class GRAAAAAAAT : Command {
    override val name: String
        get() = "garat"
    override val description: String
        get() = "it's garat"
    override val priority: Int
        get() = 1002


    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage("GRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAT\nhttps://www.youtube.com/watch?v=qMPpnCvCZvw").subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}