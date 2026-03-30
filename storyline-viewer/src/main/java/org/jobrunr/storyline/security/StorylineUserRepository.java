package org.jobrunr.storyline.security;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface StorylineUserRepository extends CrudRepository<StorylineUser, Long> {

    Optional<StorylineUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
