package com.controlleads.ping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fase 0 dummy endpoint — validates the OpenAPI → TS/Dart client pipeline
 * end to end (web and app must both render this payload).
 */
@RestController
@Tag(name = "ping")
public class PingController {

    public record PingResponse(String service, String status, Instant serverTime) {}

    @Operation(summary = "Health ping used to validate the generated API clients")
    @GetMapping("/api/ping")
    public PingResponse ping() {
        return new PingResponse("controlleads", "ok", Instant.now());
    }
}
