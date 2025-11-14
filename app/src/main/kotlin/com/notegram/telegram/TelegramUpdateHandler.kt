package com.notegram.telegram

import com.notegram.util.AllowedUserChecker
import com.notegram.util.ProfanityGenerator
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TelegramUpdateHandler(
    private val telegramBot: TelegramBot,
    private val messageProcessor: TelegramMessageProcessor,
    private val scope: CoroutineScope,
) {
    private val logger = KotlinLogging.logger {}

    fun start() {
        telegramBot.setUpdatesListener(
            { updates ->
                updates.forEach { update ->
                    scope.launch {
                        handleUpdate(update)
                    }
                }
                UpdatesListener.CONFIRMED_UPDATES_ALL
            },
            { throwable ->
                logger.error(throwable) { "Telegram long polling error" }
            },
        )
    }

    fun stop() {
        telegramBot.removeGetUpdatesListener()
    }

    private suspend fun handleUpdate(update: Update) {
        val message = update.message() ?: return
        val incoming = message.toIncomingMessage() ?: return
        messageProcessor.process(incoming)
    }

    private fun Message.toIncomingMessage(): IncomingTelegramMessage? {
        val chatId = chat()?.id() ?: return null
        val media = extractMedia()
        return IncomingTelegramMessage(
            chatId = chatId,
            messageId = messageId(),
            userId = from()?.id()?.toLong(),
            media = media,
        )
    }

    private fun Message.extractMedia(): TelegramMediaDescriptor? {
        voice()?.let {
            return TelegramMediaDescriptor(
                fileId = it.fileId(),
                type = TelegramMediaType.VOICE,
                fileName = null,
                mimeType = it.mimeType(),
                sizeBytes = it.fileSize()?.toLong(),
                durationSeconds = it.duration()?.toInt(),
            )
        }
        audio()?.let {
            val fileId = it.fileId ?: return null
            return TelegramMediaDescriptor(
                fileId = fileId,
                type = TelegramMediaType.AUDIO,
                fileName = it.fileName,
                mimeType = it.mimeType,
                sizeBytes = it.fileSize?.toLong(),
                durationSeconds = it.duration?.toInt(),
            )
        }
        video()?.let {
            val fileId = it.fileId ?: return null
            return TelegramMediaDescriptor(
                fileId = fileId,
                type = TelegramMediaType.VIDEO,
                fileName = null,
                mimeType = it.mimeType,
                sizeBytes = it.fileSize?.toLong(),
                durationSeconds = it.duration?.toInt(),
            )
        }
        videoNote()?.let {
            return TelegramMediaDescriptor(
                fileId = it.fileId(),
                type = TelegramMediaType.VIDEO_NOTE,
                fileName = null,
                mimeType = null,
                sizeBytes = it.fileSize()?.toLong(),
                durationSeconds = it.duration()?.toInt(),
            )
        }
        return null
    }
}

class TelegramMessageProcessor(
    private val allowedUserChecker: AllowedUserChecker,
    private val profanityGenerator: ProfanityGenerator,
    private val mediaDownloader: TelegramMediaDownloader,
    private val responseSender: TelegramResponseSender,
    private val processingPipeline: MediaProcessingPipeline,
) {

    private val logger = KotlinLogging.logger {}

    suspend fun process(message: IncomingTelegramMessage) {
        val userId = message.userId
        if (!allowedUserChecker.isAllowed(userId)) {
            logger.info { "Rejecting message from non-allowed user ${userId ?: "unknown"}" }
            responseSender.sendProfanity(message.chatId, message.messageId, profanityGenerator.randomQuote())
            return
        }

        val safeUserId = requireNotNull(userId) { "Allowed user id cannot be null" }
        val media = message.media
        if (media == null) {
            responseSender.sendUnsupportedMessage(message.chatId, message.messageId)
            return
        }

        responseSender.sendAcknowledgement(message.chatId, message.messageId)

        val downloaded = try {
            mediaDownloader.download(media)
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to download Telegram media ${media.fileId}" }
            responseSender.sendProcessingFailure(message.chatId, message.messageId, "Could not download media")
            return
        }

        try {
            val result = processingPipeline.process(
                MediaProcessingRequest(
                    chatId = message.chatId,
                    messageId = message.messageId,
                    userId = safeUserId,
                    media = media,
                    downloadedMedia = downloaded,
                ),
            )
            try {
                responseSender.sendProcessingSuccess(message.chatId, message.messageId, result)
            } finally {
                result.cleanup()
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Processing pipeline failed for chat ${message.chatId}" }
            responseSender.sendProcessingFailure(message.chatId, message.messageId, "Processing failed")
        } finally {
            downloaded.cleanup()
        }
    }
}
