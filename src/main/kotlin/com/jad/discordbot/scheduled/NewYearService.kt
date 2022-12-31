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

        val botChannel = botUtils.getMainChannel()

        val embed: EmbedCreateSpec = EmbedCreateSpec.builder()
            .title("A wild ${Year.now().value} appears!")
            .description(
                "${Year.now().value - 1} ist vorbei - Zeit für ein neues Jahr!" +
                        "\n Ich hoffe ihr blickt auf ein erfolgreiches und spannendes Jahr zurück und genießt die Zeit mit euren Liebsten." +
                        "\n\n Sogar Ash ist mittlerweile Pokemon Meister - also gibt es im neuen Jahr keine Ausreden. " +
                        "\n Erfüllt euch eure Träume und Wünsche und habt eine gute Zeit dabei :)"
            )
            .footer("Frohes neues Jahr euch allen!\n Jan", null)
            .image(pictureUrl)
            .build()

        botChannel.createMessage().withEmbeds(embed).block()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}