package org.jobrunr.storyline.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("storyline_users")
public record StorylineUser(
        @Id Long id,
        String email,
        String name,
        String company,
        LocalDateTime createdAt) {

    public static StorylineUser newUser(String email, String name, String company) {
        return new StorylineUser(null, email, name, company, LocalDateTime.now());
    }
}
