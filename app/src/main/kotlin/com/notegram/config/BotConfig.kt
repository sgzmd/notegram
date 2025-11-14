package com.notegram.config

/**
 * Represents runtime configuration derived from CLI flags or environment.
 */
data class BotConfig(
    val telegramToken: String,
    val allowedUsernames: Set<String>,
    val assemblyAiToken: String,
    val geminiToken: String,
)
