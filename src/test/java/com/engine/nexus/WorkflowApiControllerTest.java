package com.engine.nexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should list registered workflow definitions")
    void shouldListDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Should start new workflow instance via POST /api/v1/workflows/{definitionId}/start")
    void shouldStartWorkflowInstance() throws Exception {
        String requestJson = """
                {
                    "customId": "wf_test_api_01",
                    "input": {
                        "userId": "usr_test_42"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/user-onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", is("wf_test_api_01")))
                .andExpect(jsonPath("$.definitionId", is("user-onboarding")));
    }

    @Test
    @DisplayName("Should deliver signal via POST /api/v1/workflows/{workflowId}/signals/{signalName}")
    void shouldDeliverSignal() throws Exception {
        // Start workflow first
        String startJson = """
                {
                    "customId": "wf_test_signal_api",
                    "input": {
                        "userId": "usr_signal_test"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/user-onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startJson))
                .andExpect(status().isAccepted());

        // Wait a brief moment for it to reach WAITING_SIGNAL step
        Thread.sleep(150);

        String signalJson = """
                {
                    "payload": {
                        "approved": true,
                        "officer": "QA"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/wf_test_signal_api/signals/ID_VERIFIED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signalJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Signal delivered successfully")));
    }

    @Test
    @DisplayName("Should retrieve workflow history via GET /api/v1/workflows/{workflowId}/history")
    void shouldRetrieveWorkflowHistory() throws Exception {
        String startJson = """
                {
                    "customId": "wf_test_hist_api",
                    "input": {
                        "userId": "usr_hist_test"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/workflows/user-onboarding/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startJson))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/workflows/wf_test_hist_api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }
}
