package org.jobrunr.storylinedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcRepositories(basePackages = {"org.jobrunr.storylinedemo", "org.jobrunr.storyline.security"})
public class StorylineDemoApplication {

    public static void main(String[] args) {

        SpringApplication.run(StorylineDemoApplication.class, args);
    }

}
