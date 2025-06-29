package com.bettermountebank.controller;

import com.bettermountebank.application.service.ConfigApplicationService;
import com.bettermountebank.domain.model.ConfigCreationResult;
import com.bettermountebank.model.EndpointConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Tag(name = "Config API", description = "Endpoints for managing mock configs")
@Slf4j
public class ConfigController {
    private final ConfigApplicationService configService;

    @Operation(summary = "Create a new mock config and generate a new mockPrefix")
    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody ConfigRequest request) {
        log.info("Creating new configuration");
        ConfigCreationResult result = configService.createConfig(request.getConfigs(), request.getToggles());
        return ResponseEntity.ok().body(Map.of("mockPrefix", result.getMockPrefix()));
    }

    @Operation(summary = "Update an existing mock config by mockPrefix")
    @PutMapping("/{mockPrefix}")
    public ResponseEntity<?> updateConfig(@PathVariable String mockPrefix, @RequestBody ConfigRequest request) {
        log.info("Updating configuration with mockPrefix: {}", mockPrefix);
        Optional<ConfigCreationResult> result = configService.updateConfig(mockPrefix, request.getConfigs(), request.getToggles());

        if (result.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Config not found for mockPrefix"));
        }

        return ResponseEntity.ok().body(Map.of("mockPrefix", result.get().getMockPrefix()));
    }

    @Data
    public static class ConfigRequest {
        private Map<String, EndpointConfig> configs;
        private List<Object> toggles;
    }
}
