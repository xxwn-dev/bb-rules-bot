package com.xxwn.bbrulesbot.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RulesSearchRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private EmbeddingModel embeddingModel;

    private RulesSearchRepository searchRepository;

    @BeforeEach
    void setUp() {
        searchRepository = new RulesSearchRepository(jdbcTemplate, embeddingModel, new ObjectMapper());
    }

    @Test
    @DisplayName("Branch 1: HyDE 가상 구절을 임베딩해 벡터 검색에 사용한다")
    void hybridSearch_usesHydePassageForVectorBranch() {
        String hydePassage = "보크는 투수가 투구 동작 중 일시 정지하거나 불완전한 동작을 취할 때 선언된다";
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        searchRepository.hybridSearch(hydePassage, "보크 조건이 뭐야?", 10);

        // Branch 1은 hydePassage를 임베딩해 벡터 공간에서 검색한다
        verify(embeddingModel).embed(hydePassage);
    }

    @Test
    @DisplayName("Branch 2: originalQuestion을 pg_trgm 검색에 사용한다 (긴 hydePassage여도 동일)")
    void hybridSearch_usesOriginalQuestionForTrgmBranch_evenWithLongHyde() {
        // hydePassage가 2000자를 초과하면 originalQuestion으로 임베딩을 대체한다
        // pg_trgm Branch 2는 항상 originalQuestion을 입력으로 받는다 (SQL에서 ? 바인딩)
        String longHydePassage = "a".repeat(2001);
        String originalQuestion = "보크 조건이 뭐야?";
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        searchRepository.hybridSearch(longHydePassage, originalQuestion, 10);

        // hydePassage가 너무 길면 Branch 1도 originalQuestion으로 임베딩해야 한다
        verify(embeddingModel).embed(originalQuestion);
    }

    @Test
    @DisplayName("DB 결과가 없으면 빈 문서 목록과 maxVectorSimilarity 0.0을 반환한다")
    void hybridSearch_noResults_returnsEmptyWithZeroSimilarity() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        // jdbcTemplate.query는 기본적으로 아무것도 하지 않아 0건 반환

        HybridSearchResult result = searchRepository.hybridSearch("관련 없는 질문", "관련 없는 질문", 10);

        assertThat(result.docs()).isEmpty();
        assertThat(result.maxVectorSimilarity()).isEqualTo(0.0);
    }
}
