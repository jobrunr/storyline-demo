package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.jobrunr.scheduling.JobBuilder.anExternalJob;
import static org.jobrunr.storage.JobSearchRequestBuilder.aJobSearchRequest;

/**
 * Step 18: the fraud model runs on a GPU cluster outside this JVM and takes minutes. An external job
 * keeps that work on the dashboard, with its retries, timeout and result, without a worker thread
 * sitting there waiting for it.
 */
@Service
public class FraudReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FraudReviewService.class);

    private final JobScheduler jobScheduler;
    private final StorageProvider storageProvider;

    public FraudReviewService(JobScheduler jobScheduler, StorageProvider storageProvider) {
        this.jobScheduler = jobScheduler;
        this.storageProvider = storageProvider;
    }

    public UUID requestReview(CreditCard creditCard) {
        return jobScheduler.create(anExternalJob()
                .withName("Fraud review of " + creditCard.getEmail())
                .withLabels("customer: " + creditCard.getEmail())
                // Nothing is lost if the cluster never reports back: the job fails by itself
                .withProcessTimeOut(Duration.ofMinutes(30))
                .withJobLambda(() -> handOverToRiskCluster(creditCard))).asUUID();
    }

    public void handOverToRiskCluster(CreditCard creditCard) {
        // In production this posts the case to the GPU cluster and returns; JobRunr parks the job in
        // PROCESSED until the cluster reports back, instead of moving it straight to SUCCEEDED
        LOGGER.info("Handed application of {} to the risk cluster", creditCard.getEmail());
    }

    /** What the cluster's webhook looks up before it reports its verdict. */
    public Optional<UUID> reviewWaitingForVerdict() {
        return storageProvider
                .getJobList(aJobSearchRequest(StateName.PROCESSED).build(), new AmountRequest("updatedAt:ASC", 1))
                .stream()
                .findFirst()
                .map(Job::getId);
    }

    public void verdictReceived(UUID jobId, boolean approved) {
        if (approved) {
            jobScheduler.signalExternalJobSucceeded(jobId, new FraudVerdict("APPROVED", "fraud-model-v4", 0.02));
        } else {
            jobScheduler.signalExternalJobFailed(jobId, "fraud-model-v4 flagged this application");
        }
    }

    public record FraudVerdict(String decision, String model, double riskScore) {
    }
}
