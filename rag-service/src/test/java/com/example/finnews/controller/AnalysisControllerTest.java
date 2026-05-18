package com.example.finnews.controller;

import com.example.finnews.model.AnalysisRequest;
import com.example.finnews.model.AnalysisResponse;
import com.example.finnews.orchestrator.FinancialNewsOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FinancialNewsOrchestrator orchestrator;

    @Test
    void acceptsValidAnalysisRequestAndReturnsResponse() throws Exception {
        when(orchestrator.run(any())).thenReturn(new AnalysisResponse(
                "summary",
                "hold",
                0.82,
                List.of("https://example.test/source"),
                List.of("search_engine"),
                List.of("Educational use only.")
        ));

        AnalysisRequest request = new AnalysisRequest("u1", "analyst", "AAPL", "Analyze Apple");

        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("summary"))
                .andExpect(jsonPath("$.recommendation").value("hold"))
                .andExpect(jsonPath("$.confidence").value(0.82))
                .andExpect(jsonPath("$.toolCalls[0]").value("search_engine"));

        verify(orchestrator).run(any(AnalysisRequest.class));
    }

    @Test
    void rejectsMissingRequiredFieldsAtHttpBoundary() throws Exception {
        mockMvc.perform(post("/api/v1/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "u1",
                                  "role": "analyst",
                                  "symbol": "",
                                  "userQuestion": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
