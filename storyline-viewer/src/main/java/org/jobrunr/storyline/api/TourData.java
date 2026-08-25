package org.jobrunr.storyline.api;

import org.jobrunr.storyline.model.Category;
import org.jobrunr.storyline.model.Storyline;
import org.jobrunr.storyline.model.StorylineStep;
import org.jobrunr.storyline.model.TourInfo;
import org.jobrunr.storyline.model.TourInfo.TourAction;
import org.jobrunr.storyline.model.TourInfo.TourAnchor;
import org.jobrunr.storyline.model.TourIntro;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole tour, flattened into the shape the overlay engine wants: one act list, one step list,
 * every step already knowing which act it belongs to. Serialized once at startup and embedded in
 * tour.peb, so the browser needs no extra round-trip to know the entire storyline.
 */
public record TourData(
        String title,
        String icon,
        TourIntro intro,
        int totalSteps,
        List<Act> acts,
        List<Step> steps) {

    public record Act(int index, String name, String icon, String description, int firstStep, int lastStep) {
    }

    public record Step(
            int number,
            String title,
            int act,
            String narration,
            TourAnchor anchor,
            String mode,
            String placement,
            List<TourAction> actions,
            List<String> codeReferences,
            String learnMore,
            String tryItUrl,
            String externalUrl,
            String screenshot,
            String liveNotice) {
    }

    /**
     * A step whose tour block says {@code skip: true} sits the tour out, so the tour numbers its
     * steps itself: always 1..n with no holes, even though the written guide keeps its own numbering.
     */
    public static TourData from(Storyline storyline) {
        List<Act> acts = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        int number = 1;

        for (Category category : storyline.stepsByCategory().keySet()) {
            List<StorylineStep> kept = storyline.stepsByCategory().get(category).stream()
                    .filter(step -> !step.tour().skip())
                    .toList();
            if (kept.isEmpty()) continue;

            int actIndex = acts.size();
            int firstStep = number;
            for (StorylineStep step : kept) {
                steps.add(toStep(step, actIndex, number++));
            }
            acts.add(new Act(actIndex,
                    category.name(),
                    category.icon(),
                    category.description(),
                    firstStep,
                    number - 1));
        }

        return new TourData(storyline.title(), storyline.icon(), storyline.tourIntro(), steps.size(), acts, steps);
    }

    private static Step toStep(StorylineStep step, int actIndex, int number) {
        TourInfo tour = step.tour();
        return new Step(
                number,
                step.title(),
                actIndex,
                tour.narration(),
                tour.anchor(),
                tour.mode(),
                tour.placement(),
                tour.actions(),
                step.codeReferences(),
                step.learnMore(),
                step.tryItUrl(),
                externalUrl(step),
                tour.screenshot(),
                step.liveNotice());
    }

    /**
     * Steps 19 and 20 point at Prometheus and Jaeger, which only exist when you run the demo
     * yourself. The tour offers the link locally and a still of what you would have seen live.
     */
    private static String externalUrl(StorylineStep step) {
        String dashboardUrl = step.dashboardUrl();
        return dashboardUrl != null && !dashboardUrl.startsWith("/") ? dashboardUrl : null;
    }
}
