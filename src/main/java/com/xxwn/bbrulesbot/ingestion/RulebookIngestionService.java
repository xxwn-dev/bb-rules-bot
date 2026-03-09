package com.xxwn.bbrulesbot.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulebookIngestionService {

    private final VectorStore vectorStore;

    public void ingest(String fileName, String league) throws InterruptedException {
        log.info("룰북 적재 시작 - league: {}, file: {}", league, fileName);

        var reader = new PagePdfDocumentReader(
                new ClassPathResource(fileName),
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build()
        );

        var splitter = new TokenTextSplitter(1000, 100, 5, 10000, true);

        List<Document> docs = splitter.apply(reader.get());
        docs.forEach(doc -> doc.getMetadata().put("league", league));

        // 무료 티어 분당 100건 제한으로 배치 처리
        int batchSize = 20;
        for (int i = 0; i < docs.size(); i += batchSize) {
            List<Document> batch = docs.subList(i, Math.min(i + batchSize, docs.size()));
            vectorStore.add(batch);
            log.info("적재 중... {}/{}", Math.min(i + batchSize, docs.size()), docs.size());
            if (i + batchSize < docs.size()) {
                TimeUnit.SECONDS.sleep(60);
            }
        }
        log.info("룰북 적재 완료 - league: {}, 총 {}개 청크", league, docs.size());
    }
}
