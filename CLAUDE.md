# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an interactive JobRunr Pro demo application called "JobRunr Finance" - a Spring Boot multi-module project demonstrating background job processing patterns through a banking storyline with 22 hands-on steps.

## Build Commands

Gradle 9.2.1 cannot read Java 26 class files, so build with a Java 25 JDK:
`export JAVA_HOME=$(/usr/libexec/java_home -v 25)`

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

- **demo-solution** - Complete implementation with all 22 JobRunr Pro steps
- **demo-start** - Skeleton for implementing features yourself
- **government-app** - Mock external API for rate limiting and tracing demos (Spring Boot 3.5.6)
- **storyline-viewer** - Interactive web guide with HTMX/Pebble templates (included as dependency in demo apps) and bulma for CSS

**Domain structure in demo-solution:**
- `creditcards/` - Credit card registration, activation, statements, credit score services
- `creditcards/events/` - Spring application events for job scheduling coordination
- `payment/` - Payment processing with customer types (PRO/ENTERPRISE)
- `AdminController` - API endpoints for triggering bulk operations and demos

**Two ways through the same 22 steps:**
- `/storyline` reads like a document: scenario, challenge, solution, code, with the dashboard in a tab.
- `/tour` is centered on the live dashboard: an iframe stage, a beacon pinned to a real dashboard
  element per step, and a popup that narrates it. Content comes from a `tour:` block in the same
  `storyline/steps/step-NN.yaml`; the engine is `storyline-viewer` `templates/tour.peb`,
  `static/css/tour.css` and `static/js/tour.js`.

  **Read [TOUR.md](TOUR.md) before touching anything under `/tour`.** It has the step schema, the
  invariants that are easy to break (iframe history, MUI visibility, the pill lane), the anchor sweep
  to run after a JobRunr Pro upgrade, and what is still open.

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

Magic-link sign-in needs an SMTP sink on port 1025:
`docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit` (inbox on http://localhost:8025).

Templates and static files in `storyline-viewer` do **not** hot-reload: `bootRun` consumes the module as
a jar and devtools only watches directories. Run `./gradlew :storyline-viewer:jar`, then restart the app.

## Code Style

- Keep code to the essential - avoid over-engineering
- Minimize comments - code should be self-explanatory

## Service URLs (when running)

| Service | URL |
|---------|-----|
| Web App | http://localhost:8080/ |
| Guided tour | http://localhost:8080/tour (add `?tourDebug=1` to log anchor resolution) |
| Written guide | http://localhost:8080/storyline |
| JobRunr Dashboard | http://localhost:8080/dashboard |
| Prometheus | http://localhost:9090/ |
| Jaeger | http://localhost:16686/ |
| Government API | http://localhost:8089/ |
