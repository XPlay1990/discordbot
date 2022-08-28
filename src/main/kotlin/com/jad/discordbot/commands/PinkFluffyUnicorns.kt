package com.jad.discordbot.commands

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageCreateFields
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

@Component
class PinkFluffyUnicorns : Command {
    override val name: String
        get() = "fluffy"
    override val description: String
        get() = "it's fluffy"
    override val priority: Int
        get() = 1000


    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage(
                "@Rydia https://www.youtube.com/watch?v=5wb5HWVh6Fs"
            ).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}