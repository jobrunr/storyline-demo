package org.jobrunr.storyline;

import org.jobrunr.storyline.model.Storyline;
import org.jobrunr.storyline.model.StorylineStep;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

public class StorylineReader {

    private final Storyline storyline;
    private final ObjectMapper yamlMapper;

    public StorylineReader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.storyline = loadStoryline();
    }

    private Storyline loadStoryline() {
        try (InputStream is = new ClassPathResource("storyline/index.yaml").getInputStream()) {
            IndexYaml index = yamlMapper.readValue(is, IndexYaml.class);

            SequencedMap<String, List<StorylineStep>> stepsByCategory = new LinkedHashMap<>();
            int stepNumber = 1;

            for (CategoryFlow categoryFlow : index.flow) {
                List<StorylineStep> steps = new ArrayList<>();

                for (String stepPath : categoryFlow.steps) {
                    StepYaml stepYaml = loadStepYaml(stepPath);
                    StorylineStep step = new StorylineStep(
                            stepNumber++,
                            stepYaml.title,
                            stepYaml.icon,
                            stepYaml.estimatedTime,
                            categoryFlow.category,
                            stepYaml.challenge,
                            stepYaml.solution,
                            stepYaml.tryIt,
                            stepYaml.codeReferences != null ?
                                    stepYaml.codeReferences :
                                    stepYaml.codeReference != null ? List.of(stepYaml.codeReference) : List.of(),
                            stepYaml.tryItUrl,
                            stepYaml.dashboardUrl,
                            stepYaml.videoUrl,
                            stepYaml.learnMore
                    );
                    steps.add(step);
                }

                stepsByCategory.put(categoryFlow.category, steps);
            }

            return new Storyline(
                    index.title,
                    index.subtitle,
                    index.slogan,
                    index.intro,
                    Optional.ofNullable(index.codeRoot).orElse("src/main/java"),
                    index.githubLink,
                    stepsByCategory
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load storyline", e);
        }
    }

    private StepYaml loadStepYaml(String stepPath) {
        String cleanPath = stepPath.startsWith("./") ? stepPath.substring(2) : stepPath;
        String fullPath = "storyline/" + cleanPath;

        try (InputStream is = new ClassPathResource(fullPath).getInputStream()) {
            return yamlMapper.readValue(is, StepYaml.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load step file: " + fullPath, e);
        }
    }

    public Storyline getStoryline() {
        return storyline;
    }

    // DTOs for YAML deserialization
    private static class IndexYaml {
        public String title;
        public String subtitle;
        public String slogan;
        public String intro;
        public String codeRoot;
        public String githubLink;
        public List<CategoryFlow> flow;
    }

    private static class CategoryFlow {
        public String category;
        public List<String> steps;
    }

    private static class StepYaml {
        public String title;
        public String icon;
        public Duration estimatedTime;
        public String challenge;
        public String solution;
        public String tryIt;
        public String codeReference;
        public List<String> codeReferences;
        public String tryItUrl;
        public String dashboardUrl;
        public String videoUrl;
        public String learnMore;
    }
}
