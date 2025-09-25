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

