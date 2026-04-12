package com.xxwn.bbrulesbot.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/rules")
public class RulesQAController {

    private final RulesQAService rulesQAService;
    private final String secret;

    public RulesQAController(
            RulesQAService rulesQAService,
            @Value("${ingestion.secret}") String secret) {
        this.rulesQAService = rulesQAService;
        this.secret = secret;
    }

    @GetMapping("/ask")
    public ResponseEntity<String> ask(
            @RequestHeader("X-Ingestion-Secret") String requestSecret,
            @RequestParam String q,
            @RequestParam(defaultValue = "false") boolean debug) {
        if (!MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                requestSecret.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        if (debug) {
            double similarity = rulesQAService.similarity(q);
            return ResponseEntity.ok("similarity: " + similarity);
        }
        return ResponseEntity.ok(rulesQAService.ask(q));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }
}
