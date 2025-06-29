package com.bettermountebank.infrastructure.service;

import com.bettermountebank.model.MockConfig;
import com.bettermountebank.domain.service.MockConfigService;
import com.bettermountebank.repository.MockConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockConfigServiceImpl implements MockConfigService {
    private final MockConfigRepository mockConfigRepository;

    @Override
    public Optional<MockConfig> findByMockPrefix(String mockPrefix) {
        return mockConfigRepository.findByMockPrefix(mockPrefix);
    }

    @Override
    public MockConfig saveConfig(MockConfig config) {
        return mockConfigRepository.save(config);
    }

    @Override
    public void updateCallCount(MockConfig config, String specialName, int newCallCount) {
        if (config.getConfigs() == null || !config.getConfigs().containsKey(specialName)) {
            log.warn("Cannot update call count - specialName {} not found in config", specialName);
            return;
        }

        var endpointConfig = config.getConfigs().get(specialName);
        endpointConfig.setCallCount(newCallCount);
        config.getConfigs().put(specialName, endpointConfig);
        mockConfigRepository.save(config);
        log.debug("Updated call count for {} to {}", specialName, newCallCount);
    }
}
