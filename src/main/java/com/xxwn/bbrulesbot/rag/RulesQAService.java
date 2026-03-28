package com.xxwn.bbrulesbot.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesQAService {

    private static final int SIMILARITY_TOP_K = 10;

    private static final String HYDE_PROMPT_TEMPLATE = """
            다음 질문에 대한 답변이 야구 규칙서에 있다면 어떤 형태일지 규칙서 문체로 작성해줘.
            규칙서 구절만 출력해. 설명 없이.

            질문: %s
            """;

    private static final String QA_PROMPT_TEMPLATE = """
            [규칙서 내용]
            %s

            [질문]
            %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public String ask(String question) {
        Assert.hasText(question, "질문은 비어있을 수 없습니다");
        log.info("질문 수신: {}", question);

        String hypotheticalDoc = generateHypotheticalDoc(question);
        log.info("가상 규칙서 구절: {}", hypotheticalDoc);

        List<Document> docs = searchSimilarDocuments(hypotheticalDoc);
        log.info("검색된 청크 수: {}", docs.size());

        String context = buildContext(docs);

        String answer = chatClient.prompt()
                .user(QA_PROMPT_TEMPLATE.formatted(context, question))
                .call()
                .content();
        return Objects.requireNonNullElse(answer, "답변을 생성하지 못했습니다.");
    }

    private String generateHypotheticalDoc(String question) {
        return chatClient.prompt()
                .user(HYDE_PROMPT_TEMPLATE.formatted(question))
                .call()
                .content();
    }

    private List<Document> searchSimilarDocuments(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(SIMILARITY_TOP_K)
                        .build()
        );
    }

    private String buildContext(List<Document> docs) {
        return docs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
    }
}
