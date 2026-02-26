package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Sample API endpoint for testing rate limiting.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "status", "ok",
            "timestamp", Instant.now().toString()
        );
    }
}
