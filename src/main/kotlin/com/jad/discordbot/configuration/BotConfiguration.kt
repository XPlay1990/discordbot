package com.jad.discordbot.configuration

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.gateway.ShardInfo
import discord4j.rest.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfiguration {
    @Value("\${discord.botToken}")
    private val botToken: String = ""

    @Bean
    fun gatewayDiscordClient(): GatewayDiscordClient {
        return DiscordClientBuilder.create(botToken).build()
            .gateway()
            .setInitialPresence { _ignore: ShardInfo? ->
                ClientPresence.online(
                    ClientActivity.listening("@JanBot help")
                )
            }
            .login()
            .block()!!
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }
}