package com.bettermountebank.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Document(collection = "mockconfigs")
public class MockConfig {
    @Id
    private String id;
    private String mockPrefix;
    private Map<String, EndpointConfig> configs;
    private List<Object> toggles;
}
