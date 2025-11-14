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
            allowedUsernames = parseAllowedUsers(parsed.allowedUsersCsv),
            assemblyAiToken = parsed.assemblyAiToken,
            geminiToken = parsed.geminiToken,
        )
    }

    private fun validateTokens(args: CommandLineArgs) {
        require(args.telegramToken.isNotBlank()) { "Telegram token cannot be blank" }
        require(args.assemblyAiToken.isNotBlank()) { "AssemblyAI token cannot be blank" }
        require(args.geminiToken.isNotBlank()) { "Gemini token cannot be blank" }
    }

    private fun parseAllowedUsers(csv: String): Set<String> {
        require(csv.isNotBlank()) { "Allowed user list cannot be blank" }
        val usernames = csv
            .split(",")
            .map { normalizeUsername(it) }
            .toSet()
        require(usernames.isNotEmpty()) { "Allowed user list cannot be empty" }
        return usernames
    }

    private fun normalizeUsername(raw: String): String {
        val cleaned = raw.trim().removePrefix("@").lowercase()
        require(cleaned.isNotEmpty()) { "Username cannot be blank: '$raw'" }
        return cleaned
    }
}
