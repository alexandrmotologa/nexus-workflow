package com.engine.nexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return existing workflow instance when same Idempotency-Key is provided")
    void shouldDeduplicateWithIdempotencyKey() throws Exception {
        String key = "idemp_" + UUID.randomUUID();
        String payload = """
                {
                    "input": {
                        "userId": "usr_idemp_test"
                    }
                }
                """;

        // First request
        MvcResult firstResult = mockMvc.perform(post("/api/v1/workflows/user-onboarding/start")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andReturn();

        String firstBody = firstResult.getResponse().getContentAsString();

        // Second request with SAME idempotency key
        MvcResult secondResult = mockMvc.perform(post("/api/v1/workflows/user-onboarding/start")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andReturn();

        String secondBody = secondResult.getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode firstNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(firstBody);
        com.fasterxml.jackson.databind.JsonNode secondNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(secondBody);

        assertEquals(firstNode.get("id").asText(), secondNode.get("id").asText(), "Duplicate request with same Idempotency-Key must return same workflow ID");
        assertEquals(firstNode.get("definitionId").asText(), secondNode.get("definitionId").asText());
    }
}
