package com.bettermountebank.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigCreationResult {
    private final String mockPrefix;
}
