package com.bettermountebank.controller;

import com.bettermountebank.application.service.MockServerService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
@Hidden
@Slf4j
public class MockController {
    private final MockServerService mockServerService;

    @GetMapping("/unleash/api/client/features")
    public ResponseEntity<?> handleUnleashFeatures(@RequestHeader Map<String, String> headers) {
        log.debug("Handling Unleash features request");
        return mockServerService.handleUnleashFeatures(headers);
    }

    @RequestMapping("/**")
    public ResponseEntity<?> handleAll(
            HttpServletRequest request,
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String body) throws IOException {

        log.debug("Handling request: {} {}", request.getMethod(), request.getRequestURI());
        return mockServerService.processRequest(request, headers, body);
    }
}
