### Multi-stage build to create a slim runtime image with ffmpeg and whisper-jni dependencies.
# Builder: compile and assemble distribution
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Install build-time tools
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

# Copy entire project (ensuring cache use on reorder-sensitive layers)
COPY . .

# Build the installable distribution
RUN ./gradlew :app:installDist --no-daemon

# Runtime: minimal JRE with ffmpeg
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

ENV APP_HOME=/opt/app \
    WHISPER_MODEL_PATH=/models/ggml-model.bin \
    FFMPEG_CMD=ffmpeg

WORKDIR $APP_HOME

# Copy distribution from builder
COPY --from=builder /workspace/app/build/install/app/ ${APP_HOME}/

# Default entrypoint points to the assembled application; CLI args come from CMD/compose
ENTRYPOINT ["./bin/app"]
CMD []
