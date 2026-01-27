package org.jobrunr.storyline.model;

import java.time.Duration;
import java.util.List;
import java.util.SequencedMap;

public record Storyline(
    String title,
    String subTitle,
    String slogan,
    String intro,
    String codeRoot,
    String githubLink,
    SequencedMap<String, List<StorylineStep>> stepsByCategory
) {

    public StorylineMetadata getStorylineMetadata() {
        int totalSteps = 0;
        Duration estimatedTime = Duration.ZERO;
        for (var storyLineSteps : stepsByCategory.values()) {
            for (var step : storyLineSteps) {
                totalSteps = totalSteps + 1;
                estimatedTime = estimatedTime.plus(step.estimatedTime());
            }
        }

        return new StorylineMetadata(totalSteps, estimatedTime);
    }

    public record StorylineMetadata(int totalAmountOfSteps, Duration estimatedTime) {}
}
