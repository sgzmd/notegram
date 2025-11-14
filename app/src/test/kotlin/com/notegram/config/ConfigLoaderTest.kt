package com.notegram.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigLoaderTest {
    @Test
    fun `load parses csv allowlist`() {
        val args = arrayOf(
            "--telegram_token", "tg-token",
            "--allowed_users", "alice, Bob , @Another",
            "--assemblyai_token", "aa",
            "--gemini_token", "gm",
        )

        val config = ConfigLoader.load(args)

        assertEquals(setOf("alice", "bob", "another"), config.allowedUsernames)
        assertEquals("tg-token", config.telegramToken)
        assertEquals("aa", config.assemblyAiToken)
        assertEquals("gm", config.geminiToken)
    }

    @Test
    fun `load fails for invalid username`() {
        val args = arrayOf(
            "--telegram_token", "tg-token",
            "--allowed_users", ", ,",
            "--assemblyai_token", "aa",
            "--gemini_token", "gm",
        )

        assertFailsWith<IllegalArgumentException> {
            ConfigLoader.load(args)
        }
    }
}
