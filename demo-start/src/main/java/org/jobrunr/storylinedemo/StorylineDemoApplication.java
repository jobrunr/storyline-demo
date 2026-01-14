package org.jobrunr.storylinedemo;

import jakarta.servlet.Filter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@SpringBootApplication
public class StorylineDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorylineDemoApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<?> registerFilters() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
