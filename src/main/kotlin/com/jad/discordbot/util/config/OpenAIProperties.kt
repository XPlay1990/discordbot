package com.jad.discordbot.util.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "openai")
data class OpenAiProperties(
    var apikey: String = "",
    var badwords: List<String> = emptyList(),
    var imageCount: Int = 1,
    var chat: Chat = Chat(),
    var api: Api = Api()
) {

    data class Chat(
        var model: String = ""
    )

    data class Api(
        var baseUrl: String = ""
    )
}