package com.jad.discordbot.commands.openai

import com.fasterxml.jackson.databind.JsonNode
import com.jad.discordbot.commands.Command
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateFields
import discord4j.core.spec.MessageEditSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import java.io.*


@Component
class TextCompletion(
    @Value("\${openai.api.baseUrl}") private val openAIUrl: String,

    @Value("\${openai.chat.model}") private val openAIModel: String,

    @Value("\${openai.apikey}") private val openAIKey: String,

    @Value("\${openai.badwords}") private val badWords: List<String>,

    @Value("\${resources.images.path}") private val imagePath: String
) : Command {
    override val commandList: Array<String>
        get() = arrayOf("chat", "c")
    override val description: String
        get() = "Chat with GPT-3.5"
    override val priority: Int
        get() = 5

    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()
        val content: String = event.message.content
        val prompt: String = content.split(" ").drop(2).joinToString(" ")
        logger.info("Creating chat response for Prompt: $prompt")

        if (messageChannel == null) {
            logger.warn("No Channel found for Meme post")
            return
        }

        for (badWord in badWords) {
            if (prompt.lowercase().contains(badWord)) {
                val filesToUpload = mutableListOf<MessageCreateFields.File>()
                filesToUpload.add(
                    MessageCreateFields.File.of(
                        "busted.jpg", File("$imagePath/busted.jpg").inputStream()
                    )
                )
                messageChannel.createMessage("No Bad Words Allowed!").withFiles(filesToUpload).block()
                return
            }
        }

        val chatMessage = messageChannel.createMessage("...").block()!!

        try {
            val webClient = WebClient.create()

            val responseList = getResponseMessage(webClient, prompt)

            if (responseList.isEmpty()) {
                val updatedMessage =
                    MessageEditSpec.builder().contentOrNull("Error while creating Chatmessage.\n\n Please try again.")
                        .build()
                chatMessage.edit(updatedMessage).block()
                return
            }

            val updatedMessage =
                MessageEditSpec.builder().contentOrNull(responseList.joinToString("\n") + "\n\n // Powered by $openAIModel").build()
            chatMessage.edit(updatedMessage).block()
        } catch (e: Exception) {
            logger.warn("Error while creating chat message for $prompt")
            try {
                val updatedMessage = MessageEditSpec.builder()
                    .contentOrNull("Error while creating chat message for: ${prompt}.\n\n ${e.message}").build()
                chatMessage.edit(updatedMessage).block()
            } catch (e: Exception) {
                logger.warn("Error while updating chat Message")
            }
        }
    }

    private fun getResponseMessage(webClient: WebClient, prompt: String): ArrayList<String> {
        val message = Message(prompt)
        val messageBody = MessageBody(listOf(message), openAIModel)

        logger.warn("message.role")
        logger.warn(message.role)

        val jsonFlux = webClient.post().uri("$openAIUrl/chat/completions").header("Authorization", "Bearer $openAIKey")
            .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(messageBody)).retrieve()
            .onStatus({ statusCode: HttpStatusCode -> statusCode.isError }) { response: ClientResponse ->
                response.bodyToMono(String::class.java).map { IllegalStateException(it) }
            }.bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val responseList = ArrayList<String>()
        jsonResponse?.get("choices")?.forEachIndexed { index, dataElement ->
            val chatResponse = dataElement.get("message")?.get("content")?.asText()
            if (chatResponse != null) {
                responseList.add(chatResponse)
            }
        }
        return responseList
    }


    private class MessageBody(val messages: List<Message>, val model: String)
    private class Message(val content: String, val role: String = "user")
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}