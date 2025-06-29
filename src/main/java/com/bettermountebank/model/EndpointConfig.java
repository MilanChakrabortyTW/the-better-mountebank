package com.bettermountebank.model;

import lombok.Data;
import java.util.List;

@Data
public class EndpointConfig {
    private String specialName;
    private List<Output> outputs;
    private int callCount = 0;
}

