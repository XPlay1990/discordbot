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
class RandomSoundCommand(private val randomFileSelector: RandomFileSelector) : Command {
    override val commandList: Array<String>
        get() = arrayOf("random")
    override val description: String
        get() = "Random Game Sound"
    override val priority: Int
        get() = 10

    override fun handle(event: MessageCreateEvent) {
        event.message.channel.subscribe { messageChannel: MessageChannel ->
            val pickedFile = randomFileSelector.getRandomSoundFile()
            val file = MessageCreateFields.File.of(pickedFile.name, pickedFile.inputStream())

            messageChannel.createMessage(
                "Random Game Sound:"
            ).withFiles(listOf(file)).subscribe()
        }
    }
}