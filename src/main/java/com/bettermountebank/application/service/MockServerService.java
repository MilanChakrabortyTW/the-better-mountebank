package com.bettermountebank.application.service;

import com.bettermountebank.model.EndpointConfig;
import com.bettermountebank.model.MockConfig;
import com.bettermountebank.model.Output;
import com.bettermountebank.domain.service.MockConfigService;
import com.bettermountebank.imposter.ImposterRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockServerService {
    private final MockConfigService mockConfigService;
    private final ImposterRegistry imposterRegistry;
    private final RestTemplate restTemplate;

    public ResponseEntity<?> processRequest(HttpServletRequest request, Map<String, String> headers, String body) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        String mockPrefixHeader = headers.getOrDefault("x-vo-ref", "");
        String mockPrefix = extractMockPrefix(mockPrefixHeader);

        log.info("Processing request path={}, method={}, mockPrefix={}", path, method, mockPrefix);

        MockConfig config = mockPrefix != null ?
                mockConfigService.findByMockPrefix(mockPrefix).orElse(null) : null;

        ImposterRegistry.Imposter imposter = imposterRegistry.findImposter(path, method);
        if (imposter == null) {
            log.warn("Imposter not found for path={}, method={}, mockPrefix={}", path, method, mockPrefix);
            return ResponseEntity.status(404).body(Map.of(
                "error", "Imposter not found for this path and method",
                "lan", headers.getOrDefault("lan", "en")
            ));
        }

        String specialName = imposter.getSpecialName();
        EndpointConfig endpointConfig = getEndpointConfig(config, specialName);

        if (endpointConfig == null) {
            log.warn("No endpoint config found for specialName={}, mockPrefix={}", specialName, mockPrefix);
            return ResponseEntity.status(404).body(Map.of(
                "error", "No endpoint configuration found for this path",
                "lan", headers.getOrDefault("lan", "en")
            ));
        }

        int callIndex = endpointConfig.getCallCount();
        Output output = getOutputForCall(endpointConfig);

        if (output == null) {
            log.warn("No output configured for specialName={}, mockPrefix={}", specialName, mockPrefix);
            return ResponseEntity.status(404).body(Map.of(
                "error", "No output configured for this endpoint",
                "lan", headers.getOrDefault("lan", "en")
            ));
        }

        mockConfigService.updateCallCount(config, specialName, callIndex + 1);
        log.debug("Updated call count for specialName={}, mockPrefix={}, newCount={}", specialName, mockPrefix, callIndex + 1);

        if (!imposter.getResponses().contains(output.getResponseType())) {
            log.warn("Invalid responseType={} for imposter={}, mockPrefix={}", output.getResponseType(), specialName, mockPrefix);
            return ResponseEntity.status(404).body(Map.of(
                    "error", "ResponseType '" + output.getResponseType() + "' not allowed for this imposter"));
        }

        if ("custom".equals(output.getResponseType())) {
            if (output.getCustomResponse() != null) {
                log.info("Serving custom response for specialName={}, mockPrefix={}", specialName, mockPrefix);
                return ResponseEntity.ok(output.getCustomResponse());
            } else {
                log.warn("Custom response is null for specialName={}, mockPrefix={}", specialName, mockPrefix);
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Configuration error",
                    "message", "Custom response type specified but customResponse is not properly configured"
                ));
            }
        }

        if ("proxy".equals(output.getResponseType())) {
            log.info("Proxying request for specialName={}, mockPrefix={}, target={}",
                    specialName, mockPrefix, imposter.getProxyTarget());
            return proxyRequest(request, headers, body, imposter.getProxyTarget());
        }

        log.info("Serving static response for specialName={}, responseType={}, mockPrefix={}",
                specialName, output.getResponseType(), mockPrefix);
        return serveStaticResponse(imposter, output);
    }

    public ResponseEntity<?> handleUnleashFeatures(Map<String, String> headers) {
        String mockPrefixHeader = headers.getOrDefault("x-vo-ref", "");
        String mockPrefix = extractMockPrefix(mockPrefixHeader);

        if (mockPrefix.isEmpty()) {
            log.info("No X-VO-REF header found, proxying to Unleash");
            return proxyToUnleash();
        }

        Optional<MockConfig> configOpt = mockConfigService.findByMockPrefix(mockPrefix);
        if (configOpt.isEmpty()) {
            log.info("No config found for mockPrefix: {}, proxying to Unleash", mockPrefix);
            return proxyToUnleash();
        }

        MockConfig config = configOpt.get();
        if (config.getToggles() == null) {
            log.info("Config found but no toggles defined, proxying to Unleash");
            return proxyToUnleash();
        }

        log.info("Returning toggles from config");
        Map<String, Object> response = new HashMap<>();
        response.put("version", 1);
        response.put("features", config.getToggles());
        return ResponseEntity.ok(response);
    }

    private String extractMockPrefix(String header) {
        if (header == null) return null;
        int idx = header.indexOf("_MOCK_");
        if (idx == -1) return null;
        int end = idx + 12;
        if (header.length() < end) return null;
        return header.substring(0, end);
    }

    private EndpointConfig getEndpointConfig(MockConfig config, String specialName) {
        if (config == null || config.getConfigs() == null) return null;
        return config.getConfigs().get(specialName);
    }

    private Output getOutputForCall(EndpointConfig endpointConfig) {
        if (endpointConfig == null || endpointConfig.getOutputs() == null || endpointConfig.getOutputs().isEmpty()) return null;
        int callIndex = endpointConfig.getCallCount();
        int outputIndex = Math.min(callIndex, endpointConfig.getOutputs().size() - 1);
        return endpointConfig.getOutputs().get(outputIndex);
    }

    private ResponseEntity<?> proxyRequest(HttpServletRequest request, Map<String, String> headers, String body, String proxyTarget) {
        try {
            String path = request.getRequestURI();
            String queryString = request.getQueryString();
            String targetUrl = UriComponentsBuilder.fromHttpUrl(proxyTarget)
                    .path(path)
                    .query(queryString)
                    .build()
                    .toUriString();

            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach((key, value) -> {
                if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) {
                    httpHeaders.add(key, value);
                }
            });

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    targetUrl,
                    method,
                    httpEntity,
                    String.class
            );

            return ResponseEntity
                    .status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(responseEntity.getBody());
        } catch (Exception e) {
            log.error("Error proxying request: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "error", "Proxy failed",
                    "message", e.getMessage(),
                    "target", proxyTarget
            ));
        }
    }

    private ResponseEntity<?> proxyToUnleash() {
        try {
            String url = "http://unleash.unleash:4242/unleash/api/client/features";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Error proxying to Unleash: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "error", "Failed to proxy to Unleash",
                    "message", e.getMessage()
            ));
        }
    }

    private ResponseEntity<?> serveStaticResponse(ImposterRegistry.Imposter imposter, Output output) throws IOException {
        String staticFile = String.format("responses/%s/%s.json", imposter.getGroup(), output.getResponseType());
        ClassPathResource resource = new ClassPathResource(staticFile);

        if (!resource.exists()) {
            log.warn("Static response file not found: {}", staticFile);
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Static response file '" + output.getResponseType() + ".json' not found for group '" + imposter.getGroup() + "'"
            ));
        }

        String json = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseNode = mapper.readTree(json);

            int status = responseNode.has("status") ? responseNode.get("status").asInt() : 200;
            JsonNode body = responseNode.has("body") ? responseNode.get("body") : mapper.createObjectNode();

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status);

            if (responseNode.has("headers")) {
                JsonNode headersNode = responseNode.get("headers");
                if (headersNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> header = fields.next();
                        responseBuilder.header(header.getKey(), header.getValue().asText());
                    }
                }
            }

            if (!responseNode.has("headers") ||
                !responseNode.get("headers").has("Content-Type")) {
                responseBuilder.contentType(MediaType.APPLICATION_JSON);
            }

            return responseBuilder.body(body);

        } catch (Exception e) {
            log.error("Error parsing static response file: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to parse static response file",
                "message", e.getMessage()
            ));
        }
    }
}
