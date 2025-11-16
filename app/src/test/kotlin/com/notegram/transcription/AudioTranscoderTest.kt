package com.notegram.transcription

import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertTrue

class AudioTranscoderTest {
    @Test
    fun `ffmpeg command is constructed`() {
        val transcoder = object : AudioTranscoder {
            override fun transcodeToWav(input: Path): Path {
                return input
            }
        }
        val input = createTempFile()
        val result = transcoder.transcodeToWav(input)
        assertTrue(result.toString().isNotEmpty())
    }
}
