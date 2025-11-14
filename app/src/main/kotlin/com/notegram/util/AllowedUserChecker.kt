package com.notegram.util

/**
 * Validates whether a Telegram user is part of the configured allowlist.
 */
class AllowedUserChecker(
    private val allowedUserIds: Set<Long>,
) {

    fun isAllowed(userId: Long?): Boolean {
        return userId != null && allowedUserIds.contains(userId)
    }
}
