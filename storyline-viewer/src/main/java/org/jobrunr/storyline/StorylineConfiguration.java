package org.jobrunr.storyline;

import org.jobrunr.storyline.api.CodeController;
import org.jobrunr.storyline.api.MobileRedirectInterceptor;
import org.jobrunr.storyline.api.StorylineController;
import org.jobrunr.storyline.api.TrialController;
import org.jobrunr.storyline.model.Storyline;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Import({StorylineController.class, CodeController.class, TrialController.class})
@AutoConfiguration
public class StorylineConfiguration {

    @Bean
    public Storyline storyline() {
        StorylineReader storylineReader = new StorylineReader();
        return storylineReader.getStoryline();
    }

    @Bean
    public WebMvcConfigurer mobileRedirectWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new MobileRedirectInterceptor())
                        .addPathPatterns("/", "/storyline", "/storyline/**");
            }
        };
    }
}
