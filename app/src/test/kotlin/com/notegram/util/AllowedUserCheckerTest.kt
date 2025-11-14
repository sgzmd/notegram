package com.notegram.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowedUserCheckerTest {
    private val checker = AllowedUserChecker(setOf("alice", "bob"))

    @Test
    fun `allowed users are recognized ignoring case`() {
        assertTrue(checker.isAllowed("Alice"))
        assertTrue(checker.isAllowed("bob"))
    }

    @Test
    fun `disallowed or missing users are rejected`() {
        assertFalse(checker.isAllowed("charlie"))
        assertFalse(checker.isAllowed(null))
    }
}
