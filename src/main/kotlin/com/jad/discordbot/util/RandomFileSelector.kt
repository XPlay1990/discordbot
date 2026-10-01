package com.jad.discordbot.util

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

@Component
class RandomFileSelector(
    @param:Value("\${resources.sounds.path}") private val soundPath: String
) {
    private var soundFiles: List<Path> = emptyList()

    init {
        try {
            soundFiles = Files.walk(
                Path.of(soundPath)
            ).use { paths -> paths.filter(Files::isRegularFile).collect(Collectors.toList()) }
        } catch (e: Exception) {
            logger.error("Error while loading sound files", e)
        }
    }

    fun getRandomSoundFile(): File {
        return File(soundFiles[Random().nextInt(soundFiles.size)].toUri())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
