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
9. International payments should be processed on **another server** using server tags. 
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

# FAQ

## Common errors

**Unable to determine Dialect without JDBC metadata**

Exception during startup:

```
Caused by: org.hibernate.HibernateException: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
	at org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl.determineDialect(DialectFactoryImpl.java:191) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl.buildDialect(DialectFactoryImpl.java:87) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentWithDefaults(JdbcEnvironmentInitiator.java:186) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentUsingJdbcMetadata(JdbcEnvironmentInitiator.java:410) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:129) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:81) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.boot.registry.internal.StandardServiceRegistryImpl.initiateService(StandardServiceRegistryImpl.java:130) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:263) ~[hibernate-core-6.6.29.Final.jar:6.6.29.Final]
	... 30 common frames omitted
```

Cause: did you spin up the database container using `compose.yml`?

# TODOs

- [ ] embedded dashboard? -> in comment + laten zien
- [x] 2nd bg server; hoe aanpakken -> spring params doorgeven poort overschrijven met properties; geen apart project nodig.(env vars) 
- [ ] pdf die soms faalt: in batch faalt altijd de hele batch
