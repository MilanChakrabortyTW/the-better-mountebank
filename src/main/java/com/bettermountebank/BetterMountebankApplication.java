package com.bettermountebank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BetterMountebankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BetterMountebankApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MongoDBInitializer mongoDBInitializer(MongoTemplate mongoTemplate) {
        return new MongoDBInitializer(mongoTemplate);
    }

    @Slf4j
    static class MongoDBInitializer {
        private final MongoTemplate mongoTemplate;

        public MongoDBInitializer(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate;
            initializeCollections();
        }

        private void initializeCollections() {
            if (!mongoTemplate.collectionExists("mockconfigs")) {
                mongoTemplate.createCollection("mockconfigs");
                log.info("Created mockconfigs collection");
            }

            if (!mongoTemplate.collectionExists("counters")) {
                mongoTemplate.createCollection("counters");
                log.info("Created counters collection");
            }
        }
    }
}
