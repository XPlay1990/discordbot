package com.jad.discordbot.commands

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateFields
import discord4j.rest.util.Color
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

@Component
class FildonS4 : Command {
    override val name: String
        get() = "fildon"
    override val description: String
        get() = "it's fildon"
    override val priority: Int
        get() = 999


    override fun handle(event: MessageCreateEvent) {
        val fildonPic = ResourceUtils.getFile("classpath:images/Fildon.png")
        val embed: EmbedCreateSpec =
            EmbedCreateSpec.builder()
                .image("attachment://Fildon.png")
                .build()

        val file = MessageCreateFields.File.of("Fildon.png", fildonPic.inputStream())

        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage().withEmbeds(embed).withFiles(listOf(file)).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}