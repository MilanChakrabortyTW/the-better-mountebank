package com.bettermountebank.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnleashFeature {
    private String name;
    private boolean enabled;

    @Builder.Default
    private List<Strategy> strategies = new ArrayList<>();

    @Builder.Default
    private List<Object> variants = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Strategy {
        private String name;
        private Map<String, Object> parameters;
    }
}
