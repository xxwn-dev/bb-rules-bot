package com.xxwn.bbrulesbot.rag;

import com.xxwn.bbrulesbot.config.RagSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RulesQAServiceTest {

    @Mock private ChatClient chatClient;
    @Mock private RulesSearchRepository searchRepository;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    private final RagSearchProperties props = new RagSearchProperties(10, 0.05);

    private RulesQAService rulesQAService;

    @BeforeEach
    void setUp() {
        rulesQAService = new RulesQAService(chatClient, searchRepository, props);
    }

    private void stubChatClient(String first, String... rest) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(first, rest);
    }

    @Test
    @DisplayName("빈 질문이 오면 IllegalArgumentException을 던진다")
    void ask_withBlankQuestion_throwsException() {
        assertThatThrownBy(() -> rulesQAService.ask(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("질문");
    }

    @Test
    @DisplayName("null 질문이 오면 IllegalArgumentException을 던진다")
    void ask_withNullQuestion_throwsException() {
        assertThatThrownBy(() -> rulesQAService.ask(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("야구 용어 질문 — 모델이 규칙서 내용 찾음 → [📖 규칙서 기반] 반환 (Branch 1+2 모두 기여)")
    void ask_baseballTermQuestion_returnsRulebookAnswer() {
        // "보크" 같은 야구 용어는 pg_trgm Branch 2에서도 직접 매칭된다
        // 모델이 청크에서 답을 찾으면 "[📖 규칙서 기반]"으로 시작하는 답변을 반환한다
        stubChatClient("보크 관련 규칙서 가상 구절", "[📖 규칙서 기반]\n보크 답변");
        when(searchRepository.hybridSearch(anyString(), anyString(), anyInt()))
                .thenReturn(new HybridSearchResult(List.of(new Document("보크 규정")), 0.92));

        String answer = rulesQAService.ask("보크 선언 조건이 뭐야?");

        assertThat(answer).startsWith("[📖 규칙서 기반]");
        assertThat(answer).contains("보크 답변");
    }

    @Test
    @DisplayName("자연어 질문 — 모델이 규칙서 내용 찾음 → [📖 규칙서 기반] 반환 (Branch 1 주도)")
    void ask_naturalLanguageQuestion_returnsRulebookAnswer() {
        // 야구 용어 없는 자연어 질문은 HyDE가 규칙서 문체로 변환해 Branch 1이 관련 청크를 찾는다
        // 모델이 청크에서 답을 찾으면 "[📖 규칙서 기반]"으로 시작하는 답변을 반환한다
        stubChatClient("투수는 투구 동작 중 일시 정지 시 보크가 선언된다", "[📖 규칙서 기반]\n동작 관련 답변");
        when(searchRepository.hybridSearch(anyString(), anyString(), anyInt()))
                .thenReturn(new HybridSearchResult(List.of(new Document("투구 동작 규정")), 0.89));

        String answer = rulesQAService.ask("투수가 공 던지기 전에 잠깐 멈추면 어떻게 돼?");

        assertThat(answer).startsWith("[📖 규칙서 기반]");
        assertThat(answer).contains("동작 관련 답변");
    }

    @Test
    @DisplayName("규칙서 외 질문 — 모델이 [없음] 반환 → Gemini 직접 호출로 폴백")
    void ask_nonRuleQuestion_fallsBackToGemini() {
        // 모델이 청크를 보고 관련 내용 없다고 판단하면 "[없음]"을 반환한다
        // 코드가 이를 감지해 컨텍스트 없이 Gemini를 재호출한다
        stubChatClient("관련 없는 가상 구절", "[없음]", "오늘 경기 결과 답변");
        when(searchRepository.hybridSearch(anyString(), anyString(), anyInt()))
                .thenReturn(new HybridSearchResult(List.of(), 0.73));

        String answer = rulesQAService.ask("어제 KBO 경기에서 이긴 팀이 어디야?");

        assertThat(answer).startsWith("[💬 일반 지식 기반]");
        assertThat(answer).contains("오늘 경기 결과 답변");
    }
}
