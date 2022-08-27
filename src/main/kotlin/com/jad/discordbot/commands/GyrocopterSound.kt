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
class GyrocopterSound : Command {
    override val name: String
        get() = "körbe"
    override val description: String
        get() = "DIE KÖRBEEEE, VON HIIINTEN!"


    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val pickedFile = ResourceUtils.getFile("classpath:sounds/wc3/Units/Human/GyroCopter/GyrocopterDeath1.wav")
            val resourceFileAsStream: InputStream = pickedFile.inputStream()
            val file = MessageCreateFields.File.of(pickedFile.name, resourceFileAsStream)

            messageChannel.createMessage(
                "DIE KÖRBEEEE, VON HIIINTEN!"
            ).withFiles(listOf(file)).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}