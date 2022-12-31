package com.jad.discordbot.scheduled

import com.fasterxml.jackson.databind.JsonNode
import com.jad.discordbot.util.BotUtils
import discord4j.core.GatewayDiscordClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

//provides picture of the day from NASA
@Component
class NasaAPODService(private val gatewayDiscordClient: GatewayDiscordClient, private val botUtils: BotUtils) {
    @Value("\${nasa.api}")
    private val nasaUrl: String = ""

    @Value("\${nasa.apikey}")
    private val apiKey: String = ""

    // Every day at 23:30
    @Scheduled(cron = "\${nasa.cron}", zone = "Europe/Berlin")
    @Retryable(value = [Exception::class], maxAttempts = 2, backoff = Backoff(delay = 1000))
    fun getPictureOfTheDay() {
        logger.info("Posting Picture of the Day")
        //fetch picture of the day from NASA
        val jsonFlux = WebClient.create().get().uri(nasaUrl + apiKey).retrieve().bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()
        val url = getUrlFromRequest(jsonResponse)

        //post picture of the day to discord
        val botChannel = botUtils.getBotChannel()
        botChannel.createMessage(
            "NASA Picture of the day\n\n${jsonResponse!!.get("title").asText()!!}\n" + "\n${
                jsonResponse.get("explanation").asText()!!
            }\n$url"
        ).subscribe()
    }

    private fun getUrlFromRequest(jsonResponse: JsonNode?): String {
        var url = jsonResponse!!.get("hdurl")?.asText()
        if (url == null) {
            url = jsonResponse.get("url").asText()!!
        }
        // replace embedding if it is a YouTube video
        url = url.replace(
            "/embed/", "/watch?v="
        ).replace("?rel=0", "")
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}