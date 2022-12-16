package com.jad.discordbot.commands.persons

import com.jad.discordbot.commands.Command
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateFields
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

@Component
class FildonS4 : Command {

    @Value("\${resources.images.path}")
    private val imagePath: String = ""
    override val name: String
        get() = "fildon"
    override val description: String
        get() = "it's fildon"
    override val priority: Int
        get() = 999


    override fun handle(event: MessageCreateEvent) {
        val fildonPic = ResourceUtils.getFile("$imagePath/Fildon.jpg")
        val embed: EmbedCreateSpec = EmbedCreateSpec.builder().image("attachment://Fildon.png").build()

        val file = MessageCreateFields.File.of("Fildon.png", fildonPic.inputStream())

        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage().withEmbeds(embed).withFiles(listOf(file)).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}