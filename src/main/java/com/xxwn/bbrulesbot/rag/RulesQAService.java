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

        // 1. HyDE - 가상의 규칙서 구절을 생성하여 벡터 검색 정확도 향상
        String hypotheticalDoc = chatClient.prompt()
                .user("""
                        다음 질문에 대한 답변이 야구 규칙서에 있다면 어떤 형태일지 규칙서 문체로 작성해줘.
                        규칙서 구절만 출력해. 설명 없이.

                        질문: %s
                        """.formatted(question))
                .call()
                .content();

        log.info("가상 규칙서 구절: {}", hypotheticalDoc);

        // 2. 가상 규칙서 구절로 유사한 청크 검색
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(hypotheticalDoc)
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
