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
class BinVierSound : Command {
    override val name: String
        get() = "vier"
    override val description: String
        get() = "4"


    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val pickedFile = ResourceUtils.getFile("classpath:sounds/wc3/Units/Critters/VillagerKid/VillagerCWhat3.wav")
            val resourceFileAsStream: InputStream = pickedFile.inputStream()
            val file = MessageCreateFields.File.of(pickedFile.name, resourceFileAsStream)

            messageChannel.createMessage(
                ""
            ).withFiles(listOf(file)).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}