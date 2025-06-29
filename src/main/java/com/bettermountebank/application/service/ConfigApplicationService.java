package com.bettermountebank.application.service;

import com.bettermountebank.domain.model.ConfigCreationResult;
import com.bettermountebank.model.EndpointConfig;
import com.bettermountebank.model.MockConfig;
import com.bettermountebank.domain.service.CounterService;
import com.bettermountebank.domain.service.MockConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigApplicationService {
    private final MockConfigService mockConfigService;
    private final CounterService counterService;

    public ConfigCreationResult createConfig(Map<String, EndpointConfig> configs, List<Object> toggles) {
        String mockPrefix = counterService.generateMockPrefix("mockPrefix");
        log.info("Generated new mockPrefix: {}", mockPrefix);

        MockConfig config = MockConfig.builder()
                .mockPrefix(mockPrefix)
                .configs(configs)
                .toggles(toggles)
                .build();
        mockConfigService.saveConfig(config);
        log.info("Created new configuration with mockPrefix: {}", mockPrefix);

        return ConfigCreationResult.builder().mockPrefix(mockPrefix).build();
    }

    public Optional<ConfigCreationResult> updateConfig(String mockPrefix, Map<String, EndpointConfig> configs, List<Object> toggles) {
        Optional<MockConfig> existingConfig = mockConfigService.findByMockPrefix(mockPrefix);
        if (existingConfig.isEmpty()) {
            log.warn("Could not find config with mockPrefix: {}", mockPrefix);
            return Optional.empty();
        }

        MockConfig config = existingConfig.get();
        config.setConfigs(configs);
        config.setToggles(toggles);
        mockConfigService.saveConfig(config);
        log.info("Updated configuration with mockPrefix: {}", mockPrefix);

        return Optional.of(ConfigCreationResult.builder().mockPrefix(mockPrefix).build());
    }
}
