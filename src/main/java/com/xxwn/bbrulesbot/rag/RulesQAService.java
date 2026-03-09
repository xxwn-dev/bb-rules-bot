package com.xxwn.bbrulesbot.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesQAService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public String ask(String question) {
        log.info("질문 수신: {}", question);

        // 1. 쿼리 재작성 - 구어체를 규칙서 검색에 적합한 문체로 변환
        String rewrittenQuery = chatClient.prompt()
                .user("""
                        다음 질문을 야구 규칙서에서 검색하기 좋은 형태로 재작성해줘.
                        재작성한 질문만 출력해. 설명 없이.

                        질문: %s
                        """.formatted(question))
                .call()
                .content();

        log.info("재작성된 쿼리: {}", rewrittenQuery);

        // 2. 재작성된 쿼리로 유사한 규칙 청크 검색
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(rewrittenQuery)
                        .topK(10)
                        .build()
        );

        // 3. 검색된 청크를 컨텍스트로 조합
        String context = docs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));

        log.info("검색된 청크 수: {}", docs.size());

        // 4. LLM에 컨텍스트와 함께 질문
        return chatClient.prompt()
                .user("""
                        [규칙서 내용]
                        %s

                        [질문]
                        %s
                        """.formatted(context, question))
                .call()
                .content();
    }
}
