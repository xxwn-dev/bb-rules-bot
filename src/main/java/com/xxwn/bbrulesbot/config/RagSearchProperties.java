package com.xxwn.bbrulesbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.search")
public record RagSearchProperties(
        int topK,
        double trgmThreshold
) {}
