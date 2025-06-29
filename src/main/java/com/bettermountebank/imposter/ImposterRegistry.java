package com.bettermountebank.imposter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ImposterRegistry {
    @Getter
    private final List<Imposter> imposters = Arrays.asList(
        new Imposter(
            Pattern.compile("^/api/v1/bpm$"),
            HttpMethod.POST,
            "bpm",
            Arrays.asList("success", "proxy", "custom", "bad_request", "server_error"),
            "bpm",
            "https://some-proxy"
        )
    );

    public Imposter findImposter(String path, String method) {
        return imposters.stream()
                .filter(i -> i.getRegex().matcher(path).matches() && (i.getMethod() == null || i.getMethod().matches(method)))
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
