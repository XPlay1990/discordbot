package com.jad.discordbot.commands

import com.fasterxml.jackson.databind.JsonNode
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

@Component
class RandomMeme : Command {
    override val name: String
        get() = "meme"
    override val description: String
        get() = "returns random meme"
    override val priority: Int
        get() = 5


    override fun handle(event: MessageCreateEvent) {
        val jsonFlux = WebClient.create()
            .get()
            .uri("https://meme-api.herokuapp.com/gimme")
            .retrieve()
            .bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val embed: EmbedCreateSpec =
            EmbedCreateSpec.builder()
                .title(jsonResponse!!.get("title")!!.asText())
                .image(jsonResponse.get("url")!!.asText())
                .build()

        event.message.channel.subscribe { messageChannel: MessageChannel ->
            messageChannel.createMessage().withEmbeds(embed).subscribe()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}