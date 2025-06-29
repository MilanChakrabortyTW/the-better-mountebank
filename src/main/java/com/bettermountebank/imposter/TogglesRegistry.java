package com.bettermountebank.imposter;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TogglesRegistry {
    private final List<Toggle> toggles = new ArrayList<>(List.of(
        new Toggle("some_feature", true),
        new Toggle("another_feature", false)
    ));

    public List<Toggle> getAllToggles() {
        return toggles;
    }

    public Toggle findToggleByName(String name) {
        return toggles.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Data
    @AllArgsConstructor
    public static class Toggle {
        private String name;
        private boolean enabled;
    }
}
