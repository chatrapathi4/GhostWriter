package com.ghostwriter.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByGithubId(String githubId);

    Optional<User> findByUsername(String username);
}
