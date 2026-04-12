package com.xxwn.bbrulesbot.rag;

import org.springframework.ai.document.Document;

import java.util.List;

public record HybridSearchResult(List<Document> docs, double maxVectorSimilarity) {}
