package com.jad.discordbot.scheduled

import com.jad.discordbot.util.BotUtils
import io.netty.channel.ChannelOption
import reactor.netty.http.client.HttpClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.node.ObjectNode
import java.time.Duration

//provides picture of the day from NASA
@Component
class NasaAPODService(
    private val botUtils: BotUtils,
    @param:Value("\${nasa.api}") private val nasaUrl: String,
    @param:Value("\${nasa.apikey}") private val apiKey: String
) {
    private val webClient = WebClient.builder()
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT.toMillis().toInt())
                    .responseTimeout(REQUEST_TIMEOUT)
            )
        )
        .build()

    // Every day at 23:30
    @Scheduled(cron = "\${nasa.cron}", zone = "Europe/Berlin")
    fun getPictureOfTheDay() {
        logger.info("Posting Picture of the Day")
        try {
            val jsonResponse = webClient.get()
                .uri(nasaUrl + apiKey)
                .retrieve()
                .bodyToMono<ObjectNode>()
                .block(REQUEST_TIMEOUT)
                ?: throw IllegalStateException("NASA returned an empty APOD response")

            val title = jsonResponse.get("title")?.asString()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("NASA APOD response does not contain a title")
            val explanation = jsonResponse.get("explanation")?.asString()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("NASA APOD response does not contain an explanation")
            val url = getUrlFromRequest(jsonResponse)

            botUtils.getBotChannel().createMessage(
                "NASA Picture of the day\n\n$title\n\n$explanation\n$url"
            ).block(REQUEST_TIMEOUT)
        } catch (exception: Exception) {
            logger.error("Unable to post NASA Picture of the Day", exception)
        }
    }

    private fun getUrlFromRequest(jsonResponse: ObjectNode): String {
        var url = jsonResponse.get("hdurl")?.asString()?.takeIf(String::isNotBlank)
            ?: jsonResponse.get("url")?.asString()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("NASA APOD response does not contain a media URL")
        // replace embedding if it is a YouTube video
        url = url.replace(
            "/embed/", "/watch?v="
        ).replace("?rel=0", "")
        return url
    }

    companion object {
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(30)
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
