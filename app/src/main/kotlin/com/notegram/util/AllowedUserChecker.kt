package com.notegram.util

/**
 * Validates whether a Telegram user is part of the configured allowlist.
 */
class AllowedUserChecker(
    allowedUsernames: Set<String>,
) {

    private val normalized = allowedUsernames.map { it.lowercase() }.toSet()

    fun isAllowed(username: String?): Boolean {
        return username != null && normalized.contains(username.lowercase())
    }
}
