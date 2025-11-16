# Notegram Bot – Working Draft Documentation

## Overview
Notegram is a Kotlin-based Telegram bot that accepts user-sent audio/video, downloads the media, and (eventually) transcribes and summarizes it into Markdown meeting notes. The current implementation covers scaffolding, configuration, allowlist/profanity handling, Telegram long polling, media download with basic error messaging, and a placeholder processing pipeline.

## Current State by Area
- **Configuration (`config`):** CLI parsing via `kotlinx-cli` for `--telegram_token`, `--allowed_users` (usernames, case-insensitive, `@` stripped), `--assemblyai_token`, `--gemini_token`. `BotConfig` stores normalized usernames.
- **Allowlist & Profanity (`util`):** `AllowedUserChecker` enforces username allowlist; `ProfanityGenerator` supplies random Monty Python quotes for non-allowed users.
- **Telegram Integration (`telegram`):**
  - Long polling via `TelegramUpdateHandler` with coroutine scope `SupervisorJob() + Dispatchers.IO`.
  - Message routing extracts media descriptors (voice/audio/video/video-note).
  - Media download with `DefaultTelegramMediaDownloader` (OkHttp) and explicit handling of Telegram “file is too big” errors.
  - Responses via `DefaultTelegramResponseSender` for acknowledgements, profanity replies, unsupported messages, processing success/failure, and stats formatting.
  - `TelegramMessageProcessor` wires allowlist, profanity, download, pipeline dispatch, and cleanup.
  - `MediaProcessingPipeline` interface defined; currently a stub implementation in `Main.kt` (“not implemented”).
- **Entry point (`Main.kt`):** Builds clients, wires the Telegram handler, installs a shutdown hook, and blocks on a latch. Processing pipeline is intentionally stubbed pending AssemblyAI/Gemini integration.
- **Logging:** Using kotlin-logging with lazy lambdas; logback config still TBD.
- **Testing:** Unit tests cover config parsing, allowlist behavior, profanity generation, and Telegram processor flows (including oversized-file errors).

## Known Gaps / Upcoming Work
- Implement AssemblyAI client (upload, transcript start, polling with timeouts/retries).
- Implement Gemini client and meeting-notes prompt.
- Replace the stub pipeline in `Main.kt` with real transcription + summarization sequencing and stats collection.
- Add logging configuration (logback) and any needed structured logging.
- Docker/Docker Compose packaging per the project brief.
- Update CLI/docs once IDs vs usernames are finalized (current code uses usernames for practicality).

## Running Locally (summary)
- Prepare `.env` or use `ENV_FILE` with `TELEGRAM_TOKEN`, `ALLOWED_USERS` (usernames), `ASSEMBLYAI_TOKEN`, `GEMINI_TOKEN`.
- Run `ENV_FILE=.env ./scripts/start-local.sh` to build (`:app:installDist`) and launch via `java -cp … com.notegram.MainKt`.
- Current behavior: bot acknowledges allowed users, rejects non-allowed users with profanity, and reports “too large” for oversized media; processing will fail fast because the pipeline is not yet implemented.
