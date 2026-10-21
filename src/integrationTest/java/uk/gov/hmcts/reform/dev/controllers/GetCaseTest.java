package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
class GetCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseRepository caseRepository;

    @ParameterizedTest
    @ValueSource(strings = {"Read the witness statement", ""})
    void retrievesExistingCase(String description) throws Exception {
        String optionalDescription = description.isEmpty() ? null : description;
        CaseTask task = new CaseTask(new CreateCaseRequest(
            "ABC12345",
            "Review evidence", optionalDescription, "Accepted", LocalDateTime.of(2026, 11, 1, 10, 30)
        ));
        ReflectionTestUtils.setField(task, "id", 42L);
        when(caseRepository.findById(42L)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/cases/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.caseNumber").value("ABC12345"))
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").value(optionalDescription))
            .andExpect(jsonPath("$.status").value("Accepted"))
            .andExpect(jsonPath("$.dueDateTime").value("2026-11-01T10:30:00"));

        verify(caseRepository).findById(42L);
    }

    @Test
    void returnsNotFoundForMissingCase() throws Exception {
        when(caseRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cases/42"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));

        verify(caseRepository).findById(42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "1.5", "9223372036854775808"})
    void rejectsInvalidId(String id) throws Exception {
        mockMvc.perform(get("/cases/{id}", id))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(caseRepository);
    }
}
