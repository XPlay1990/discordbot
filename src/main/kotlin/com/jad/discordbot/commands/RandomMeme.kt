package com.jad.discordbot.commands

import com.fasterxml.jackson.databind.JsonNode
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class RandomMeme : Command {
    override val commandList: Array<String>
        get() = arrayOf("meme")
    override val description: String
        get() = "returns random meme"
    override val priority: Int
        get() = 5

    @Value("\${meme.api}")
    private val memeUrl: String = ""

    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()

        if (messageChannel == null) {
            logger.warn("No Channel found for Meme post")
            return
        }

        sendMeme(messageChannel)
    }

    fun sendMeme(messageChannel: MessageChannel) {
        val jsonFlux = WebClient.create().get().uri(memeUrl).retrieve().bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val embed: EmbedCreateSpec = EmbedCreateSpec.builder().title(jsonResponse!!.get("title")!!.asText())
            .image(jsonResponse.get("url")!!.asText()).build()

        messageChannel.createMessage().withEmbeds(embed).subscribe()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}