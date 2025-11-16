# Notegram Telegram Bot

This repository hosts a Kotlin-based Telegram bot that transcribes user-submitted audio/video, summarizes the transcript with Gemini, and returns structured meeting notes. The bot currently targets JVM 21 and is built with Gradle.

## Local Development Quickstart

### 1. Prerequisites
- JDK 21
- Bash-compatible shell
- Access tokens for Telegram, AssemblyAI, and Gemini
- At least one Telegram username allowed to interact with the bot (prefix optional `@` will be stripped)

### 2. Create a local environment file
Create `.env.local` in the repository root (or another path referenced via `ENV_FILE`). Example:

```env
TELEGRAM_TOKEN=123456789:ABCDEF_your_bot_token
ALLOWED_USERS=sgzmd,design
ASSEMBLYAI_TOKEN=assemblyai_token_here
GEMINI_TOKEN=gemini_token_here
```

### 3. Start the bot
Run the helper script, which loads environment variables, performs sanity checks, builds the distribution jar set, and launches the app via `java`:

```bash
./scripts/start-local.sh
```

The script accepts an optional `ENV_FILE` variable to point at a different env file:

```bash
ENV_FILE=.env ./scripts/start-local.sh
```

Logs stream to the console through Gradle. Stop the bot with `Ctrl+C`.

## About `scripts/start-local.sh`
- Loads environment variables from `.env.local` by default (or any file via `ENV_FILE`).
- Enforces the presence of `TELEGRAM_TOKEN`, `ALLOWED_USERS`, `ASSEMBLYAI_TOKEN`, and `GEMINI_TOKEN` before execution.
- `ALLOWED_USERS` accepts comma-separated usernames (the script and bot treat them case-insensitively and strip `@`).
- Builds the runnable distribution via `./gradlew :app:installDist`, then launches `com.notegram.MainKt` with `java -cp app/build/install/app/lib/* ...`.
- Override `JAVA_CMD` or `MAIN_CLASS` if you need a different JVM or entrypoint.
- Fails fast if required dependencies are missing, ensuring misconfiguration is caught early.

### Customizing Behavior
- Override the Gradle task by editing `GRADLE_TASK` in the script if you need a different entrypoint.
- Pass JVM debugging flags by exporting `ORG_GRADLE_PROJECT_org.gradle.jvmargs` before running the script.
- To run in CI without a `.env.local`, export the required variables directly in the environment.

## Troubleshooting
- **Missing env file**: ensure `.env.local` exists or set `ENV_FILE`.
- **Permission denied**: run `chmod +x scripts/start-local.sh` if the script loses execute permissions.
- **Gradle daemon issues**: stop daemons with `./gradlew --stop` and rerun the script.
- **Token errors from Telegram/AssemblyAI/Gemini**: verify the exported variables or .env contents.
- **File too large**: Telegram bots cannot download media beyond ~20MB—trim or compress before sending.

## Repository Structure (excerpt)
- `app/`: Kotlin source, tests, and Gradle module configuration.
- `scripts/start-local.sh`: one-touch launcher for local development.
- `gradle/`: Gradle wrapper files.
- `README.md`: this document.

For deeper architectural details and future deployment instructions, refer to `prompt.md` and future documentation updates.
