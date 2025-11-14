package com.notegram.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigLoaderTest {
    @Test
    fun `load parses csv allowlist`() {
        val args = arrayOf(
            "--telegram_token", "tg-token",
            "--allowed_users", "123, 456",
            "--assemblyai_token", "aa",
            "--gemini_token", "gm",
        )

        val config = ConfigLoader.load(args)

        assertEquals(setOf(123L, 456L), config.allowedUserIds)
        assertEquals("tg-token", config.telegramToken)
        assertEquals("aa", config.assemblyAiToken)
        assertEquals("gm", config.geminiToken)
    }

    @Test
    fun `load fails for invalid user id`() {
        val args = arrayOf(
            "--telegram_token", "tg-token",
            "--allowed_users", "abc",
            "--assemblyai_token", "aa",
            "--gemini_token", "gm",
        )

        assertFailsWith<IllegalArgumentException> {
            ConfigLoader.load(args)
        }
    }
}
