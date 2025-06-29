package com.bettermountebank.controller;

import com.bettermountebank.application.service.ConfigApplicationService;
import com.bettermountebank.domain.model.ConfigCreationResult;
import com.bettermountebank.imposter.ImposterRegistry;
import com.bettermountebank.imposter.TogglesRegistry;
import com.bettermountebank.model.EndpointConfig;
import com.bettermountebank.model.Output;
import com.bettermountebank.model.UnleashFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ui")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UIController {
    private final ImposterRegistry imposterRegistry;
    private final TogglesRegistry togglesRegistry;
    private final ConfigApplicationService configService;

    @GetMapping("/imposters")
    public ResponseEntity<?> getAllImposters() {
        List<Map<String, Object>> imposters = imposterRegistry.getImposters().stream()
                .map(imposter -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("specialName", imposter.getSpecialName());
                    map.put("group", imposter.getGroup());
                    map.put("responses", imposter.getResponses());
                    map.put("method", imposter.getMethod() != null ? imposter.getMethod().toString() : "ANY");
                    map.put("pattern", imposter.getRegex().pattern());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(imposters);
    }

    @GetMapping("/toggles")
    public ResponseEntity<?> getAllToggles() {
        List<Map<String, Object>> toggles = togglesRegistry.getAllToggles().stream()
                .map(toggle -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", toggle.getName());
                    map.put("enabled", toggle.isEnabled());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(toggles);
    }

    @PostMapping("/config")
    public ResponseEntity<?> createConfig(@RequestBody ConfigRequest request) {
        Map<String, EndpointConfig> configs = new HashMap<>();

        for (Map.Entry<String, List<ResponseConfig>> entry : request.getEndpointConfigs().entrySet()) {
            String specialName = entry.getKey();
            List<Output> outputs = entry.getValue().stream()
                    .map(responseConfig -> {
                        Output output = new Output();
                        output.setResponseType(responseConfig.getType());
                        output.setCustomResponse(responseConfig.getCustomResponse());
                        return output;
                    })
                    .collect(Collectors.toList());

            EndpointConfig config = new EndpointConfig();
            config.setCallCount(0);
            config.setOutputs(outputs);
            configs.put(specialName, config);
        }

        List<UnleashFeature> toggles = request.getToggles().entrySet().stream()
                .map(entry -> {
                    UnleashFeature feature = new UnleashFeature();
                    feature.setName(entry.getKey());
                    feature.setEnabled(entry.getValue());
                    feature.setStrategies(Collections.singletonList(
                            new UnleashFeature.Strategy("default", Collections.emptyMap())
                    ));
                    feature.setVariants(Collections.emptyList());
                    return feature;
                })
                .collect(Collectors.toList());

        ConfigCreationResult result = configService.createConfig(configs, toggles);
        return ResponseEntity.ok().body(Map.of("mockPrefix", result.getMockPrefix()));
    }

    public static class ConfigRequest {
        private Map<String, List<ResponseConfig>> endpointConfigs = new HashMap<>();
        private Map<String, Boolean> toggles = new HashMap<>();

        public Map<String, List<ResponseConfig>> getEndpointConfigs() {
            return endpointConfigs;
        }

        public void setEndpointConfigs(Map<String, List<ResponseConfig>> endpointConfigs) {
            this.endpointConfigs = endpointConfigs;
        }

        public Map<String, Boolean> getToggles() {
            return toggles;
        }

        public void setToggles(Map<String, Boolean> toggles) {
            this.toggles = toggles;
        }
    }

    public static class ResponseConfig {
        private String type;
        private Object customResponse;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getCustomResponse() {
            return customResponse;
        }

        public void setCustomResponse(Object customResponse) {
            this.customResponse = customResponse;
        }
    }
}
