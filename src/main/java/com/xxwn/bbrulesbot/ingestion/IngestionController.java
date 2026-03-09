package com.xxwn.bbrulesbot.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final RulebookIngestionService ingestionService;

    @Value("${ingestion.secret}")
    private String secret;

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
            @RequestHeader("X-Ingestion-Secret") String requestSecret) throws InterruptedException {
        if (!secret.equals(requestSecret)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        ingestionService.ingest("2025-kbo-rulebook.pdf", "KBO");
        return ResponseEntity.ok("Ingestion complete");
    }
}
