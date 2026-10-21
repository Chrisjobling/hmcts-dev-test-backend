package uk.gov.hmcts.reform.dev.controllers;

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
                                  "title": "Review evidence",
                                  "description": "Read the witness statement",
                                  "status": "Accepted",
                                  "dueDateTime": "2026-11-01T10:30:00"
                                }
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").value("Read the witness statement"))
            .andExpect(jsonPath("$.status").value("Accepted"))
            .andExpect(jsonPath("$.dueDateTime").value("2026-11-01T10:30:00"));

        verify(caseRepository).save(any(CaseTask.class));
    }

    @Test
    void createsCaseWithoutDescription() throws Exception {
        when(caseRepository.save(any(CaseTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/cases")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"title":"Review evidence","status":"Accepted","dueDateTime":"2026-11-01T10:30:00"}
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").doesNotExist());

        verify(caseRepository).save(any(CaseTask.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"title\":\" \",\"status\":\"Accepted\",\"dueDateTime\":\"2026-11-01T10:30:00\"}",
        "{\"title\":\"Review evidence\",\"status\":\" \",\"dueDateTime\":\"2026-11-01T10:30:00\"}",
        "{\"title\":\"Review evidence\",\"status\":\"Accepted\"}",
        "{\"title\":\"Review evidence\",\"status\":\"Accepted\",\"dueDateTime\":\"not-a-date\"}"
    })
    void rejectsInvalidRequests(String body) throws Exception {
        mockMvc.perform(post("/cases").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(caseRepository);
    }

    @Test
    void preservesExampleEndpoint() throws Exception {
        mockMvc.perform(get("/get-example-case"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Case Title"));
    }
}
