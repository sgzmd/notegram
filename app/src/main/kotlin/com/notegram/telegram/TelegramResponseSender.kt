package com.notegram.telegram

import com.pengrad.telegrambot.model.request.ReplyParameters
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.pengrad.telegrambot.response.SendResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.math.pow
import kotlin.math.round

interface TelegramResponseSender {
    fun sendAcknowledgement(chatId: Long, replyToMessageId: Int)
    fun sendProfanity(chatId: Long, replyToMessageId: Int, quote: String)
    fun sendUnsupportedMessage(chatId: Long, replyToMessageId: Int)
    fun sendProcessingSuccess(chatId: Long, replyToMessageId: Int, result: ProcessingResult)
    fun sendProcessingFailure(chatId: Long, replyToMessageId: Int, reason: String)
}

class DefaultTelegramResponseSender(
    private val telegramClient: TelegramClient,
) : TelegramResponseSender {

    private val logger = KotlinLogging.logger {}

    override fun sendAcknowledgement(chatId: Long, replyToMessageId: Int) {
        val message = "Got it! Processing your media now, I'll be back with notes shortly."
        sendMessage(chatId, message, replyToMessageId)
    }

    override fun sendProfanity(chatId: Long, replyToMessageId: Int, quote: String) {
        sendMessage(chatId, quote, replyToMessageId)
    }

    override fun sendUnsupportedMessage(chatId: Long, replyToMessageId: Int) {
        val message = "Please send an audio or video message so I can take notes."
        sendMessage(chatId, message, replyToMessageId)
    }

    override fun sendProcessingSuccess(chatId: Long, replyToMessageId: Int, result: ProcessingResult) {
        logger.info { "Sending processed notes to chat $chatId" }
        sendDocument(chatId, replyToMessageId, result.markdownFile)
        val statsMessage = formatStatsMessage(result.stats)
        sendMessage(chatId, statsMessage, replyToMessageId)
    }

    override fun sendProcessingFailure(chatId: Long, replyToMessageId: Int, reason: String) {
        val message = buildString {
            appendLine("Processing failed: $reason")
            append("Please try again in a moment.")
        }
        sendMessage(chatId, message.trim(), replyToMessageId)
    }

    private fun sendMessage(chatId: Long, text: String, replyToMessageId: Int): SendResponse {
        val request = SendMessage(chatId, text).replyParameters(replyParams(replyToMessageId))
        return executeRequest(request)
    }

    private fun sendDocument(chatId: Long, replyToMessageId: Int, path: Path): SendResponse {
        val request = SendDocument(chatId, path.toFile()).replyParameters(replyParams(replyToMessageId))
        return executeRequest(request)
    }

    private fun replyParams(replyToMessageId: Int): ReplyParameters {
        return ReplyParameters(replyToMessageId).allowSendingWithoutReply(true)
    }

    private fun <T : BaseRequest<T, R>, R : BaseResponse> executeRequest(request: T): R {
        val response: R = telegramClient.execute(request)
        if (!response.isOk) {
            logger.warn { "Telegram API call failed: ${response.description()}" }
        }
        return response
    }

    private fun formatStatsMessage(stats: ProcessingStats): String {
        return buildString {
            appendLine("Processing complete ✅")
            appendLine("Duration: ${stats.durationSeconds}s")
            appendLine("Size: ${formatSize(stats.fileSizeBytes)}")
            appendLine("Transcription latency: ${formatLatency(stats.transcriptionLatencyMs)}")
            append("Summarization latency: ${formatLatency(stats.summarizationLatencyMs)}")
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${roundToTwo(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "${roundToTwo(mb)} MB"
        val gb = mb / 1024.0
        return "${roundToTwo(gb)} GB"
    }

    private fun formatLatency(latencyMs: Long): String {
        val seconds = latencyMs / 1000.0
        return "${roundToTwo(seconds)} s"
    }

    private fun roundToTwo(value: Double): String {
        val factor = 10.0.pow(2)
        val rounded = round(value * factor) / factor
        return "%.2f".format(rounded)
    }
}
