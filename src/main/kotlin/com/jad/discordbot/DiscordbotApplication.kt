package com.jad.discordbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DiscordbotApplication

fun main(args: Array<String>) {
    runApplication<DiscordbotApplication>(*args)
}