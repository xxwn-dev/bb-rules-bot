package com.xxwn.bbrulesbot.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private static final ResponseEntity<String> UNAUTHORIZED =
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

    private final RulebookIngestionService ingestionService;
    private final String secret;

    public IngestionController(
            RulebookIngestionService ingestionService,
            @Value("${ingestion.secret}") String secret) {
        this.ingestionService = ingestionService;
        this.secret = secret;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
            @RequestHeader("X-Ingestion-Secret") String requestSecret) {
        if (!isValidSecret(requestSecret)) {
            return UNAUTHORIZED;
        }
        try {
            ingestionService.ingest("2025-kbo-rulebook.pdf", "KBO");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ingestion interrupted");
        }
        return ResponseEntity.ok("Ingestion complete");
    }

    @PostMapping("/trigger-url")
    public ResponseEntity<String> triggerUrl(
            @RequestHeader("X-Ingestion-Secret") String requestSecret,
            @RequestParam String url,
            @RequestParam String league) {
        if (!isValidSecret(requestSecret)) {
            return UNAUTHORIZED;
        }
        try {
            ingestionService.ingestFromUrl(url, league);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ingestion interrupted");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("URL 접근 실패: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("URL ingestion complete");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        return UNAUTHORIZED;
    }

    private boolean isValidSecret(String requestSecret) {
        return MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                requestSecret.getBytes(StandardCharsets.UTF_8));
    }
}
