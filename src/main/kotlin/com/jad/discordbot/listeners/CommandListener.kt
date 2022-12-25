package com.jad.discordbot.listeners

import com.jad.discordbot.commands.Command
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class CommandListener(commands: List<Command>) : ListenerAdapter() {
//    val botMention = "<@${discordClient.selfUser.id}>"

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isFromType(ChannelType.PRIVATE))
        {
            System.out.printf("[PM] %s: %s\n", event.author.name,
                event.message.contentDisplay
            )
        }
        else
        {
            System.out.printf("[%s][%s] %s: %s\n", event.guild.name,
                event.channel.name, event.member?.effectiveName,
                event.message.contentDisplay
            )
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "ping" -> {
                val time = System.currentTimeMillis()
                event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                    .flatMap { v ->
                        event.hook.editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)
                    } // then edit original
                    .queue() // Queue both reply and edit
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}