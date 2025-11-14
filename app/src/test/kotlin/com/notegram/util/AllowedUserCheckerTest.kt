package com.notegram.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowedUserCheckerTest {
    private val checker = AllowedUserChecker(setOf(1L, 2L))

    @Test
    fun `allowed users are recognized`() {
        assertTrue(checker.isAllowed(1L))
        assertTrue(checker.isAllowed(2L))
    }

    @Test
    fun `disallowed or missing users are rejected`() {
        assertFalse(checker.isAllowed(3L))
        assertFalse(checker.isAllowed(null))
    }
}
