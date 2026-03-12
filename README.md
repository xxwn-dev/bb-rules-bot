# bb-rules-bot

KBO/MLB 야구 규칙서 기반 RAG Discord Q&A 봇.
규칙서 PDF를 벡터 DB에 임베딩하고, HyDE(Hypothetical Document Embeddings)로 가상 규칙서 구절을 생성해 관련 규칙을 검색한 뒤 LLM이 답변합니다.

## 기술 스택

| 역할 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 21, Spring Boot 3.5 |
| AI | Spring AI, Gemini 2.5 Flash (chat/embedding) |
| 벡터 DB | pgvector (Neon PostgreSQL) |
| Discord | JDA 5 |
| 빌드 | Gradle |

## 아키텍처

```mermaid
flowchart TD
    A["Discord 멘션\n(@룰봇 질문)"]
    A --> B["MessageListener\n봇 멘션 감지"]
    B --> C["RulesQAService"]
    C --> D["HyDE\n가상 규칙서 구절 생성"]
    D --> E["VectorStore\n유사도 검색 (top-10)"]
    E --> F["ChatClient\n규칙서 컨텍스트 + 질문"]
    F --> G["Discord 답변"]

    H["REST API\nPOST /api/ingestion/trigger"]
    H --> I["RulebookIngestionService"]
    I --> J["PDF 파싱 + 청크 분할\n(1000 tokens, overlap 100)"]
    J --> K["VectorStore\n임베딩 저장"]
```

**RAG 흐름**
1. 사용자가 Discord에서 봇을 멘션해 야구 규칙 질문
2. HyDE: 질문에 대한 가상 규칙서 구절을 LLM으로 생성
3. 가상 구절을 임베딩해 pgvector에서 유사 청크 상위 10개 검색
4. 검색된 청크를 컨텍스트로 LLM에 전달해 최종 답변 생성

## 환경변수

```yaml
DB_URL: jdbc:postgresql://<neon-host>/neondb?sslmode=require&currentSchema=bb_rules
DB_USERNAME: <username>
DB_PASSWORD: <password>

AI_API_KEY: <gemini-api-key>
AI_BASE_URL: https://generativelanguage.googleapis.com/v1beta/openai/

DISCORD_BOT_TOKEN: <discord-bot-token>
INGESTION_SECRET: <임의의 시크릿>
```
