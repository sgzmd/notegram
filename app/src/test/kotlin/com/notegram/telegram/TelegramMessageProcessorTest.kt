package com.notegram.telegram

import com.notegram.util.AllowedUserChecker
import com.notegram.util.ProfanityGenerator
import java.nio.file.Files
import kotlin.io.path.createTempFile
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TelegramMessageProcessorTest {

    @Test
    fun `non-allowed user receives profanity`() = runTest {
        val sender = RecordingResponseSender()
        val processor = buildProcessor(sender = sender)
        val message = IncomingTelegramMessage(chatId = 1L, messageId = 10, username = "intruder", media = null)

        processor.process(message)

        assertEquals(1, sender.profanity.size)
        assertTrue(sender.acknowledgements.isEmpty())
        assertNull(sender.lastSuccess)
    }

    @Test
    fun `allowed user without media gets guidance`() = runTest {
        val sender = RecordingResponseSender()
        val processor = buildProcessor(sender = sender)
        val message = IncomingTelegramMessage(chatId = 1L, messageId = 10, username = "alice", media = null)

        processor.process(message)

        assertEquals(1, sender.unsupported.size)
        assertTrue(sender.profanity.isEmpty())
    }

    @Test
    fun `successful processing sends ack and stats`() = runTest {
        val sender = RecordingResponseSender()
        val downloader = RecordingDownloader()
        val pipeline = RecordingPipeline()
        val processor = buildProcessor(sender = sender, downloader = downloader, pipeline = pipeline)
        val media = TelegramMediaDescriptor(
            fileId = "file-123",
            type = TelegramMediaType.AUDIO,
            fileName = "clip.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 512L,
            durationSeconds = 42,
        )
        val message = IncomingTelegramMessage(chatId = 5L, messageId = 7, username = "alice", media = media)
        pipeline.result = ProcessingResult(
            markdownFile = createTempFile(),
            markdownFileName = "notes.md",
            stats = ProcessingStats(60, 2048, 1_500, 800),
        )

        processor.process(message)

        assertEquals(media, downloader.requested)
        assertEquals(1, sender.acknowledgements.size)
        assertEquals(message.chatId, sender.lastSuccess?.chatId)
        assertEquals(ProcessingStats(60, 2048, 1_500, 800), sender.lastSuccess?.stats)
        assertEquals(1, downloader.cleanupCount)
        assertEquals(1, pipeline.cleanupCount)
    }

    @Test
    fun `download failure notifies user`() = runTest {
        val sender = RecordingResponseSender()
        val downloader = object : TelegramMediaDownloader {
            override suspend fun download(media: TelegramMediaDescriptor): DownloadedMedia {
                throw IllegalStateException("boom")
            }
        }
        val pipeline = RecordingPipeline()
        val processor = buildProcessor(sender = sender, downloader = downloader, pipeline = pipeline)
        val media = TelegramMediaDescriptor(
            fileId = "file",
            type = TelegramMediaType.VOICE,
            fileName = null,
            mimeType = null,
            sizeBytes = null,
            durationSeconds = null,
        )
        val message = IncomingTelegramMessage(chatId = 1L, messageId = 2, username = "alice", media = media)

        processor.process(message)

        assertEquals(1, sender.failures.size)
        assertTrue(pipeline.invocations.isEmpty())
    }

    private fun buildProcessor(
        sender: RecordingResponseSender,
        downloader: TelegramMediaDownloader = RecordingDownloader(),
        pipeline: RecordingPipeline = RecordingPipeline(),
    ): TelegramMessageProcessor {
        val allowedChecker = AllowedUserChecker(setOf("alice"))
        val profanity = ProfanityGenerator(Random(0))
        return TelegramMessageProcessor(allowedChecker, profanity, downloader, sender, pipeline)
    }
}

private class RecordingDownloader : TelegramMediaDownloader {
    var requested: TelegramMediaDescriptor? = null
    var cleanupCount: Int = 0

    override suspend fun download(media: TelegramMediaDescriptor): DownloadedMedia {
        requested = media
        val tempFile = createTempFile()
        return DownloadedMedia(
            path = tempFile,
            sizeBytes = media.sizeBytes,
            durationSeconds = media.durationSeconds,
            mimeType = media.mimeType,
            originalFileName = media.fileName,
            telegramFileId = media.fileId,
        ) {
            cleanupCount += 1
            Files.deleteIfExists(tempFile)
        }
    }
}

private class RecordingPipeline : MediaProcessingPipeline {
    var result: ProcessingResult = ProcessingResult(
        markdownFile = createTempFile(),
        markdownFileName = "default.md",
        stats = ProcessingStats(1, 1, 1, 1),
    )
    var cleanupCount: Int = 0
    val invocations = mutableListOf<MediaProcessingRequest>()

    override suspend fun process(request: MediaProcessingRequest): ProcessingResult {
        invocations += request
        return ProcessingResult(
            markdownFile = result.markdownFile,
            markdownFileName = result.markdownFileName,
            stats = result.stats,
        ) {
            cleanupCount += 1
            Files.deleteIfExists(result.markdownFile)
        }
    }
}

private class RecordingResponseSender : TelegramResponseSender {
    val acknowledgements = mutableListOf<Pair<Long, Int>>()
    val profanity = mutableListOf<Pair<Long, Int>>()
    val unsupported = mutableListOf<Pair<Long, Int>>()
    val failures = mutableListOf<Pair<Long, Int>>()
    var lastSuccess: SuccessRecord? = null

    override fun sendAcknowledgement(chatId: Long, replyToMessageId: Int) {
        acknowledgements += chatId to replyToMessageId
    }

    override fun sendProfanity(chatId: Long, replyToMessageId: Int, quote: String) {
        profanity += chatId to replyToMessageId
    }

    override fun sendUnsupportedMessage(chatId: Long, replyToMessageId: Int) {
        unsupported += chatId to replyToMessageId
    }

    override fun sendProcessingSuccess(chatId: Long, replyToMessageId: Int, result: ProcessingResult) {
        lastSuccess = SuccessRecord(chatId, replyToMessageId, result.stats)
    }

    override fun sendProcessingFailure(chatId: Long, replyToMessageId: Int, reason: String) {
        failures += chatId to replyToMessageId
    }
}

private data class SuccessRecord(
    val chatId: Long,
    val messageId: Int,
    val stats: ProcessingStats,
)
