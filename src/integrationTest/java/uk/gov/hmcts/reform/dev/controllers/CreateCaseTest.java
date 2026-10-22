package uk.gov.hmcts.reform.dev.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
class CreateCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseRepository caseRepository;

    @Test
    void createsCaseWithDescription() throws Exception {
        when(caseRepository.save(any(CaseTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/cases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "caseNumber": "ABC12345",
                                  "title": "Review evidence",
                                  "description": "Read the witness statement",
                                  "status": "Accepted",
                                  "dueDateTime": "2026-11-01T10:30:00Z"
                                }
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.caseNumber").value("ABC12345"))
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").value("Read the witness statement"))
            .andExpect(jsonPath("$.status").value("Accepted"))
            .andExpect(jsonPath("$.createdDate").isString())
            .andExpect(jsonPath("$.dueDateTime").value("2026-11-01T10:30:00Z"));

        verify(caseRepository).save(any(CaseTask.class));
    }

    @Test
    void createsCaseWithoutDescription() throws Exception {
        when(caseRepository.save(any(CaseTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/cases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"caseNumber":"00012345","title":"Review evidence",
                                 "status":"Accepted","dueDateTime":"2026-11-01T10:30:00Z"}
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").doesNotExist());

        verify(caseRepository).save(any(CaseTask.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"caseNumber\":\"1\",\"title\":\" \",\"status\":\"Accepted\",\"dueDateTime\":\"2026-11-01T10:30:00Z\"}",
        "{\"caseNumber\":\"1\",\"title\":\"Review evidence\",\"status\":\" \",\"dueDateTime\":\"2026-11-01T10:30:00Z\"}",
        "{\"caseNumber\":\"1\",\"title\":\"Review evidence\",\"status\":\"Accepted\"}",
        "{\"caseNumber\":\"1\",\"title\":\"Review evidence\",\"status\":\"Accepted\",\"dueDateTime\":\"not-a-date\"}",
        "{\"caseNumber\":\"1\",\"title\":\"Review evidence\",\"status\":\"Accepted\",\"dueDateTime\":\"2026-11-01T10:30:00\"}"
    })
    void rejectsInvalidRequests(String body) throws Exception {
        mockMvc.perform(post("/cases").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(caseRepository);
    }

    @Test
    void rejectsMissingCaseNumber() throws Exception {
        mockMvc.perform(post("/cases").contentType(MediaType.APPLICATION_JSON).content("""
            {"title":"Review evidence","status":"Accepted","dueDateTime":"2026-11-01T10:30:00Z"}
            """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\" \"", "12345", "1.5", "true", "[]", "{}"})
    void rejectsInvalidCaseNumber(String caseNumber) throws Exception {
        mockMvc.perform(post("/cases").contentType(MediaType.APPLICATION_JSON).content("""
            {"caseNumber":%s,"title":"Review evidence","status":"Accepted","dueDateTime":"2026-11-01T10:30:00Z"}
            """.formatted(caseNumber)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void preservesExampleEndpoint() throws Exception {
        String response = mockMvc.perform(get("/get-example-case"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Case Title"))
            .andReturn().getResponse().getContentAsString();
        assertNotNull(Instant.parse(JsonPath.read(response, "$.createdDate")));
    }
}
