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
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
class ModifyCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseRepository caseRepository;

    @Test
    void updatesStatusWithoutChangingOtherFields() throws Exception {
        CaseTask task = task();
        String createdDate = task.getCreatedDate().toString();
        when(caseRepository.findById(42L)).thenReturn(Optional.of(task));
        when(caseRepository.save(task)).thenReturn(task);

        mockMvc.perform(patch("/cases/42/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"Completed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Completed"))
            .andExpect(jsonPath("$.caseNumber").value("ABC123"))
            .andExpect(jsonPath("$.title").value("Review evidence"))
            .andExpect(jsonPath("$.description").value("Read statement"))
            .andExpect(jsonPath("$.dueDateTime").value("2026-11-01T10:30:00"))
            .andExpect(jsonPath("$.createdDate").value(createdDate));

        verify(caseRepository).save(task);
    }

    @Test
    void rejectsPutForStatusEndpoint() throws Exception {
        mockMvc.perform(put("/cases/42/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"Completed\"}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingTask() throws Exception {
        when(caseRepository.findById(42L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/cases/42/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"Completed\"}"))
            .andExpect(status().isNotFound());
        verify(caseRepository, never()).save(any(CaseTask.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"\"}", "{\"status\":\" \"}", "{"})
    void rejectsInvalidStatus(String body) throws Exception {
        mockMvc.perform(patch("/cases/42/status").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void rejectsStatusExceedingMaximumLength() throws Exception {
        mockMvc.perform(patch("/cases/42/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + "a".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void deletesExistingTask() throws Exception {
        CaseTask task = task();
        when(caseRepository.findById(42L)).thenReturn(Optional.of(task));
        mockMvc.perform(delete("/cases/42"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        verify(caseRepository).delete(task);
    }

    @Test
    void returnsNotFoundWhenDeletingMissingTask() throws Exception {
        when(caseRepository.findById(42L)).thenReturn(Optional.empty());
        mockMvc.perform(delete("/cases/42")).andExpect(status().isNotFound());
        verify(caseRepository, never()).delete(any(CaseTask.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mockMvc.perform(delete("/cases/{id}", id)).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/cases/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"Completed\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    private CaseTask task() {
        return new CaseTask(new CreateCaseRequest(
            "ABC123", "Review evidence", "Read statement", "Accepted", LocalDateTime.of(2026, 11, 1, 10, 30)
        ));
    }
}
