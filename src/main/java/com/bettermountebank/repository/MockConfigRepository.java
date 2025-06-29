package com.bettermountebank.repository;

import com.bettermountebank.model.MockConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MockConfigRepository extends MongoRepository<MockConfig, String> {
    Optional<MockConfig> findByMockPrefix(String mockPrefix);
}

