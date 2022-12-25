package com.jad.discordbot.configuration

import com.jad.discordbot.listeners.CommandListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class BotConfiguration(private val commandListener: CommandListener) {
    @Value("\${discord.botToken}")
    private val botToken: String = ""

    @Bean
    @Scope("singleton")
    fun gatewayDiscordClient(): JDA {
        val jda = JDABuilder.createDefault(botToken)
            .setActivity(Activity.listening("@R2D2 help"))
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .addEventListeners(commandListener)
            .build()

        jda.updateCommands().addCommands(
            Commands.slash("ping", "Calculate ping of the bot"),
        ).queue()

        return jda
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        const val DEFAULT_VOICE_CHANNEL_ID = "709058379628150835"
    }
}