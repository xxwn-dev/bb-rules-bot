package com.xxwn.bbrulesbot.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RulesSearchRepository {

    private static final int RRF_K = 60;
    private static final int BRANCH_FETCH_MULTIPLIER = 2;
    private static final int MAX_HYDE_LENGTH = 2000;

    private static final String HYBRID_SEARCH_SQL = """
            WITH vector_distances AS (
                SELECT id, content, metadata,
                       embedding <=> ?::vector AS distance
                FROM bb_rules.vector_store
                ORDER BY distance
                LIMIT ?
            ),
            vector_results AS (
                SELECT id, content, metadata,
                       1 - distance AS vector_similarity,
                       ROW_NUMBER() OVER (ORDER BY distance) AS rank
                FROM vector_distances
            ),
            trgm_results AS (
                SELECT id, content, metadata,
                       ROW_NUMBER() OVER (ORDER BY similarity(content, ?) DESC) AS rank
                FROM bb_rules.vector_store
                WHERE content % ?
                ORDER BY similarity(content, ?) DESC
                LIMIT ?
            ),
            rrf AS (
                SELECT
                    COALESCE(v.id, t.id) AS id,
                    COALESCE(v.content, t.content) AS content,
                    COALESCE(v.metadata, t.metadata) AS metadata,
                    COALESCE(1.0 / (? + v.rank), 0) + COALESCE(1.0 / (? + t.rank), 0) AS rrf_score
                FROM vector_results v
                FULL OUTER JOIN trgm_results t ON v.id = t.id
            )
            SELECT id, content, metadata, rrf_score,
                   (SELECT COALESCE(MAX(vector_similarity), 0.0) FROM vector_results) AS max_vector_similarity
            FROM rrf
            ORDER BY rrf_score DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    public HybridSearchResult hybridSearch(String hydePassage, String originalQuestion, int topK) {
        String embeddingInput = (hydePassage != null && hydePassage.length() <= MAX_HYDE_LENGTH)
                ? hydePassage : originalQuestion;

        float[] embedding = embeddingModel.embed(embeddingInput);
        String vectorStr = new PGvector(embedding).toString();
        int branchLimit = topK * BRANCH_FETCH_MULTIPLIER;

        List<Document> docs = new ArrayList<>();
        double[] maxSimilarity = {0.0};

        jdbcTemplate.query(
                HYBRID_SEARCH_SQL,
                ps -> {
                    ps.setString(1, vectorStr);
                    ps.setInt(2, branchLimit);
                    ps.setString(3, originalQuestion);
                    ps.setString(4, originalQuestion);
                    ps.setString(5, originalQuestion);
                    ps.setInt(6, branchLimit);
                    ps.setInt(7, RRF_K);
                    ps.setInt(8, RRF_K);
                    ps.setInt(9, topK);
                },
                rs -> {
                    if (maxSimilarity[0] == 0.0) {
                        maxSimilarity[0] = rs.getDouble("max_vector_similarity");
                    }
                    String content = rs.getString("content");
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = parseMetadata(metadataJson);
                    docs.add(new Document(content, metadata));
                }
        );

        log.debug("[HybridSearch] 청크 수: {}, max_vector_similarity: {}",
                docs.size(), maxSimilarity[0]);

        return new HybridSearchResult(docs, maxSimilarity[0]);
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("metadata 파싱 실패, 빈 Map으로 대체: {}", e.getMessage());
            return Map.of();
        }
    }
}
