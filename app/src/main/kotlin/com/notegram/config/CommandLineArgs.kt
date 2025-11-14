package com.notegram.config

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * Raw representation of CLI arguments prior to validation or transformation.
 */
data class CommandLineArgs(
    val telegramToken: String,
    val allowedUsersCsv: String,
    val assemblyAiToken: String,
    val geminiToken: String,
) {
    companion object {
        fun parse(rawArgs: Array<String>): CommandLineArgs {
            val parser = ArgParser("notegram")

            val telegramToken by parser.option(
                ArgType.String,
                fullName = "telegram_token",
                description = "Telegram bot token obtained from BotFather",
            ).required()

            val allowedUsers by parser.option(
                ArgType.String,
                fullName = "allowed_users",
                description = "Comma-separated list of allowed Telegram user IDs",
            ).required()

            val assemblyAiToken by parser.option(
                ArgType.String,
                fullName = "assemblyai_token",
                description = "AssemblyAI API token",
            ).required()

            val geminiToken by parser.option(
                ArgType.String,
                fullName = "gemini_token",
                description = "Gemini API token",
            ).required()

            parser.parse(rawArgs)

            return CommandLineArgs(
                telegramToken = telegramToken.trim(),
                allowedUsersCsv = allowedUsers.trim(),
                assemblyAiToken = assemblyAiToken.trim(),
                geminiToken = geminiToken.trim(),
            )
        }
    }
}
