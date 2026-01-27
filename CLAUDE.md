# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an interactive JobRunr Pro demo application called "JobRunr Finance" - a Spring Boot multi-module project demonstrating background job processing patterns through a banking storyline with 20 hands-on steps.

## Build Commands

```bash
# Start infrastructure (PostgreSQL, Prometheus, Jaeger)
docker compose up

# Run the main demo application (port 8080)
./gradlew :demo-solution:bootRun

# Run the starter template for exercises
./gradlew :demo-start:bootRun

# Run the government mock API (port 8089)
./gradlew :government-app:bootRun

# Run a second background server with international tag (port 8081)
./gradlew :demo-solution:bootRun --args='--server.port=8081 --jobrunr.dashboard.enabled=false --jobrunr.background-job-server.tags=international'

# Build all modules
./gradlew build
```

## Architecture

**Multi-module Gradle project** (Java 25, Spring Boot 4.0.0):

- **demo-solution** - Complete implementation with all 20 JobRunr Pro steps
- **demo-start** - Skeleton for implementing features yourself
- **government-app** - Mock external API for rate limiting and tracing demos (Spring Boot 3.5.6)
- **storyline-viewer** - Interactive web guide with HTMX/Pebble templates (included as dependency in demo apps)

**Domain structure in demo-solution:**
- `creditcards/` - Credit card registration, activation, statements, credit score services
- `creditcards/events/` - Spring application events for job scheduling coordination
- `payment/` - Payment processing with customer types (PRO/ENTERPRISE)
- `AdminController` - API endpoints for triggering bulk operations and demos

**Key integrations:**
- JobRunr Pro with PostgreSQL storage
- Micrometer metrics with Prometheus
- OpenTelemetry tracing with Jaeger
- Spring Data JDBC for persistence

## Configuration

Required credentials in `gradle.properties`:
```properties
jobRunrRepoUser=yourUserName
jobRunrRepoPassword=yourPassword
```

License key: Place `jobrunr-pro.license` in `src/main/resources` or set `JOBRUNR_PRO_LICENSE` environment variable.

## Code Style

- Keep code to the essential - avoid over-engineering
- Minimize comments - code should be self-explanatory

## Service URLs (when running)

| Service | URL |
|---------|-----|
| Web App | http://localhost:8080/ |
| JobRunr Dashboard | http://localhost:8080/dashboard |
| Prometheus | http://localhost:9090/ |
| Jaeger | http://localhost:16686/ |
| Government API | http://localhost:8089/ |
