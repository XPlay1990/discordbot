package com.jad.discordbot.scheduled

import com.jad.discordbot.util.BotUtils
import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import tools.jackson.databind.node.ObjectNode
import java.time.Duration

//provides picture of the day from NASA
@Component
class NasaAPODService(
    private val botUtils: BotUtils,
    @param:Value("\${nasa.api}") private val nasaUrl: String,
    @param:Value("\${nasa.apikey}") private val apiKey: String,
    @param:Value("\${nasa.request-timeout:PT30S}") private val requestTimeout: Duration = Duration.ofSeconds(30),
    @param:Value("\${nasa.retry-delay:PT2S}") private val retryDelay: Duration = Duration.ofSeconds(2)
) {
    init {
        require(!requestTimeout.isNegative && !requestTimeout.isZero) { "NASA request timeout must be positive" }
        require(!retryDelay.isNegative && !retryDelay.isZero) { "NASA retry delay must be positive" }
    }

    private val webClient = WebClient.builder()
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT.toMillis().toInt())
                    .responseTimeout(requestTimeout)
            )
        )
        .build()

    // Every day at 23:30
    @Scheduled(cron = "\${nasa.cron}", zone = "Europe/Berlin")
    fun getPictureOfTheDay() {
        logger.info("Posting Picture of the Day")
        val jsonResponse = try {
            webClient.get()
                .uri(nasaUrl + apiKey)
                .retrieve()
                .bodyToMono<ObjectNode>()
                .switchIfEmpty(Mono.error(IllegalStateException("NASA returned an empty APOD response")))
                .timeout(requestTimeout)
                .retryWhen(
                    Retry.fixedDelay(MAX_RETRIES, retryDelay)
                        .doBeforeRetry { signal ->
                            logger.warn(
                                "NASA APOD fetch failed ({}); retry {}/{}",
                                describeFailure(signal.failure()), signal.totalRetries() + 1, MAX_RETRIES
                            )
                        }
                        .onRetryExhaustedThrow { _, signal -> signal.failure() }
                )
                // Each attempt has its own deadline; an outer 30-second wait would cancel the retries.
                .block()
                ?: throw IllegalStateException("NASA returned an empty APOD response")
        } catch (exception: Exception) {
            // HTTP exception messages can contain the request URL, including the API key.
            logger.error("Unable to fetch NASA Picture of the Day ({})", describeFailure(exception))
            return
        }

        try {
            val title = jsonResponse.get("title")?.asString()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("NASA APOD response does not contain a title")
            val explanation = jsonResponse.get("explanation")?.asString()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("NASA APOD response does not contain an explanation")
            val url = getUrlFromRequest(jsonResponse)

            botUtils.getBotChannel().createMessage(
                "NASA Picture of the day\n\n$title\n\n$explanation\n$url"
            ).block(DISCORD_TIMEOUT)
            logger.info("Posted NASA Picture of the Day")
        } catch (exception: Exception) {
            logger.error("Unable to post NASA Picture of the Day", exception)
        }
    }

    private fun describeFailure(exception: Throwable): String = when (val failure = Exceptions.unwrap(exception)) {
        is WebClientResponseException -> "HTTP ${failure.statusCode.value()}"
        is WebClientRequestException -> failure.cause?.javaClass?.simpleName ?: failure.javaClass.simpleName
        else -> failure.javaClass.simpleName
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
        private val DISCORD_TIMEOUT: Duration = Duration.ofSeconds(30)
        private const val MAX_RETRIES = 2L
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
