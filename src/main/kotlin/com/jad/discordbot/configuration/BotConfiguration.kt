package com.jad.discordbot.configuration

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.gateway.ShardInfo
import discord4j.rest.RestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfiguration {
    @Value("\${discord.botToken}")
    private val botToken: String = ""

    private var gatewayDiscordClient: GatewayDiscordClient? = null

    @Bean
    fun gatewayDiscordClient(): GatewayDiscordClient {
        if (gatewayDiscordClient != null) {
            return gatewayDiscordClient as GatewayDiscordClient
        }

        gatewayDiscordClient = DiscordClientBuilder.create(botToken).build()
            .gateway()
            .setInitialPresence { _: ShardInfo? ->
                ClientPresence.online(
                    ClientActivity.listening("@R2D2 help")
                )
            }
            .login()
            .block()!!

//        val guildsList = (gatewayDiscordClient as GatewayDiscordClient).guilds.collect(Collectors.toList()).block()
//        guildsList!!.map { guild: Guild ->
//            logger.info("---------------- ${guild.name} -------------------")
//            val channels = guild.channels.collect(Collectors.toList()).block()
//            channels?.map { channel: GuildChannel -> logger.info("${channel.type.name} ${channel.name} - ${channel.id.asString()}") }
//        }
        return gatewayDiscordClient as GatewayDiscordClient
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        const val DEFAULT_VOICE_CHANNEL_ID = "709058379628150835"
    }
}