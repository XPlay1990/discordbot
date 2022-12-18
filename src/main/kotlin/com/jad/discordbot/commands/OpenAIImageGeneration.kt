package com.jad.discordbot.commands

import com.fasterxml.jackson.databind.JsonNode
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
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream


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

    @Value("\${openai.imageCount}")
    private val imageCount: Int = 1
    override fun handle(event: MessageCreateEvent) {
        val messageChannel = event.message.channel.block()
        val content: String = event.message.content
        val prompt: String = content.split(" ").drop(2).joinToString(" ")

        if (messageChannel == null) {
            logger.warn("No Channel found for Meme post")
            return
        }

        val imageMessage = messageChannel.createMessage("Creating Image for \"$prompt\"...").block()!!

        try {
            val webClient = WebClient.create()

            val imageUrlList = getImageUrls(webClient, prompt)

            if (imageUrlList.isEmpty()) {
                val updatedMessage =
                    MessageEditSpec.builder().contentOrNull("Error while creating Image.\n\n Please try again.").build()
                imageMessage.edit(updatedMessage).subscribe()
                return
            }

            val embeddingFields = mutableListOf<MessageCreateFields.File>()
            imageUrlList.forEachIndexed { index, imageUrl ->
                logger.info("Downloading Image $imageUrl")
                val imageInputStream = getImageAsInputStream(imageUrl)
                if (imageInputStream != null) {
                    embeddingFields.add(
                        MessageCreateFields.File.of(
                            "$index.jpg", imageInputStream
                        )
                    )
                }
            }

            val updatedMessage = MessageEditSpec.builder().contentOrNull(prompt).build().withFiles(embeddingFields)
            imageMessage.edit(updatedMessage).subscribe()
        } catch (e: Exception) {
            logger.error("Error while creating Image $prompt", e)
            val updatedMessage =
                MessageEditSpec.builder().contentOrNull("Error while creating Image: ${prompt}.\n\n ${e.message}")
                    .build()
            imageMessage.edit(updatedMessage).subscribe()
        }
    }

    private fun getImageUrls(webClient: WebClient, prompt: String): ArrayList<String> {
        val jsonFlux = webClient.post().uri(openAIUrl).header("Authorization", "Bearer $openAIKey")
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

    private class MessageBody(val prompt: String, val n: Int, val size: String = "1024x1024")

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}