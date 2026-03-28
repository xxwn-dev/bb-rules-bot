# Q&A

## Q. Jsoup의 역할이 무엇인가?

Jsoup은 Java용 HTML 파싱 라이브러리입니다.

이 프로젝트에서의 역할:

```
URL (https://koreabaseball.com/...)
    → HTTP 요청 + HTML 수신       ← Jsoup.connect(url).get()
    → HTML 파싱 (DOM 트리 구성)   ← Jsoup 내부 처리
    → 태그 제거, 텍스트 추출      ← doc.select("...").text()
    → 순수 텍스트
    → TokenTextSplitter로 청크 분할
    → VectorStore 임베딩 저장
```

**Jsoup이 해주는 것:**
- `<div>`, `<p>`, `<table>` 같은 HTML 태그를 걷어내고 사람이 읽을 수 있는 텍스트만 추출
- CSS selector로 원하는 영역만 골라낼 수 있음 (e.g. 메뉴/푸터 제외하고 본문만)
- HTTP 요청까지 내장돼 있어서 별도 HTTP 클라이언트 불필요

**없으면:** raw HTML(`<div class="rule">ABS 규정...</div>`)째로 임베딩되어 노이즈가 많아져 검색 정확도가 떨어짐.
