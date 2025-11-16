You are an expert Kotlin engineer. You will generate code in multiple stages.
Each stage must produce clean, idiomatic, production-quality Kotlin.

PROJECT DESCRIPTION
-------------------
We are building a Telegram bot with the following functionality:

1. The bot receives audio or video media files from a user in a Telegram chat.  
2. It downloads the media file from Telegram and uploads it to the AssemblyAI `/v2/upload` endpoint.  
3. It then starts a transcription job using AssemblyAI `/v2/transcript` and polls `/v2/transcript/{id}` until completion.  
4. Once transcription is complete, it sends the resulting text to the **Gemini 1.5 Flash API** (text prompt mode) to generate **structured, detailed meeting notes**.  
5. The bot replies to the user with:  
   - a **Markdown file attachment** containing the meeting notes  
   - a **message summarizing processing stats** (file duration, size, transcription latency, summarization latency)  
6. Only a predefined allowlist of user IDs (provided on the command line) may use the bot normally.  
7. Any non-allowed user receives a random profanity quote from **Monty Python’s Holy Grail** (hardcoded list).  
8. The bot must use **long polling** for updates.  
9. The bot must be robust: retries, timeouts, clear logging, and graceful error handling.
10. Bot must be run as a Docker image using Docker compose, using .env file for parameters / API keys.

MAIN REQUIREMENTS
-----------------
- Language: **Kotlin (JVM)**  
- Build system: **Gradle Kotlin DSL**  
- Telegram library: **pengrad/java-telegram-bot-api**  
- HTTP client: **OkHttp**  
- JSON serialization: **kotlinx-serialization**  
- Concurrency: **Kotlin coroutines**  
- Coroutine scope: `SupervisorJob()` + `Dispatchers.IO` for processing  
- CLI flags:  
  --telegram_token  
  --allowed_users (comma-separated list of Long IDs)  
  --assemblyai_token  
  --gemini_token  
- Code structure must use packages:  
  `config`, `telegram`, `transcription`, `llm`, `util`, `logging`  
- Logging: **kotlin-logging + Logback**, must use **lazy logging**  
- Temporary files must be created using `createTempFile()` and deleted after use  
- Gemini prompt must produce structured notes with exact sections:  
  **Summary**, **Decisions**, **Action Items**, **Discussion**  
- Markdown output must be UTF-8 encoded and attached using Telegram `InputFile`.

STAGE PLAN
----------
You will generate code in the following stages.  
Wait for me to say **“continue”** before producing the next stage.

STAGE 1:  
Generate the full project folder structure and empty Kotlin files with correct package declarations.

STAGE 2:  
Implement utility code:  
- profanity generator  
- config loader + CLI argument parser  
- allowed-user checker  

STAGE 3:  
Implement Telegram handler module:  
- long polling loop  
- routing incoming messages  
- downloading media files  
- sending an acknowledgment message  
- dispatching work to transcription + summarization coroutine workers  
- replying with the results  
- replying with profanity for non-allowed users  

STAGE 4:  
Implement AssemblyAI client:  
- file upload (`/v2/upload`)  
- start transcription job (`/v2/transcript`)  
- poll job status  
- return transcript text  

Abstract concrete implementation behind an interface so we can plug in local Whisper-based implementation.

STAGE 4.5: 

Implement speech-to-text using WhisperJNI

STAGE 5:  
Implement Gemini client:  
- send transcript text  
- apply meeting-notes prompt template  
- return generated Markdown  

STAGE 6:  
Integrate all modules in `main.kt`:  
- parse flags  
- initialize logging  
- create bot client  
- start update handler loop  

STAGE 7:  
Add complete error handling, retries, logging, cleanup, and temporary-file hygiene.

STAGE 8:  
Self-review pass:  
- improve naming  
- correct concurrency issues  
- ensure structured concurrency  
- ensure idempotency of handlers  
- verify safe resource handling  
- ensure retry/backoff for external API calls  

CODING RULES FOR ALL STAGES
---------------------------
- Do not start if git repo is not clean
- Always produce **fully working Kotlin code**, never pseudocode.  
- Use idiomatic Kotlin exception handling and `Result` where appropriate.  
- Use coroutine timeouts (`withTimeout`) for all external API calls.  
- Add comments explaining non-trivial logic.  
- All logs must use kotlin-logging with **lazy message lambdas**.  
- Include full serialization data classes for AssemblyAI and Gemini requests/responses.  
- Avoid global variables except the profanity list.  
- Do not hardcode API keys — they must always come from flags.
- Write deterministic, testable functions.  
- Produce unit tests when feasible (JUnit 5 + MockWebServer).  
- All tests must pass prior to submission.
- Each stage MUST contain tests and they must pass.
- At the end of each stage update DOCS.md with detailed documentation how the code works - detect what had changed during current stage and add this update, update all of the documentation, verify the entire document to ensure it's up to date.
- Submit all code to git after finishing stage


FIRST ACTION
------------
Start with **STAGE 1 only**.  
Generate the folder tree and empty Kotlin files with correct package names.  
**Do not write any implementation yet.**

WAIT for my “continue” before moving to STAGE 2.