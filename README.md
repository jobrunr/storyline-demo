# JobRunr Finance — A Day in the Life of Background Job Processing 🏦

Welcome to **JobRunr Finance**, an interactive demo where you'll experience real-world challenges that arise throughout a day at a fictional bank, and discover how JobRunr Pro handles each one with elegance.

## The Story

It's Monday morning at JobRunr Finance. Thousands of customers are waiting for their credit cards, payments need processing, and regulators are watching every move. Through **21 hands-on steps across 5 acts**, you'll solve critical problems from processing a flood of applications at 8 AM to debugging cross-border payments at midnight.

## The 21 Steps

### 🚀 Act 1: Getting Started
*Customers are flooding the website. Time to process applications at scale.*

| Step | Title | Feature |
|------|-------|---------|
| 1 | Customer Onboarding | `enqueue()` — Background job basics |
| 2 | Gentle Reminders | `schedule()` + `delete()` — Cancel when circumstances change |
| 3 | Month-End Reports | `@Recurring` + Advanced CRON — Business day support |
| 4 | Dashboard First Look | Pro Dashboard — Real-time monitoring |

### 🔧 Act 2: Handling Failures
*The first problems start appearing. How do we handle failures gracefully?*

| Step | Title | Feature |
|------|-------|---------|
| 5 | The PDF Printer Jam | Automatic retries with exponential backoff |
| 6 | Don't Charge Twice! | `runStepOnce()` — Idempotent job processing |
| 7 | Batch Processing | Batches + `continueWith()` — Coordinated workflows |
| 8 | Alert the Team | `onFailure()` chains — Error notifications |
| 9 | Finding the Needle | Labels — Filter 100K jobs instantly |

### ⚡️ Act 3: Priority & Concurrency
*Volume is spiking. Some jobs are more important than others.*

| Step | Title | Feature |
|------|-------|---------|
| 10 | VIP Treatment | Priority Queues — Critical jobs first |
| 11 | Fair Play | Weighted Round-Robin — AMEX vs MASTERCARD customers |
| 12 | One Printer, Many Jobs | Mutexes — Exclusive resource access |
| 13 | The Hung Job | Job Timeouts — Fail stuck jobs automatically |

### 🌍 Act 4: Scaling Out
*International markets are opening. Time to scale across borders.*

| Step | Title | Feature |
|------|-------|---------|
| 14 | Different Tracks | Server Tags — Route to servers with credentials |
| 15 | Go Easy on Your Partners | Rate Limiters — Throttle external API calls |
| 16 | Track Every Transaction | Progress Bars + Logging — Real-time visibility |

### 📈 Act 5: Production Ready
*Preparing for tomorrow. Monitoring, debugging, and continuous improvement.*

| Step | Title | Feature |
|------|-------|---------|
| 17 | Credit Score API | Job Results — Return data from background jobs |
| 18 | Metrics That Matter | Prometheus + Micrometer — Observability |
| 19 | Debug Like a Detective | Distributed Tracing — Jaeger integration |
| 20 | Replacing Outdated Jobs | `enqueueOrReplace()` — Update pending jobs |
| 21 | Compliance by Default | Job Filters — React to state changes without touching business code |

---

## Project Structure

The project contains three subprojects:

- **`demo-solution`** — The fully implemented version with all 21 steps
- **`demo-start`** — A skeleton version where you implement the features yourself
- **`government-app`** — A mock external API for rate limiting & tracing demos (port `8089`)
- **`storyline-viewer`** — An interactive web guide with HTMX/Pebble templates

## Getting Started

### 1. Add JobRunr Credentials

**Private Maven Repository** — Create `gradle.properties` in the project root:

```properties
jobRunrRepoUser=yourUserName
jobRunrRepoPassword=yourPassword
```

**License Key** — Create `jobrunr-pro.license` in `src/main/resources` of each subproject or make it available via the environment variable `JOBRUNR_PRO_LICENSE`.

### 2. Start the Infrastructure

```bash
docker compose up
```

This starts PostgreSQL, Prometheus, and Jaeger.

### 3. Run the Application

```bash
./gradlew :demo-solution:bootRun
```

### 4. Explore!

| Service | URL |
|---------|-----|
| Web App | http://localhost:8080/ |
| JobRunr Dashboard | http://localhost:8080/dashboard |
| Prometheus | http://localhost:9090/ |
| Jaeger | http://localhost:16686/ |
| Government API | http://localhost:8089/ |

### 5. Start the External Server (Step 14)

```bash
./gradlew :demo-solution:bootRun --args='--server.port=8081 --jobrunr.dashboard.enabled=false --jobrunr.background-job-server.tags=external'
```

---

## API Endpoints for Testing

| Endpoint | Description |
|----------|-------------|
| `/credit-cards/register` | Register a new credit card |
| `/credit-cards/activate` | Activate a credit card |
| `/bulk-add-cards` | Add 100 random credit cards |
| `/bulk-generate-expenses` | Generate 100 expense reports |
| `/bulk-generate-with-progress` | Generate with progress bar |
| `/trigger-payments` | Trigger nightly payment processing |
| `/credit-score/request/{customerId}` | Request credit score (Job Results demo) |
| `/credit-score/result/{jobId}` | Poll for credit score result |
