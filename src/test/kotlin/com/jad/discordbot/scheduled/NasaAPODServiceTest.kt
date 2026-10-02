package com.jad.discordbot.scheduled

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.jad.discordbot.util.BotUtils
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageCreateMono
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Timeout(10)
class NasaAPODServiceTest {
    @Test
    fun `Spring injects the default NASA timeout and retry delay`() {
        AnnotationConfigApplicationContext().use { context ->
            context.beanFactory.conversionService = ApplicationConversionService.getSharedInstance()
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "test-nasa", mapOf("nasa.api" to "https://example.com/apod?api_key=", "nasa.apikey" to TEST_API_KEY)
                )
            )
            context.beanFactory.registerSingleton("botUtils", mock(BotUtils::class.java))
            context.register(NasaAPODService::class.java)
            context.refresh()

            val service = context.getBean(NasaAPODService::class.java)
            assertEquals(Duration.ofSeconds(30), ReflectionTestUtils.getField(service, "requestTimeout"))
            assertEquals(Duration.ofSeconds(2), ReflectionTestUtils.getField(service, "retryDelay"))
        }
    }

    @Test
    fun `a timed out NASA request is retried and posts the recovered response once`() {
        withNasaServer({ attempt, response ->
            if (attempt == 1) Mono.never<Void>() else successfulResponse(response)
        }) { fixture ->
            fixture.service.getPictureOfTheDay()

            assertEquals(2, fixture.attempts.get())
            verify(fixture.channel).createMessage(EXPECTED_MESSAGE)
        }
    }

    @Test
    fun `a NASA server error is retried and posts once`() {
        withNasaServer({ attempt, response ->
            if (attempt == 1) response.status(503).send().then() else successfulResponse(response)
        }) { fixture ->
            fixture.service.getPictureOfTheDay()

            assertEquals(2, fixture.attempts.get())
            verify(fixture.channel).createMessage(EXPECTED_MESSAGE)
        }
    }

    @Test
    fun `NASA rate limiting is retried`() {
        withNasaServer({ attempt, response ->
            if (attempt == 1) response.status(429).send().then() else successfulResponse(response)
        }) { fixture ->
            fixture.service.getPictureOfTheDay()

            assertEquals(2, fixture.attempts.get())
            verify(fixture.channel).createMessage(EXPECTED_MESSAGE)
        }
    }

    @Test
    fun `all NASA fetch errors are retried without exposing the API key in logs`() {
        val logger = LoggerFactory.getLogger("${NasaAPODService::class.java.name}\$Companion") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            withNasaServer({ _, response -> response.status(403).send().then() }) { fixture ->
                fixture.service.getPictureOfTheDay()

                assertEquals(3, fixture.attempts.get())
                verifyNoInteractions(fixture.channel)
                val error = appender.list.single { it.level.toString() == "ERROR" }
                assertEquals("Unable to fetch NASA Picture of the Day (HTTP 403)", error.formattedMessage)
                assertFalse(error.formattedMessage.contains(TEST_API_KEY))
                assertEquals(null, error.throwableProxy)
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `persistent server errors stop after three attempts without posting`() {
        withNasaServer({ _, response -> response.status(503).send().then() }) { fixture ->
            fixture.service.getPictureOfTheDay()

            assertEquals(3, fixture.attempts.get())
            verifyNoInteractions(fixture.channel)
        }
    }

    @Test
    fun `persistent timeouts stop after three attempts without posting`() {
        withNasaServer({ _, _ -> Mono.never<Void>() }) { fixture ->
            fixture.service.getPictureOfTheDay()

            assertEquals(3, fixture.attempts.get())
            verifyNoInteractions(fixture.channel)
        }
    }

    @Test
    fun `a Discord send failure does not retry the fetch or the message`() {
        withNasaServer({ _, response -> successfulResponse(response) }) { fixture ->
            `when`(fixture.message.block(any(Duration::class.java)))
                .thenThrow(IllegalStateException("Discord unavailable"))

            fixture.service.getPictureOfTheDay()

            assertEquals(1, fixture.attempts.get())
            verify(fixture.channel).createMessage(EXPECTED_MESSAGE)
            verify(fixture.message).block(Duration.ofSeconds(30))
        }
    }

    private fun withNasaServer(
        respond: (Int, HttpServerResponse) -> Mono<Void>,
        check: (Fixture) -> Unit
    ) {
        val attempts = AtomicInteger()
        val server = HttpServer.create().host("127.0.0.1").port(0)
            .handle { _, response -> respond(attempts.incrementAndGet(), response) }
            .bindNow()
        try {
            val botUtils = mock(BotUtils::class.java)
            val channel = mock(MessageChannel::class.java)
            val message = mock(MessageCreateMono::class.java)
            `when`(botUtils.getBotChannel()).thenReturn(channel)
            `when`(channel.createMessage(anyString())).thenReturn(message)
            val service = NasaAPODService(
                botUtils, "http://127.0.0.1:${server.port()}/apod?api_key=", TEST_API_KEY,
                requestTimeout = Duration.ofSeconds(1), retryDelay = Duration.ofMillis(10)
            )
            check(Fixture(service, attempts, channel, message))
        } finally {
            server.disposeNow()
        }
    }

    private fun successfulResponse(response: HttpServerResponse): Mono<Void> = response
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .sendString(Mono.just("""{"title":"Test APOD","explanation":"Space","url":"https://example.com/image.jpg"}"""))
        .then()

    private data class Fixture(
        val service: NasaAPODService,
        val attempts: AtomicInteger,
        val channel: MessageChannel,
        val message: MessageCreateMono
    )

    companion object {
        private const val TEST_API_KEY = "test-key-never-log-this"
        private const val EXPECTED_MESSAGE = "NASA Picture of the day\n\nTest APOD\n\nSpace\nhttps://example.com/image.jpg"
    }
}
