package com.jad.discordbot.commands.openai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
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
import java.util.*


@Component
class TextCompletion(
    @Value("\${openai.api.baseUrl}") private val openAIUrl: String,

    @Value("\${openai.chat.model}") private val openAIModel: String,

    @Value("\${openai.apikey}") private val openAIKey: String,

    @Value("\${openai.badwords}") private val badWords: List<String>,

    @Value("\${resources.images.path}") private val imagePath: String
) : Command {

    private val messageStore = HashMap<String, MutableList<StoredMessage>>()

    override val commandList: Array<String>
        get() = arrayOf("chat", "c")
    override val description: String
        get() = "Chat with OpenAI $openAIModel"
    override val priority: Int
        get() = 5

    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()
        val content: String = event.message.content
        val prompt: String = content.split(" ").drop(2).joinToString(" ")
        if (prompt == "clear") {
            messageStore.remove(event.message.author.get().id.asString())
            messageChannel?.createMessage("Chat history cleared")?.block()
            return
        }

        logger.info("Creating chat response for Prompt: $prompt")

        if (messageChannel == null) {
            logger.warn("No Channel found for Chat response")
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

            val messageStoreForUser = prepareMessageStoreForUser(event, prompt)

            val responseList = getResponseMessage(webClient, messageStoreForUser)

            if (responseList.isEmpty()) {
                val updatedMessage =
                    MessageEditSpec.builder().contentOrNull("Error while creating Chatmessage.\n\n Please try again.")
                        .build()
                chatMessage.edit(updatedMessage).block()
                return
            }

            writeMessageStoreForUser(responseList, messageStoreForUser, event)

            val responseMessage = responseList.joinToString("\n") { message -> message.content }
            val updatedMessage = MessageEditSpec.builder().contentOrNull(responseMessage).build()
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

    private fun writeMessageStoreForUser(
        responseList: ArrayList<Message>, messageStoreForUser: MutableList<StoredMessage>, event: MessageCreateEvent
    ) {
        for (message in responseList) {
            messageStoreForUser.add(StoredMessage(Date(), Message(message.content, message.role)))
        }
        messageStore[event.message.author.get().id.asString()] = messageStoreForUser
    }

    private fun prepareMessageStoreForUser(
        event: MessageCreateEvent, prompt: String
    ): MutableList<StoredMessage> {
        var messageStoreForUser = messageStore[event.message.author.get().id.asString()]
        if (messageStoreForUser == null) {
            messageStoreForUser = mutableListOf()
        }
        //check if last message is older than 5 minutes, if yes -> reset List
        if (messageStoreForUser.isNotEmpty() && messageStoreForUser.last().creationDate.time + 300000 < Date().time) {
            messageStoreForUser = mutableListOf()
        }
        messageStoreForUser.add(StoredMessage(Date(), Message(prompt)))
        return messageStoreForUser
    }

    private fun getResponseMessage(
        webClient: WebClient, storedMessageList: MutableList<StoredMessage>
    ): ArrayList<Message> {
        val messageContent = ArrayList<Message>()
        for (storedMessage in storedMessageList) {
            messageContent.add(storedMessage.message)
        }
        val messageBody = MessageBody(messageContent, openAIModel)

        logger.debug("Sending request to OpenAI: ${objectWriter.writeValueAsString(messageBody)})}")

        val jsonFlux = webClient.post().uri("$openAIUrl/chat/completions").header("Authorization", "Bearer $openAIKey")
            .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(messageBody)).retrieve()
            .onStatus({ statusCode: HttpStatusCode -> statusCode.isError }) { response: ClientResponse ->
                response.bodyToMono(String::class.java).map { IllegalStateException(it) }
            }.bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val responseList = ArrayList<Message>()
        jsonResponse?.get("choices")?.forEach { dataElement ->
            val message = dataElement.get("message")
            val role = message.get("role").asText()
            val chatResponse = message.get("content").asText()
            if (chatResponse != null) {
                responseList.add(Message(chatResponse, role))
            }
        }
        return responseList
    }


    private class MessageBody(val messages: List<Message>, val model: String)
    private class Message(val content: String, val role: String = "user")

    private class StoredMessage(val creationDate: Date, val message: Message)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        var objectWriter: ObjectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()
    }
}