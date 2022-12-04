package com.jad.discordbot

import com.fasterxml.jackson.databind.JsonNode
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

//provides picture of the day from NASA
@Component
class NasaAPODService(private val gatewayDiscordClient: GatewayDiscordClient) {
    @Value("\${nasa.api}")
    private val nasaUrl: String = ""

    @Value("\${nasa.apikey}")
    private val apiKey: String = ""

    @Value("\${bot.channelId}")
    private val botChannelId: String = ""

    // Every day at 23:00
    @Scheduled(cron = "0 0 23 * * *")
    fun getPictureOfTheDay() {
        logger.info("Posting Picture of the Day")
        //fetch picture of the day from NASA
        //post picture of the day to discord
        val jsonFlux = WebClient.create().get().uri(nasaUrl + apiKey).retrieve().bodyToFlux(JsonNode::class.java)

        val jsonResponse = jsonFlux.blockLast()

        val botChannel = getBotChannel()
        botChannel.createMessage(
            "${jsonResponse!!.get("title").asText()!!}\n" + "NASA Picture of the day\n" + jsonResponse.get("url")
                .asText()!!.replace("/embed/", "/watch?v=").replace("?rel=0", "")
        ).subscribe()
    }

    private fun getBotChannel(): MessageChannel {
        return gatewayDiscordClient.getChannelById(Snowflake.of(botChannelId)).block()!! as MessageChannel
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}