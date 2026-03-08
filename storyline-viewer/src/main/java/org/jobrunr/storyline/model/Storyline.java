package org.jobrunr.storyline.model;

import java.time.Duration;
import java.util.List;
import java.util.SequencedMap;

public record Storyline(
    String title,
    String icon,
    String subTitle,
    String slogan,
    String intro,
    String codeRoot,
    String githubLink,
    SequencedMap<Category, List<StorylineStep>> stepsByCategory
) {

    public StorylineStep getStep(int stepNumber) {
        for (var storyLineSteps : stepsByCategory.values()) {
            for (var step : storyLineSteps) {
                if (step.number() == stepNumber) {
                    return step;
                }
            }
        }
        return null;
    }

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
