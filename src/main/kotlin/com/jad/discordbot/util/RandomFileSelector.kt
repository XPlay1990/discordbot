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
    @Value("\${resources.sounds.path}") private val soundPath: String
) {
    private var soundFiles: List<Path> = emptyList()

    private var soundFilesDE: List<Path> = emptyList()

    init {
        try {
            soundFiles = Files.walk(
                Path.of(soundPath)
            ).filter(Files::isRegularFile).collect(Collectors.toList())
        } catch (e: Exception) {
            logger.error("Error while loading sound files", e)
        }

        try {
            soundFilesDE = Files.walk(
                Path.of("$soundPath/meme_de")
            ).filter(Files::isRegularFile).collect(Collectors.toList())
        } catch (e: Exception) {
            logger.error("Error while loading meme_de sound files", e)
        }
    }

    fun getRandomSoundFile(): File {
        return File(soundFiles[Random().nextInt(soundFiles.size)].toUri())
    }

    fun getRandomSoundFileDE(): File {
        return File(soundFilesDE[Random().nextInt(soundFilesDE.size)].toUri())
    }

    fun getRandomSoundFileList(count: Int): MutableList<File> {
        val fileList = mutableListOf<File>()
        for (i in 0 until count) {
            fileList.add(getRandomSoundFile())
        }
        return fileList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}