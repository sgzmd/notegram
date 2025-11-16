package com.notegram.transcription

import com.notegram.telegram.DownloadedMedia

/**
 * Converts downloaded audio/video into plain text transcript.
 */
fun interface SpeechToTextService {
    suspend fun transcribe(media: DownloadedMedia): TranscriptResult
}

data class TranscriptResult(
    val text: String,
    val transcriptionLatencyMs: Long? = null,
)
