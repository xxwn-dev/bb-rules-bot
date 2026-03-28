package com.xxwn.bbrulesbot.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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

        // 무료 티어 분당 100건 제한 - 문서 1개씩 700ms 간격으로 처리 (~85 RPM)
        for (int i = 0; i < docs.size(); i++) {
            vectorStore.add(List.of(docs.get(i)));
            log.info("적재 중... {}/{}", i + 1, docs.size());
            if (i < docs.size() - 1) {
                TimeUnit.MILLISECONDS.sleep(700);
            }
        }
        log.info("룰북 적재 완료 - league: {}, 총 {}개 청크", league, docs.size());
    }

    public void ingestFromUrl(String url, String league) throws InterruptedException, IOException {
        log.info("URL 룰북 적재 시작 - league: {}, url: {}", league, url);

        org.jsoup.nodes.Document html = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        html.select("header, footer, nav, script, style, noscript").remove();

        String text = html.body().text();
        if (text.isBlank()) {
            throw new IllegalStateException("페이지에서 텍스트를 추출할 수 없습니다: " + url);
        }

        var splitter = new TokenTextSplitter(1000, 100, 5, 10000, true);
        List<Document> docs = splitter.apply(List.of(new Document(text)));
        docs.forEach(doc -> doc.getMetadata().putAll(Map.of("league", league, "source", url)));

        for (int i = 0; i < docs.size(); i++) {
            vectorStore.add(List.of(docs.get(i)));
            log.info("URL 적재 중... {}/{}", i + 1, docs.size());
            if (i < docs.size() - 1) {
                TimeUnit.MILLISECONDS.sleep(700);
            }
        }
        log.info("URL 룰북 적재 완료 - league: {}, 총 {}개 청크", league, docs.size());
    }
}
