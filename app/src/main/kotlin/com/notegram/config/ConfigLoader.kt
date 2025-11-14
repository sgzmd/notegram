package com.notegram.config

/**
 * Converts CLI arguments into a strongly typed [BotConfig].
 */
object ConfigLoader {

    fun load(rawArgs: Array<String>): BotConfig {
        val parsed = CommandLineArgs.parse(rawArgs)
        validateTokens(parsed)

        return BotConfig(
            telegramToken = parsed.telegramToken,
            allowedUserIds = parseAllowedUsers(parsed.allowedUsersCsv),
            assemblyAiToken = parsed.assemblyAiToken,
            geminiToken = parsed.geminiToken,
        )
    }

    private fun validateTokens(args: CommandLineArgs) {
        require(args.telegramToken.isNotBlank()) { "Telegram token cannot be blank" }
        require(args.assemblyAiToken.isNotBlank()) { "AssemblyAI token cannot be blank" }
        require(args.geminiToken.isNotBlank()) { "Gemini token cannot be blank" }
    }

    private fun parseAllowedUsers(csv: String): Set<Long> {
        require(csv.isNotBlank()) { "Allowed user list cannot be blank" }
        val ids = csv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { value ->
                value.toLongOrNull() ?: throw IllegalArgumentException("Invalid user id: $value")
            }
            .toSet()
        require(ids.isNotEmpty()) { "Allowed user list cannot be empty" }
        return ids
    }
}
