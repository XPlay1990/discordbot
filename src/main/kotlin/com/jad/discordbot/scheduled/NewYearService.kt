package com.jad.discordbot.scheduled

import com.jad.discordbot.util.BotUtils
import discord4j.core.spec.EmbedCreateSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Year

//provides new years message
@Component
class NewYearService(private val botUtils: BotUtils) {
    @Value("\${newYear.title}")
    private val title: String = ""

    @Value("\${newYear.message}")
    private val message: String = ""

    @Value("\${newYear.picture}")
    private val pictureUrl: String = ""

    @Value("\${newYear.footer}")
    private val footer: String = ""

    @Scheduled(cron = "\${newYear.cron}", zone = "Europe/Berlin")
    @Retryable(value = [Exception::class], maxAttempts = 2, backoff = Backoff(delay = 1000))
    fun postNewYearMessage() {
        logger.info("Posting new year message")
        logger.info("Current year: ${Year.now().value}, last year: ${Year.now().value - 1}")

        val botChannel = botUtils.getMainChannel()

        val embed: EmbedCreateSpec = EmbedCreateSpec.builder().title(title).description(
            message
        ).footer(footer, null).image(pictureUrl).build()

        botChannel.createMessage().withEmbeds(embed).block()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}