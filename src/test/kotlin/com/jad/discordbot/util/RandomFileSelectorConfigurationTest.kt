package com.jad.discordbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import java.nio.file.Files
import java.nio.file.Path

class RandomFileSelectorConfigurationTest {
    @TempDir
    lateinit var soundsDirectory: Path

    @Test
    fun `Spring injects the configured sounds path and selects files from it`() {
        val sound = Files.write(soundsDirectory.resolve("sound.wav"), byteArrayOf(1))
        val nestedDirectory = Files.createDirectory(soundsDirectory.resolve("nested"))
        val nestedSound = Files.write(nestedDirectory.resolve("nested.wav"), byteArrayOf(2))

        withSelector { selector ->
            val selected = selector.getRandomSoundFile().toPath()
            assertTrue(selected == sound || selected == nestedSound)
        }
    }

    @Test
    fun `configured selector returns the only available sound attachment`() {
        val sound = Files.write(soundsDirectory.resolve("sound.wav"), byteArrayOf(1))
        withSelector { selector ->
            assertEquals(sound, selector.getRandomSoundFile().toPath())
        }
    }

    private fun withSelector(check: (RandomFileSelector) -> Unit) {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "test-sounds",
                    mapOf("resources.sounds.path" to soundsDirectory.toString())
                )
            )
            context.register(RandomFileSelector::class.java)
            context.refresh()
            check(context.getBean(RandomFileSelector::class.java))
        }
    }
}
