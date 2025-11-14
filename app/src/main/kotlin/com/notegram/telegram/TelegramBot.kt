package com.notegram.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse

/**
 * Minimal abstraction over the Telegram bot SDK to simplify testing.
 */
interface TelegramClient {
    fun <T : BaseRequest<T, R>, R : BaseResponse> execute(request: T): R
}

class PengradTelegramClient(
    private val bot: TelegramBot,
) : TelegramClient {
    override fun <T : BaseRequest<T, R>, R : BaseResponse> execute(request: T): R = bot.execute(request)
}
