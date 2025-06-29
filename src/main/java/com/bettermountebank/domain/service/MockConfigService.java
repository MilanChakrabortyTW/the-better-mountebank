package com.bettermountebank.domain.service;

import com.bettermountebank.model.MockConfig;
import java.util.Optional;
import java.util.Map;


public interface MockConfigService {

    Optional<MockConfig> findByMockPrefix(String mockPrefix);

    MockConfig saveConfig(MockConfig config);

    void updateCallCount(MockConfig config, String specialName, int newCallCount);
}
