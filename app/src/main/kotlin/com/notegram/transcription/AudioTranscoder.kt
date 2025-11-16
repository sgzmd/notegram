package com.notegram.transcription

import com.notegram.util.execBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createTempFile

interface AudioTranscoder {
    /**
    * Transcode the input audio to a mono 16k PCM WAV suitable for Whisper.
    * Returns the path to the transcoded file (caller is responsible for cleanup).
    */
    fun transcodeToWav(input: Path): Path
}

class FfmpegAudioTranscoder : AudioTranscoder {
    private val logger = KotlinLogging.logger {}
    override fun transcodeToWav(input: Path): Path {
        val output = createTempFile(prefix = "notegram-ffmpeg-", suffix = ".wav")
        val command = listOf(
            "ffmpeg",
            "-y",
            "-i", input.toString(),
            "-ac", "1",
            "-ar", "16000",
            "-f", "wav",
            output.toString(),
        )
        logger.info { "Transcoding audio via ffmpeg for ${input.fileName}" }
        val result = execBlocking(command)
        if (result.exitCode != 0) {
            throw IOException("ffmpeg failed with code ${result.exitCode}: ${result.stderr.ifBlank { result.stdout }}")
        }
        return output
    }
}
