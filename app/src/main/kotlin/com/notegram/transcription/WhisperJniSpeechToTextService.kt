package com.notegram.transcription

import com.notegram.telegram.DownloadedMedia
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import io.github.givimad.whisperjni.WhisperSamplingStrategy
import io.github.givimad.whisperjni.WhisperContextParams
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Speech-to-text implementation backed by whisper-jni.
 *
 * Note: the JVM must run on a platform supported by the bundled native libs, and the specified model path must exist.
 */
class WhisperJniSpeechToTextService(
    private val modelPath: Path,
    private val language: String = "en",
    private val whisper: WhisperEngine = RealWhisperEngine(),
    private val transcoder: AudioTranscoder = FfmpegAudioTranscoder(),
) : SpeechToTextService {

    private val logger = KotlinLogging.logger {}

    override suspend fun transcribe(media: DownloadedMedia): TranscriptResult = withContext(Dispatchers.IO) {
        val audioFloats = loadAudio(media.path)
        val context = whisper.initContext(modelPath)
        try {
            val params = whisper.defaultParams(language)
            val code = whisper.full(context, params, audioFloats)
            if (code != 0) {
                throw IOException("Whisper returned non-zero status: $code")
            }
            val text = whisper.collectText(context)
            TranscriptResult(text = text, transcriptionLatencyMs = null)
        } finally {
            whisper.free(context)
        }
    }

    private fun loadAudio(path: Path): FloatArray {
        val targetFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16_000f,
            16,
            1,
            2,
            16_000f,
            false, // little-endian
        )
        var tempWav: Path? = null
        try {
            val sourceFile = path.toFile()
            val rawStream = AudioSystem.getAudioInputStream(sourceFile)
            val pcmStream: AudioInputStream = AudioSystem.getAudioInputStream(targetFormat, rawStream)
            return readPcmStream(pcmStream)
        } catch (e: Exception) {
            logger.warn(e) { "Primary audio decode failed, attempting ffmpeg transcode" }
            tempWav = transcoder.transcodeToWav(path)
            val pcmStream = AudioSystem.getAudioInputStream(tempWav.toFile())
            val converted = AudioSystem.getAudioInputStream(targetFormat, pcmStream)
            return readPcmStream(converted)
        } finally {
            tempWav?.let { runCatching { it.toFile().delete() } }
        }
    }

    private fun readPcmStream(pcmStream: AudioInputStream): FloatArray {
        val bytes = ByteArrayOutputStream()
        pcmStream.use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                bytes.write(buffer, 0, read)
            }
        }
        val pcmBytes = bytes.toByteArray()
        val samples = FloatArray(pcmBytes.size / 2)
        val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        var idx = 0
        while (bb.hasRemaining()) {
            val s = bb.short.toInt()
            samples[idx++] = s / 32768f
        }
        return samples
    }
}

interface WhisperEngine {
    fun initContext(modelPath: Path): WhisperHandle
    fun defaultParams(language: String): WhisperFullParams
    fun full(context: WhisperHandle, params: WhisperFullParams, audio: FloatArray): Int
    fun collectText(context: WhisperHandle): String
    fun free(context: WhisperHandle)
}

data class WhisperHandle(val context: Any)

class RealWhisperEngine : WhisperEngine {
    private val whisper = WhisperJNI()

    init {
        WhisperJNI.loadLibrary()
    }

    override fun initContext(modelPath: Path): WhisperHandle {
        val ctx = whisper.init(modelPath, WhisperContextParams())
        return WhisperHandle(ctx)
    }

    override fun defaultParams(language: String): WhisperFullParams {
        return WhisperFullParams(WhisperSamplingStrategy.GREEDY).apply {
            this.language = language
            this.detectLanguage = false
            this.noTimestamps = true
            this.printProgress = false
            this.printRealtime = false
            this.printTimestamps = false
        }
    }

    override fun full(context: WhisperHandle, params: WhisperFullParams, audio: FloatArray): Int {
        val ctx = context.context as io.github.givimad.whisperjni.WhisperContext
        return whisper.full(ctx, params, audio, audio.size)
    }

    override fun collectText(context: WhisperHandle): String {
        val ctx = context.context as io.github.givimad.whisperjni.WhisperContext
        val segments = whisper.fullNSegments(ctx)
        val builder = StringBuilder()
        for (i in 0 until segments) {
            builder.append(whisper.fullGetSegmentText(ctx, i))
        }
        return builder.toString().trim()
    }

    override fun free(context: WhisperHandle) {
        val ctx = context.context as io.github.givimad.whisperjni.WhisperContext
        whisper.free(ctx)
    }
}
