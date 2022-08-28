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
        private val soundFiles = Files.walk(
            Path.of(ClassLoader.getSystemResource("sounds/wc3/").toURI())
        ).filter(Files::isRegularFile).collect(Collectors.toList())

        fun getRandomSoundFile(): File {
            return File(soundFiles[Random().nextInt(soundFiles.size)].toUri())
        }

        fun getRandomSoundFileList(count: Int): MutableList<File> {
            val fileList = mutableListOf<File>()
            for (i in 0 until count) {
                fileList.add(getRandomSoundFile())
            }
            return fileList
        }
    }
}