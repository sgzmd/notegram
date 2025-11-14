package com.notegram.telegram

import java.nio.file.Files
import java.nio.file.Path

/** Metadata describing supported Telegram media payloads. */
data class TelegramMediaDescriptor(
    val fileId: String,
    val type: TelegramMediaType,
    val fileName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val durationSeconds: Int?,
)

enum class TelegramMediaType {
    AUDIO,
    VOICE,
    VIDEO,
    VIDEO_NOTE,
}

/** Representation of an incoming Telegram message relevant for processing. */
data class IncomingTelegramMessage(
    val chatId: Long,
    val messageId: Int,
    val username: String?,
    val media: TelegramMediaDescriptor?,
)

/** Downloaded media ready for transcription pipeline. */
data class DownloadedMedia(
    val path: Path,
    val sizeBytes: Long?,
    val durationSeconds: Int?,
    val mimeType: String?,
    val originalFileName: String?,
    val telegramFileId: String,
    private val cleanupAction: (() -> Unit)? = null,
) {
    fun cleanup() {
        cleanupAction?.invoke() ?: Files.deleteIfExists(path)
    }
}

/** Result returned from the processing pipeline. */
data class ProcessingResult(
    val markdownFile: Path,
    val markdownFileName: String,
    val stats: ProcessingStats,
    private val cleanupAction: (() -> Unit)? = null,
) {
    fun cleanup() {
        cleanupAction?.invoke() ?: Files.deleteIfExists(markdownFile)
    }
}

/** Summary of processing speeds and source metadata reported to the user. */
data class ProcessingStats(
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val transcriptionLatencyMs: Long,
    val summarizationLatencyMs: Long,
)

/** Request forwarded to the transcription + summarization pipeline. */
data class MediaProcessingRequest(
    val chatId: Long,
    val messageId: Int,
    val username: String,
    val media: TelegramMediaDescriptor,
    val downloadedMedia: DownloadedMedia,
)

/** Entry point for the processing pipeline integration. */
fun interface MediaProcessingPipeline {
    suspend fun process(request: MediaProcessingRequest): ProcessingResult
}
