package com.notegram.telegram

import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.response.GetFileResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createTempFile

interface TelegramMediaDownloader {
    suspend fun download(media: TelegramMediaDescriptor): DownloadedMedia
}

class DefaultTelegramMediaDownloader(
    private val telegramClient: TelegramClient,
    private val httpClient: OkHttpClient,
    private val botToken: String,
) : TelegramMediaDownloader {

    private val logger = KotlinLogging.logger {}

    override suspend fun download(media: TelegramMediaDescriptor): DownloadedMedia = withContext(Dispatchers.IO) {
        logger.info { "Downloading Telegram media ${media.fileId}" }
        val fileInfo = fetchTelegramFile(media.fileId)
        val tempFile = allocateTempFile(media.fileName)
        val downloadUrl = buildDownloadUrl(fileInfo)
        streamToFile(downloadUrl, tempFile)
        DownloadedMedia(
            path = tempFile,
            sizeBytes = fileInfo.fileSize() ?: media.sizeBytes,
            durationSeconds = media.durationSeconds,
            mimeType = media.mimeType,
            originalFileName = media.fileName,
            telegramFileId = media.fileId,
        )
    }

    private fun fetchTelegramFile(fileId: String): com.pengrad.telegrambot.model.File {
        val response: GetFileResponse = telegramClient.execute(GetFile(fileId))
        if (!response.isOk) {
            val description = response.description() ?: "unknown error"
            if (description.contains("file is too big", ignoreCase = true)) {
                throw MediaTooLargeException(description)
            }
            throw IOException("Failed to fetch Telegram file metadata: $description")
        }
        return response.file() ?: throw IOException("Telegram response missing file metadata")
    }

    private fun allocateTempFile(fileName: String?): Path {
        val suffix = fileName?.let { "-$it" } ?: ".media"
        return createTempFile(prefix = "notegram-", suffix = suffix)
    }

    private fun buildDownloadUrl(file: com.pengrad.telegrambot.model.File): String {
        val path = file.filePath() ?: throw IOException("Telegram file metadata missing file path")
        return "https://api.telegram.org/file/bot$botToken/$path"
    }

    private fun streamToFile(url: String, target: Path) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download media from Telegram: ${response.code}")
            }
            val responseBody = response.body ?: throw IOException("Empty body while downloading Telegram file")
            responseBody.byteStream().use { input ->
                target.toFile().outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

class MediaTooLargeException(message: String) : IOException(message)
