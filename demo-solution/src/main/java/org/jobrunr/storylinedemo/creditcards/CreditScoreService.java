package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Step 17: Job Results - Return data from background jobs
 * 
 * This service demonstrates how to use JobRunr Pro's Job Results feature
 * to return values from background jobs that can be polled by API clients.
 */
@Service
public class CreditScoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditScoreService.class);
    private final JobScheduler jobScheduler;

    public CreditScoreService(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    /**
     * Request a credit score calculation for a customer.
     * Returns the job ID that can be used to poll for the result.
     */
    public UUID requestCreditScoreCalculation(String customerId) {
        // Create a unique job ID from the customer ID
        UUID jobId = JobId.fromIdentifier("credit-score:" + customerId);
        
        // Enqueue the calculation - it returns a CreditScore result
        jobScheduler.enqueue(jobId, () -> calculateCreditScore(customerId));
        
        return jobId;
    }

    /**
     * This job calculates the credit score and RETURNS the result.
     * JobRunr serializes and stores the result automatically!
     */
    @Job(name = "Calculate Credit Score for %0")
    public CreditScore calculateCreditScore(String customerId) {
        LOGGER.info("🔄 Calculating credit score for customer: {}", customerId);
        
        // Simulate complex calculation (credit bureau calls, data analysis, etc.)
        int score = simulateCreditCardScoreCalculation();
                
        String rating = getRating(score);
        
        CreditScore result = new CreditScore(score, rating, customerId);
        LOGGER.info("✅ Credit score calculated: {}", result);
        
        // Just return the result - JobRunr stores it automatically!
        return result;
    }

    private String getRating(int score) {
        if (score >= 800) return "Exceptional";
        if (score >= 740) return "Very Good";
        if (score >= 670) return "Good";
        if (score >= 580) return "Fair";
        return "Poor";
    }

    private int simulateCreditCardScoreCalculation() {
        try {
            // Simulate calling credit bureaus, analyzing history, etc.
            Thread.sleep(new Random().nextInt(5000, 10000)); // 5-10 seconds

            // Generate a realistic-looking score
            return new Random().nextInt(300, 850); // 300-850 range
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Record class to hold credit score results.
     * This will be serialized and stored by JobRunr.
     */
    public record CreditScore(int score, String rating, String customerId) {
        @Override
        public String toString() {
            return "CreditScore{customerId='" + customerId + "', score=" + score + ", rating='" + rating + "'}";
        }
    }
}
