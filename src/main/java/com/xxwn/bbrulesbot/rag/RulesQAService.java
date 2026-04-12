package com.xxwn.bbrulesbot.rag;

import com.xxwn.bbrulesbot.config.RagSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesQAService {

    private static final String HYDE_PROMPT_TEMPLATE = """
            다음 질문에 대한 답변이 야구 규칙서에 있다면 어떤 형태일지 규칙서 문체로 작성해줘.
            규칙서 구절만 출력해. 설명 없이.

            질문: [%s]
            """;

    private static final String QA_PROMPT_TEMPLATE = """
            [규칙서 내용]
            %s

            [질문]
            %s

            위 규칙서 내용을 우선 참고해서 답해줘.
            규칙서에 관련 내용이 전혀 없으면 첫 줄에 "[없음]"만 출력해줘.
            규칙서에 내용이 있으면 "[📖 규칙서 기반]"을 첫 줄에 붙이고, 핵심 규칙과 판정 기준만 800자 이내로 답해줘. 서론과 맺음말은 생략해.
            """;

    private final ChatClient chatClient;
    private final RulesSearchRepository searchRepository;
    private final RagSearchProperties props;

    public double similarity(String question) {
        Assert.hasText(question, "질문은 비어있을 수 없습니다");
        String hydePassage = generateHypotheticalDoc(question);
        if (hydePassage == null) hydePassage = question;
        return searchRepository.hybridSearch(hydePassage, question, props.topK()).maxVectorSimilarity();
    }

    public String ask(String question) {
        Assert.hasText(question, "질문은 비어있을 수 없습니다");
        log.info("질문 수신: {}", question);

        String hydePassage = generateHypotheticalDoc(question);
        if (hydePassage == null) {
            log.warn("HyDE 생성 실패, 원래 질문으로 검색 진행");
            hydePassage = question;
        }
        log.info("HyDE 가상 구절: {}", hydePassage);

        HybridSearchResult result = searchRepository.hybridSearch(hydePassage, question, props.topK());
        log.info("검색 결과 - 청크 수: {}, 최대 유사도: {}", result.docs().size(), result.maxVectorSimilarity());

        String context = buildContext(result.docs());
        String answer = Objects.requireNonNullElse(
                chatClient.prompt()
                        .user(QA_PROMPT_TEMPLATE.formatted(context, question))
                        .call()
                        .content(),
                "[없음]");

        if (answer.startsWith("[없음]")) {
            log.info("규칙서에 관련 내용 없음, Gemini 직접 호출");
            String fallbackAnswer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            return "[💬 일반 지식 기반]\n" + Objects.requireNonNullElse(fallbackAnswer, "답변을 생성하지 못했습니다.");
        }

        return answer;
    }

    private String generateHypotheticalDoc(String question) {
        try {
            return chatClient.prompt()
                    .user(HYDE_PROMPT_TEMPLATE.formatted(question))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("HyDE 생성 중 예외 발생: {}", e.getMessage());
            return null;
        }
    }

    private String buildContext(List<Document> docs) {
        return docs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
    }
}
