package com.jad.discordbot.commands

import com.fasterxml.jackson.databind.JsonNode
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient


@Component
class OpenAIImageGeneration : Command {
    override val name: String
        get() = "createimage"
    override val description: String
        get() = "returns an AI Generated Image"
    override val priority: Int
        get() = 5

    @Value("\${openai.api}")
    private val openAIUrl: String = ""

    @Value("\${openai.apikey}")
    private val openAIKey: String = ""

    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()
        val content: String = event.message.content
        val prompt: String = content.split(" ").drop(2).joinToString(" ")

        if (messageChannel == null) {
            logger.warn("No Channel found for Meme post")
            return
        }

        try {
            val imageUrl = getImage(prompt)

            if (imageUrl == null) {
                messageChannel.createMessage("Error while creating Image.\n\n Please try again.").subscribe()
                return
            }

            val embed: EmbedCreateSpec = EmbedCreateSpec.builder().title(prompt).image(imageUrl).build()

            messageChannel.createMessage().withEmbeds(embed).subscribe()
        } catch (e: Exception) {
            logger.error("Error while creating Image $prompt", e)
            messageChannel.createMessage("Error while creating Image: ${prompt}.\n\n ${e.message}").subscribe()
        }
    }

    private fun getImage(prompt: String): String? {
        val jsonFlux = WebClient.create().post().uri(openAIUrl).header("Authorization", "Bearer $openAIKey")
            .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(MessageBody(prompt))).retrieve()
            .onStatus(HttpStatus.BAD_REQUEST::equals) { response: ClientResponse ->
                response.bodyToMono(String::class.java).map { IllegalStateException(it) }
            }.bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        return jsonResponse?.get("data")?.get(0)?.get("url")?.asText()
    }

    private class MessageBody(val prompt: String, val n: Int = 1, val size: String = "1024x1024")

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}