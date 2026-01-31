FROM gradle:9.3.1-jdk25-alpine AS build
ARG TAILWINDCSS_VERSION=4.1.18
WORKDIR /code

# Build configuration (changes rarely — cached layer for dependency resolution)
COPY settings.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY buildSrc/ buildSrc/
COPY app-server/build.gradle.kts app-server/
COPY ui-library/build.gradle.kts ui-library/

# Source code (changes often)
COPY app-server/src/main/ app-server/src/main/
COPY ui-library/src/main/ ui-library/src/main/

RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "aarch64" ]; then ARCH="arm64"; elif [ "$ARCH" = "x86_64" ]; then ARCH="x64"; fi && \
    wget -qO /usr/local/bin/tailwindcss \
    "https://github.com/tailwindlabs/tailwindcss/releases/download/v${TAILWINDCSS_VERSION}/tailwindcss-linux-${ARCH}-musl" && \
    chmod +x /usr/local/bin/tailwindcss

RUN --mount=type=cache,target=/home/gradle/.gradle \
    --mount=type=cache,target=/code/.gradle \
    --mount=type=cache,target=/code/build \
    --mount=type=cache,target=/code/buildSrc/build \
    --mount=type=cache,target=/code/buildSrc/.gradle \
    --mount=type=cache,target=/code/app-server/.gradle \
    --mount=type=cache,target=/code/app-server/build \
    --mount=type=cache,target=/code/ui-library/.gradle \
    --mount=type=cache,target=/code/ui-library/build \
    gradle :app-server:installDist -x check --no-daemon && \
    cp -R /code/app-server/build/install/app-server /code/built

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /code/built/ .
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1
ENTRYPOINT ["bin/app-server"]
