package com.notegram.transcription

import com.notegram.telegram.DownloadedMedia
import com.notegram.telegram.MediaProcessingPipeline
import com.notegram.telegram.MediaProcessingRequest
import com.notegram.telegram.ProcessingResult
import com.notegram.telegram.ProcessingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis

/**
 * Media processing pipeline that performs speech-to-text and returns a Markdown transcript file.
 */
class SpeechToTextPipeline(
    private val speechToText: SpeechToTextService,
) : MediaProcessingPipeline {

    override suspend fun process(request: MediaProcessingRequest): ProcessingResult {
        val (result, latencyMs) = transcribe(request.downloadedMedia)
        val markdownFile = writeMarkdown(result.text)
        val stats = ProcessingStats(
            durationSeconds = request.downloadedMedia.durationSeconds?.toLong() ?: 0L,
            fileSizeBytes = request.downloadedMedia.sizeBytes ?: 0L,
            transcriptionLatencyMs = result.transcriptionLatencyMs ?: latencyMs,
            summarizationLatencyMs = 0L,
        )
        return ProcessingResult(
            markdownFile = markdownFile,
            markdownFileName = "transcript.md",
            stats = stats,
        )
    }

    private suspend fun transcribe(media: DownloadedMedia): Pair<TranscriptResult, Long> {
        val result: TranscriptResult
        val measured: Long
        measured = measureTimeMillis {
            result = speechToText.transcribe(media)
        }
        val latency = result.transcriptionLatencyMs ?: measured
        return result to latency
    }

    private suspend fun writeMarkdown(text: String): Path = withContext(Dispatchers.IO) {
        val file = createTempFile(prefix = "notegram-transcript-", suffix = ".md")
        file.writeText(
            buildString {
                appendLine("# Transcript")
                appendLine()
                appendLine(text.trim())
            }.trimEnd(),
        )
        file
    }
}
