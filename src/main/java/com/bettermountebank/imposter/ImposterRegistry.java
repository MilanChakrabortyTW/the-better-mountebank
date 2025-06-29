package com.bettermountebank.imposter;

import lombok.AllArgsConstructor;
import lombok.*;
import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Getter
public class ImposterRegistry {
    private final List<Imposter> imposters = new ArrayList<>(List.of(
        new Imposter(
            Pattern.compile("^/api/v1/bpm$"),
            HttpMethod.POST,
            "bpm",
            List.of("success", "bad_request", "server_error", "proxy", "custom"),
            "bpm",
            "https://some-proxy"
        ),
        new Imposter(
            Pattern.compile("^/api/v1/ccm$"),
            HttpMethod.POST,
            "ccm",
            List.of("success", "bad_request", "server_error", "proxy", "custom"),
            "ccm",
            "https://some-proxy"
        )
    ));

    public Imposter findImposter(String path, String method) {
        System.out.println("Finding imposter for path: " + path + ", method: " + method);
        return imposters.stream()
                .filter(i -> {
                    boolean pathMatches = i.getRegex().matcher(path).matches();
                    boolean methodMatches = i.getMethod() == null || i.getMethod().name().equalsIgnoreCase(method);
                    System.out.println("Checking imposter: " + i + ", pathMatches: " + pathMatches + ", methodMatches: " + methodMatches);
                    return pathMatches && methodMatches;
                })
                .findFirst()
                .orElse(null);
    }

    @Data
    @AllArgsConstructor
    public static class Imposter {
        private Pattern regex;
        private HttpMethod method;
        private String specialName;
        private List<String> responses;
        private String group;
        private String proxyTarget;
    }
}
