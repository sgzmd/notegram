package com.notegram.transcription

import com.notegram.telegram.DownloadedMedia
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import com.notegram.transcription.WhisperHandle

class WhisperJniSpeechToTextServiceTest {

    @Test
    fun `transcribe succeeds with fake engine`() = runBlocking {
        val audioFile = createPcmWav()
        val media = media(audioFile)
        val engine = FakeWhisperEngine(text = "hello world")
        val service = WhisperJniSpeechToTextService(
            modelPath = audioFile,
            language = "en",
            whisper = engine,
        )

        val result = service.transcribe(media)

        assertEquals("hello world", result.text)
        // Ensure audio samples were passed through
        assertEquals(true, engine.receivedAudio.isNotEmpty())
    }

    @Test
    fun `transcribe fails on non-zero exit`() = runBlocking {
        val audioFile = createPcmWav()
        val media = media(audioFile)
        val engine = FakeWhisperEngine(text = "", exitCode = 1)
        val service = WhisperJniSpeechToTextService(
            modelPath = audioFile,
            language = "en",
            whisper = engine,
        )

        assertFailsWith<IOException> {
            service.transcribe(media)
        }
    }

    private fun media(path: java.nio.file.Path): DownloadedMedia = DownloadedMedia(
        path = path,
        sizeBytes = null,
        durationSeconds = null,
        mimeType = null,
        originalFileName = null,
        telegramFileId = "file",
    )

    private fun createPcmWav(): java.nio.file.Path {
        val format = AudioFormat(16_000f, 16, 1, true, false)
        val samples = ByteArray(1600) // short silence
        val ais = AudioInputStream(ByteArrayInputStream(samples), format, samples.size.toLong() / format.frameSize)
        val file = createTempFile(suffix = ".wav")
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file.toFile())
        return file
    }
}

private class FakeWhisperEngine(
    private val text: String,
    private val exitCode: Int = 0,
) : WhisperEngine {
    val receivedAudio = mutableListOf<FloatArray>()

    override fun initContext(modelPath: java.nio.file.Path): WhisperHandle = WhisperHandle(Any())

    override fun defaultParams(language: String): io.github.givimad.whisperjni.WhisperFullParams {
        return io.github.givimad.whisperjni.WhisperFullParams(io.github.givimad.whisperjni.WhisperSamplingStrategy.GREEDY)
    }

    override fun full(
        context: WhisperHandle,
        params: io.github.givimad.whisperjni.WhisperFullParams,
        audio: FloatArray,
    ): Int {
        receivedAudio += audio
        return exitCode
    }

    override fun collectText(context: WhisperHandle): String = text

    override fun free(context: WhisperHandle) {
        // no-op
    }
}
