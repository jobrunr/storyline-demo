package org.jobrunr.storylinedemo.payments;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.utils.JobUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class PaymentAuditFilter implements ApplyStateFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentAuditFilter.class);

    private static final Method PROCESS_PAYMENT_METHOD;

    static {
        try {
            PROCESS_PAYMENT_METHOD = PaymentService.class.getDeclaredMethod("processPayment", Long.class, JobContext.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        if (!PROCESS_PAYMENT_METHOD.equals(JobUtils.getJobMethod(job.getJobDetails()))) return;

        LOGGER.info("AUDIT | Payment Job{id={}, name={}} | {} → {}",
                job.getId(), job.getJobName(),
                oldState != null ? oldState.getName() : "NEW",
                newState.getName());

        if (job.hasState(StateName.FAILED)) {
            LOGGER.warn("AUDIT | Payment Job{id={}, name={}} failed permanently — compliance team notified", job.getId(), job.getJobName());
        }
    }
}
