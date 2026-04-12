package com.xxwn.bbrulesbot.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestionController.class)
@TestPropertySource(properties = "ingestion.secret=test-secret")
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RulebookIngestionService ingestionService;

    private static final String TRIGGER_URL = "/api/ingestion/trigger";
    private static final String SECRET_HEADER = "X-Ingestion-Secret";
    private static final String VALID_SECRET = "test-secret";

    @Test
    @DisplayName("올바른 secret으로 요청 시 200 OK 반환")
    void trigger_withValidSecret_returnsOk() throws Exception {
        mockMvc.perform(post(TRIGGER_URL)
                        .header(SECRET_HEADER, VALID_SECRET))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 secret으로 요청 시 401 Unauthorized 반환")
    void trigger_withInvalidSecret_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(TRIGGER_URL)
                        .header(SECRET_HEADER, "wrong-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("secret 헤더 없을 때 401 반환")
    void trigger_withMissingSecret_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(TRIGGER_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("valid secret이면 ingestionService.ingest() 호출됨")
    void trigger_withValidSecret_callsIngestionService() throws Exception {
        mockMvc.perform(post(TRIGGER_URL)
                        .header(SECRET_HEADER, VALID_SECRET))
                .andExpect(status().isOk());

        verify(ingestionService).ingest("2025-kbo-rulebook.pdf", "KBO");
    }
}
