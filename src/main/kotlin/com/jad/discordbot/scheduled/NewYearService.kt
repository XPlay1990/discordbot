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

//provides picture of the day from NASA
@Component
class NewYearService(private val botUtils: BotUtils) {
    @Value("\${newYear.picture}")
    private val pictureUrl: String = ""

    @Scheduled(cron = "\${newYear.cron}", zone = "Europe/Berlin")
    @Retryable(value = [Exception::class], maxAttempts = 2, backoff = Backoff(delay = 1000))
    fun postNewYearMessage() {
        logger.info("Posting new year message")
        logger.info("Current year: ${Year.now().value}, last year: ${Year.now().value - 1}")

        val botChannel = botUtils.getMainChannel()

        val embed: EmbedCreateSpec = EmbedCreateSpec.builder().title("A wild 2024 appears!").description(
                "Happy new Year!"
            ).footer("Frohes neues Jahr euch allen!\n Jan", null).image(pictureUrl).build()

        botChannel.createMessage().withEmbeds(embed).block()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}