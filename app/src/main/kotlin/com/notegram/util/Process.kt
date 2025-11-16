package com.notegram.util

import java.io.BufferedReader
import java.io.InputStreamReader

data class ExecResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)

fun execBlocking(command: List<String>): ExecResult {
    val process = ProcessBuilder(command)
        .redirectErrorStream(false)
        .start()
    val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
    val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)
    val code = process.waitFor()
    return ExecResult(stdout, stderr, code)
}
