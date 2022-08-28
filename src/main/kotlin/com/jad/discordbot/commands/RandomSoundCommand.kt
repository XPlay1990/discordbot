package com.jad.discordbot.commands

import com.jad.discordbot.util.RandomFileSelector
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
            val pickedFile = RandomFileSelector.getRandomSoundFile()
            val file = MessageCreateFields.File.of(pickedFile.name, pickedFile.inputStream())

            messageChannel.createMessage(
                "Random WC3 Sound:"
            ).withFiles(listOf(file)).subscribe()
        }
    }
}