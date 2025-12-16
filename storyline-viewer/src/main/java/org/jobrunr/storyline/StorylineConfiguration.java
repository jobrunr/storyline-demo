package org.jobrunr.storyline;

import org.jobrunr.storyline.api.CodeController;
import org.jobrunr.storyline.api.StorylineController;
import org.jobrunr.storyline.model.Storyline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({StorylineController.class, CodeController.class})
public class StorylineConfiguration {

    @Bean
    public Storyline storyline() {
        StorylineReader storylineReader = new StorylineReader();
        return storylineReader.getStoryline();
    }
}
