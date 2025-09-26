# JobRunr Finance Storyline Demo

In this demo, we gradually explore the capabilities of JobRunr Pro by following these steps:

1. Create 2 **basic jobs**: (a) creation of the credit card, (b) Schedule a future job in a week: a reminder email to confirm receival. 
2. **Recurring Jobs**: reports of expenses need to be generated each month. Simulate PDF generation using `Thread.sleep()`. Cron `0 0 1 * *` = "at 00:00 on day-of-month 1".
3. **Batches**. We after successful generation of expense reports, generate/send a summary report (another job).
4. Sometimes PDF generation **fails**. Showcase automatic retries & what happens when a batch job throws a `RuntimeException`.
5. Add **job labels** per credit card type to more easily filter on the dashboard. Showcase filtering on name.
6. Show the **Pro Dashboard** to inspect which credit cards are created, which jobs failed, various filters, ...
7. **High-prio jobs**: payment should have priority over monthly report generation . Create `high-prio,low-prio` queues. Trigger 1000 expenses and showcase the high-prios still get done.
8. **Queues**: Well-paying `ENTERPRISE` customers should have priority over `PRO` customers. Create a `customer:` prefix and add `CustomerType.name()` as the label suffix. Switch to _weighted round-robin_ to showcase the difference.
9. International payments should be processed on **another server** using server tags. (To start the second server, see the Gradle command below.) 
10. International payments should also be exported to an external system that has to be **rate-limited** to avoid DDoSing their system.

The project contains two subprojects:

- `demo-solution`; the implemented version
- `demo-start`; the version without any JobRunr specifics

## Adding JobRunr credentials

You will need to enter your JobRunr credentials in two ways:

**Private Maven repository credentials**

Create `gradle.properties` in the root folder of this project with the following contents:

```
PRIVATE_MAVEN_REPO_URL=https://repo.jobrunr.io
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8 --add-opens=java.base/java.io=ALL-UNNAMED
org.gradle.caching=true

mavenUser=yourUserName
mavenPass=yourPassword
```

**JobRunr Pro license key**

Create `jobrunr-pro.license` in `src/main/resources` of each of the subprojects and paste in your license key.

## Starting the application

1. Start the database container: `docker compose up`
2. Start the Spring Boot container: run `StorylineDemoApplication` or use Gradle: `./gradlew :demo-solution:bootRun`.
3. Navigate to http://localhost:8080/.

To run the second background server, override these properties: 
`server.port`, `jobrunr.dashboard.enabled`, and set the correct server tags with `jobrunr.background-job-server.tags`:

```
./gradlew :demo-start:bootRun --args='--server.port=8081 --jobrunr.dashboard.enabled=false --jobrunr.background-job-server.tags=international'
./gradlew :demo-solution:bootRun --args='--server.port=8081 --jobrunr.dashboard.enabled=false --jobrunr.background-job-server.tags=international'
```

# TODOs

- [ ] pdf die soms faalt: in batch faalt altijd de hele batch
