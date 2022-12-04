package com.jad.discordbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class DiscordbotApplication

fun main(args: Array<String>) {
    runApplication<DiscordbotApplication>(*args)
}