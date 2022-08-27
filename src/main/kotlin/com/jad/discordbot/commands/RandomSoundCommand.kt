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
class RandomSoundCommand : Command {
    override val name: String
        get() = "sound"
    override val description: String
        get() = "Random WC3 Sound"

    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val soundFiles = Files.walk(
                Path.of(ClassLoader.getSystemResource("sounds/wc3/").toURI())
            )
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
            val pickedFile = File(soundFiles[Random().nextInt(soundFiles.size)].toUri())
            val resourceFileAsStream: InputStream = pickedFile.inputStream()
            val file = MessageCreateFields.File.of(pickedFile.name, resourceFileAsStream)

            messageChannel.createMessage(
                "Random WC3 Sound:"
            ).withFiles(listOf(file)).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}