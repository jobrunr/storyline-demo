package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/credit-score")
public class CreditScoreController {

    private final CreditScoreService creditScoreService;
    private final JobScheduler jobScheduler;

    public CreditScoreController(CreditScoreService creditScoreService, JobScheduler jobScheduler) {
        this.creditScoreService = creditScoreService;
        this.jobScheduler = jobScheduler;
    }

    @GetMapping("/request/{customerId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestCreditScore(@PathVariable String customerId) {
        UUID jobId = creditScoreService.requestCreditScoreCalculation(customerId);
        return ResponseEntity.accepted().body(Map.of(
            "message", "Credit score calculation started",
            "jobId", jobId.toString(),
            "pollUrl", "/credit-score/result/" + jobId
        ));
    }

    @GetMapping("/result/{jobId}")
    @ResponseBody
    public ResponseEntity<?> getCreditScoreResult(@PathVariable UUID jobId) {
        // Step 17B: The client is asking for the result, return it if it's available, otherwise let them know to wait!
        var result = jobScheduler.getJobResult(jobId);

        if (result.isAvailable()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.accepted().body(Map.of(
                "status", "processing",
                "message", "Credit score calculation in progress",
                "retryAfterSeconds", result.backOffPeriod().toSeconds()
            ));
        }
    }
}
