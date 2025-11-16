package com.notegram

import com.notegram.config.ConfigLoader
import com.notegram.telegram.DefaultTelegramMediaDownloader
import com.notegram.telegram.DefaultTelegramResponseSender
import com.notegram.telegram.PengradTelegramClient
import com.notegram.telegram.TelegramMessageProcessor
import com.notegram.telegram.TelegramUpdateHandler
import com.notegram.telegram.MediaProcessingPipeline
import com.notegram.telegram.MediaProcessingRequest
import com.notegram.util.AllowedUserChecker
import com.notegram.util.ProfanityGenerator
import com.pengrad.telegrambot.TelegramBot
import com.notegram.transcription.SpeechToTextPipeline
import com.notegram.transcription.WhisperJniSpeechToTextService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}

    val config = ConfigLoader.load(args)
    val bot = TelegramBot(config.telegramToken)
    val telegramClient = PengradTelegramClient(bot)
    val okHttp = OkHttpClient()
    val downloader = DefaultTelegramMediaDownloader(telegramClient, okHttp, config.telegramToken)
    val responseSender = DefaultTelegramResponseSender(telegramClient)
    val allowedChecker = AllowedUserChecker(config.allowedUsernames)
    val profanityGenerator = ProfanityGenerator()
    val whisperModelPath = System.getenv("WHISPER_MODEL_PATH")?.let(Path::of)
        ?: error("WHISPER_MODEL_PATH must be set to use Whisper transcription")
    val speechToText = WhisperJniSpeechToTextService(modelPath = whisperModelPath)
    val pipeline: MediaProcessingPipeline = SpeechToTextPipeline(speechToText)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val processor = TelegramMessageProcessor(
        allowedUserChecker = allowedChecker,
        profanityGenerator = profanityGenerator,
        mediaDownloader = downloader,
        responseSender = responseSender,
        processingPipeline = pipeline,
    )

    val handler = TelegramUpdateHandler(bot, processor, scope)
    val latch = CountDownLatch(1)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Shutdown requested" }
            handler.stop()
            scope.cancel()
            latch.countDown()
        },
    )

    logger.info { "Starting Notegram bot (Telegram long polling)" }
    handler.start()
    logger.info { "Bot started; awaiting updates." }

    latch.await()
}

private object NotImplementedPipeline : MediaProcessingPipeline {
    override suspend fun process(request: MediaProcessingRequest): Nothing {
        error("Transcription pipeline not implemented yet")
    }
}
