package org.jobrunr.storyline;

import org.jobrunr.storyline.model.Category;
import org.jobrunr.storyline.model.QuickAction;
import org.jobrunr.storyline.model.Storyline;
import org.jobrunr.storyline.model.StorylineStep;
import org.jobrunr.storyline.model.TourInfo;
import org.jobrunr.storyline.model.TourInfo.TourAction;
import org.jobrunr.storyline.model.TourInfo.TourAnchor;
import org.jobrunr.storyline.model.TourIntro;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class StorylineReader {

    private static final String DASHBOARD_PATH = "/dashboard";
    private static final Set<String> TOUR_MODES = Set.of(TourInfo.MODE_DASHBOARD, TourInfo.MODE_APP, TourInfo.MODE_INFO);
    private static final Set<String> TOUR_PLACEMENTS = Set.of("auto", "top", "right", "bottom", "left");
    private static final Set<String> TOUR_MATCHES = Set.of(TourAnchor.MATCH_PATH, TourAnchor.MATCH_PREFIX);
    private static final Set<String> TOUR_METHODS = Set.of("GET", "POST");

    private final Storyline storyline;
    private final ObjectMapper yamlMapper;

    public StorylineReader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.storyline = loadStoryline();
    }

    public Storyline getStoryline() {
        return storyline;
    }

    private Storyline loadStoryline() {
        try (InputStream inputStream = new ClassPathResource("storyline/index.yaml").getInputStream()) {
            IndexYaml index = yamlMapper.readValue(inputStream, IndexYaml.class);

            SequencedMap<Category, List<StorylineStep>> stepsByCategory = new LinkedHashMap<>();
            int stepNumber = 1;

            for (CategoryFlow categoryFlow : index.flow) {
                List<StorylineStep> steps = new ArrayList<>();
                Category category = new Category(categoryFlow.category, categoryFlow.icon, categoryFlow.description);

                for (String stepPath : categoryFlow.steps) {
                    var step = loadStorylineStep(stepPath, categoryFlow.category, stepNumber++);
                    steps.add(step);
                }

                stepsByCategory.put(category, steps);
            }

            return new Storyline(
                    index.title,
                    index.icon,
                    index.subtitle,
                    index.slogan,
                    index.intro,
                    index.guideIntro,
                    Optional.ofNullable(index.codeRoot).orElse("src/main/java"),
                    index.githubLink,
                    tourIntro(index.tour),
                    stepsByCategory
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load storyline", e);
        }
    }

    private StepYaml loadStepYaml(String stepPath) {
        String cleanPath = stepPath.startsWith("./") ? stepPath.substring(2) : stepPath;
        String fullPath = "storyline/" + cleanPath;

        try (InputStream inputStream = new ClassPathResource(fullPath).getInputStream()) {
            return yamlMapper.readValue(inputStream, StepYaml.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load step file: " + fullPath, e);
        }
    }

    private StorylineStep loadStorylineStep(String stepPath, String category, int stepNumber) {
        StepYaml stepYaml = loadStepYaml(stepPath);
        return new StorylineStep(
            stepNumber,
            stepYaml.title,
            stepYaml.icon,
            stepYaml.estimatedTime,
            category,
            stepYaml.scenario,
            stepYaml.challenge,
            stepYaml.solution,
            stepYaml.tryIt,
            codeReferences(stepYaml),
            stepYaml.tryItUrl,
            quickActions(stepYaml),
            stepYaml.dashboardUrl,
            stepYaml.videoUrl,
            stepYaml.learnMore,
            stepYaml.liveNotice,
            tourInfo(stepYaml, stepPath)
        );
    }

    private List<String> codeReferences(StepYaml stepYaml) {
        if(stepYaml.codeReferences != null) return stepYaml.codeReferences;
        if(stepYaml.codeReference != null) return List.of(stepYaml.codeReference);
        return List.of();
    }

    private List<QuickAction> quickActions(StepYaml stepYaml) {
        if(stepYaml.quickActions != null) return stepYaml.quickActions;
        if(stepYaml.quickAction != null) return List.of(stepYaml.quickAction);
        return List.of();
    }

    private TourIntro tourIntro(TourIntroYaml intro) {
        if (intro == null) return null;
        return new TourIntro(intro.headline, intro.body, intro.cta);
    }

    /**
     * Builds the tour view of a step. Every field falls back to something sensible so a step that has
     * no {@code tour:} block yet still narrates (its challenge) and still knows where to stand
     * (the dashboard URL the guide already uses).
     */
    private TourInfo tourInfo(StepYaml step, String stepPath) {
        TourYaml tour = Optional.ofNullable(step.tour).orElseGet(TourYaml::new);
        String mode = oneOf(tour.mode, defaultMode(step), TOUR_MODES, "mode", stepPath);
        return new TourInfo(
                Optional.ofNullable(tour.narration).orElse(step.challenge),
                TourInfo.MODE_INFO.equals(mode) ? null : tourAnchor(tour.anchor, step, stepPath),
                oneOf(tour.placement, "auto", TOUR_PLACEMENTS, "placement", stepPath),
                mode,
                tourActions(tour.actions, stepPath),
                tour.screenshot,
                Boolean.TRUE.equals(tour.skip));
    }

    private static String defaultMode(StepYaml step) {
        boolean onDashboard = step.dashboardUrl != null && step.dashboardUrl.startsWith(DASHBOARD_PATH);
        return onDashboard ? TourInfo.MODE_DASHBOARD : TourInfo.MODE_INFO;
    }

    private static TourAnchor tourAnchor(AnchorYaml anchor, StepYaml step, String stepPath) {
        AnchorYaml yaml = Optional.ofNullable(anchor).orElseGet(AnchorYaml::new);
        String page = Optional.ofNullable(yaml.page).orElse(step.dashboardUrl);
        if (page == null || !page.startsWith(DASHBOARD_PATH)) {
            throw new IllegalStateException("Tour anchor of " + stepPath + " needs a dashboard page, but got: " + page);
        }
        return new TourAnchor(page, yaml.selector, yaml.fallbackSelector, yaml.reveal,
                oneOf(yaml.match, TourAnchor.MATCH_PATH, TOUR_MATCHES, "anchor.match", stepPath));
    }

    private static List<TourAction> tourActions(List<ActionYaml> actions, String stepPath) {
        if (actions == null) return emptyList();
        return actions.stream()
                .map(action -> new TourAction(
                        action.label,
                        action.url,
                        httpMethod(action.method, stepPath),
                        Boolean.TRUE.equals(action.anonymous)))
                .toList();
    }

    private static String httpMethod(String value, String stepPath) {
        String method = isBlank(value) ? "GET" : value.trim().toUpperCase(Locale.ROOT);
        return verified(method, TOUR_METHODS, "action.method", stepPath);
    }

    private static String oneOf(String value, String fallback, Set<String> allowed, String field, String stepPath) {
        String normalized = isBlank(value) ? fallback : value.trim().toLowerCase(Locale.ROOT);
        return verified(normalized, allowed, field, stepPath);
    }

    private static String verified(String value, Set<String> allowed, String field, String stepPath) {
        if (!allowed.contains(value)) {
            throw new IllegalStateException("Tour " + field + " of " + stepPath + " is '" + value + "', expected one of " + allowed);
        }
        return value;
    }

    // DTOs for YAML deserialization
    private static class IndexYaml {
        public String title;
        public String icon;
        public String subtitle;
        public String slogan;
        public String intro;
        public String guideIntro;
        public String codeRoot;
        public String githubLink;
        public TourIntroYaml tour;
        public List<CategoryFlow> flow;
    }

    private static class TourIntroYaml {
        public String headline;
        public String body;
        public String cta;
    }

    private static class CategoryFlow {
        public String category;
        public String icon;
        public String description;
        public List<String> steps;
    }

    private static class StepYaml {
        public String title;
        public String icon;
        public Duration estimatedTime;
        public String scenario;
        public String challenge;
        public String solution;
        public String tryIt;
        public String codeReference;
        public List<String> codeReferences;
        public String tryItUrl;
        public QuickAction quickAction;
        public List<QuickAction> quickActions;
        public String dashboardUrl;
        public String videoUrl;
        public String learnMore;
        public String liveNotice;
        public TourYaml tour;
    }

    private static class TourYaml {
        public String narration;
        public AnchorYaml anchor;
        public String placement;
        public String mode;
        public String screenshot;
        public List<ActionYaml> actions;
        public Boolean skip;
    }

    private static class AnchorYaml {
        public String page;
        public String selector;
        public String fallbackSelector;
        public String reveal;
        public String match;
    }

    private static class ActionYaml {
        public String label;
        public String url;
        public String method;
        public Boolean anonymous;
    }
}
