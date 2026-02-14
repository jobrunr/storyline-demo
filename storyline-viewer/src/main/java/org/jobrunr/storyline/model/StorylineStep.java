package org.jobrunr.storyline.model;

import java.time.Duration;
import java.util.List;

public record StorylineStep(int number,
    String title,
    String icon,
    Duration estimatedTime,
    String category,
    String scenario,
    String challenge,
    String solution,
    String tryIt,
    List<String> codeReferences,
    String tryItUrl,
    String dashboardUrl,
    String videoUrl,
    String learnMore) {

}
