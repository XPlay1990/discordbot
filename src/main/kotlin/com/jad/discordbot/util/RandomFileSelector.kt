package com.jad.discordbot.util

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

class RandomFileSelector {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun getRandomSoundFile(): File {
            val soundFiles = Files.walk(
                Path.of(ClassLoader.getSystemResource("sounds/wc3/").toURI())
            ).filter(Files::isRegularFile).collect(Collectors.toList())

            return File(soundFiles[Random().nextInt(soundFiles.size)].toUri())
        }
    }
}