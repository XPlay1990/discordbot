package com.jad.discordbot.commands.openai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jad.discordbot.commands.Command
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateFields
import discord4j.core.spec.MessageEditSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.*


@Component
class ImageGeneration(
    @Value("\${openai.api.baseUrl}") private val openAIBaseUrl: String,

    @Value("\${openai.apikey}") private val openAIKey: String,

    @Value("\${openai.imageCount}") private val imageCount: Int = 1,

    @Value("\${openai.badwords}") private val badWords: List<String>,

    @Value("\${resources.images.path}") private val imagePath: String
) : Command {
    override val commandList: Array<String>
        get() = arrayOf("createimage", "ci")
    override val description: String
        get() = "returns an AI Generated Image"
    override val priority: Int
        get() = 5

    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()
        val content: String = event.message.content
        val prompt: String = content.split(" ").drop(2).joinToString(" ")
        logger.info("Creating Images for Prompt: $prompt")

        if (messageChannel == null) {
            logger.warn("No Channel found for Image post")
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

        val imageMessage = messageChannel.createMessage("Creating Image for \"$prompt\"...").block()!!

        try {
            val webClient = WebClient.create()

            val imageUrlList = getImageUrls(webClient, prompt)

            if (imageUrlList.isEmpty()) {
                val updatedMessage =
                    MessageEditSpec.builder().contentOrNull("Error while creating Image.\n\n Please try again.").build()
                imageMessage.edit(updatedMessage).block()
                return
            }
            var updatedMessage =
                MessageEditSpec.builder().contentOrNull(prompt + "\n\n" + imageUrlList.joinToString("\n")).build()
            imageMessage.edit(updatedMessage).block()

            val filesToUpload = mutableListOf<MessageCreateFields.File>()
            imageUrlList.forEachIndexed { index, imageUrl ->
                logger.info("Downloading Image $imageUrl")
                val imageInputStream = getImageAsInputStream(imageUrl)
                if (imageInputStream != null) {
                    filesToUpload.add(
                        MessageCreateFields.File.of(
                            "$index.jpg", imageInputStream
                        )
                    )
                }
            }

            updatedMessage = MessageEditSpec.builder().contentOrNull(prompt).build().withFiles(filesToUpload)
            imageMessage.edit(updatedMessage).block()
        } catch (e: Exception) {
            logger.warn("Error while creating Image $prompt")
            try {
                val updatedMessage =
                    MessageEditSpec.builder().contentOrNull("Error while creating Image: ${prompt}.\n\n ${e.message}")
                        .build()
                imageMessage.edit(updatedMessage).block()
            } catch (e: Exception) {
                logger.warn("Error while updating Image Message")
            }
        }
    }

    private fun getImageUrls(webClient: WebClient, prompt: String): ArrayList<String> {
        val jsonFlux =
            webClient.post().uri("$openAIBaseUrl/images/generations").header("Authorization", "Bearer $openAIKey")
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(MessageBody(prompt, imageCount)))
                .retrieve().onStatus({ statusCode: HttpStatusCode -> statusCode.isError }) { response: ClientResponse ->
                    response.bodyToMono(String::class.java).map { IllegalStateException(it) }
                }.bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val imageLinks = ArrayList<String>()
        jsonResponse?.get("data")?.forEach { dataElement ->
            val url = dataElement.get("url")?.asText()
            if (url != null) {
                imageLinks.add(url)
            }
        }
        return imageLinks
    }

    fun getImageAsInputStream(url: String): InputStream? {
        val factory = DefaultUriBuilderFactory()
        factory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        val webClient = WebClient.builder().uriBuilderFactory(factory).build()


        val body: Flux<DataBuffer> = webClient.get().uri(url)
            .exchangeToFlux { clientResponse: ClientResponse -> clientResponse.body(BodyExtractors.toDataBuffers()) }
            .doOnError(IOException::class.java) { e: IOException -> logger.error("Error while downloading Image", e) }
            .doOnCancel { logger.info("Image-Download cancelled") }.retry(3)

        return imageRequestToInputStream(body)
    }

    private fun imageRequestToInputStream(body: Flux<DataBuffer>): PipedInputStream {
        val osPipe = PipedOutputStream()
        val isPipe = PipedInputStream(osPipe)

        DataBufferUtils.write(body, osPipe).subscribeOn(Schedulers.boundedElastic()).doOnComplete {
            try {
                osPipe.close()
            } catch (ignored: IOException) {
            }
        }.subscribe(DataBufferUtils.releaseConsumer())
        return isPipe
    }

    private class MessageBody(
        val prompt: String, val n: Int, val size: String = "1024x1024", val model: String = "dall-e-3"
    )

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val objectMapper = ObjectMapper()
    }
}