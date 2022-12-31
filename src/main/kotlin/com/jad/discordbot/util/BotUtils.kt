package com.jad.discordbot.util

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BotUtils(private val gatewayDiscordClient: GatewayDiscordClient) {
    @Value("\${bot.channelId}")
    private val botChannelId: String = ""

    @Value("\${bot.mainChannelId}")
    private val mainChannelId: String = ""


    fun getBotChannel(): MessageChannel {
        return gatewayDiscordClient.getChannelById(Snowflake.of(botChannelId)).block()!! as MessageChannel
    }

    fun getMainChannel(): MessageChannel {
        return gatewayDiscordClient.getChannelById(Snowflake.of(mainChannelId)).block()!! as MessageChannel
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}