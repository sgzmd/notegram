package com.notegram.transcription

import com.notegram.telegram.DownloadedMedia
import com.notegram.telegram.MediaProcessingRequest
import com.notegram.telegram.TelegramMediaDescriptor
import com.notegram.telegram.TelegramMediaType
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SpeechToTextPipelineTest {

    @Test
    fun `pipeline writes markdown and stats`() = runBlocking {
        val mediaFile = createTempFile().apply { toFile().writeBytes(ByteArray(10)) }
        val media = DownloadedMedia(
            path = mediaFile,
            sizeBytes = 10,
            durationSeconds = 5,
            mimeType = "audio/wav",
            originalFileName = "clip.wav",
            telegramFileId = "file",
        )
        val request = MediaProcessingRequest(
            chatId = 1L,
            messageId = 1,
            username = "tester",
            media = TelegramMediaDescriptor(
                fileId = "file",
                type = TelegramMediaType.AUDIO,
                fileName = "clip.wav",
                mimeType = "audio/wav",
                sizeBytes = 10,
                durationSeconds = 5,
            ),
            downloadedMedia = media,
        )
        val pipeline = SpeechToTextPipeline(FakeSpeechToText("hello"))

        val result = pipeline.process(request)

        val contents = result.markdownFile.readText()
        assertEquals(true, contents.contains("hello"))
        assertEquals(5L, result.stats.durationSeconds)
        assertEquals(10L, result.stats.fileSizeBytes)
    }
}

private class FakeSpeechToText(private val transcript: String) : SpeechToTextService {
    override suspend fun transcribe(media: DownloadedMedia): TranscriptResult {
        return TranscriptResult(text = transcript, transcriptionLatencyMs = 123)
    }
}
