package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
class GetAllCasesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseRepository caseRepository;

    @Test
    void retrievesAllTasks() throws Exception {
        CaseTask first = new CaseTask(new CreateCaseRequest(
            "ABC12345",
            "Review evidence", "Read the witness statement", "Accepted",
            LocalDateTime.of(2026, 11, 1, 10, 30)
        ));
        CaseTask second = new CaseTask(new CreateCaseRequest(
            "00054321",
            "Schedule hearing", null, "Pending", LocalDateTime.of(2026, 11, 2, 14, 0)
        ));
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        when(caseRepository.findAll()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/cases"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].caseNumber").value("ABC12345"))
            .andExpect(jsonPath("$[0].title").value("Review evidence"))
            .andExpect(jsonPath("$[0].description").value("Read the witness statement"))
            .andExpect(jsonPath("$[0].status").value("Accepted"))
            .andExpect(jsonPath("$[0].dueDateTime").value("2026-11-01T10:30:00"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].caseNumber").value("00054321"))
            .andExpect(jsonPath("$[1].title").value("Schedule hearing"))
            .andExpect(jsonPath("$[1].description").doesNotExist())
            .andExpect(jsonPath("$[1].status").value("Pending"))
            .andExpect(jsonPath("$[1].dueDateTime").value("2026-11-02T14:00:00"));

        verify(caseRepository).findAll();
    }

    @Test
    void returnsEmptyArrayWhenFilterHasNoMatches() throws Exception {
        when(caseRepository.findByStatusIgnoreCase("open")).thenReturn(List.of());
        mockMvc.perform(get("/cases").param("status", "open"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
        verify(caseRepository).findByStatusIgnoreCase("open");
        verifyNoMoreInteractions(caseRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"open", "closed", "in-progress", "OPEN", "Closed", "In-Progress"})
    void retrievesTasksMatchingStatus(String filter) throws Exception {
        CaseTask task = new CaseTask(new CreateCaseRequest(
            "ABC123", "Review evidence", null, filter, LocalDateTime.of(2026, 11, 1, 10, 30)
        ));
        when(caseRepository.findByStatusIgnoreCase(filter)).thenReturn(List.of(task));
        mockMvc.perform(get("/cases").param("status", filter))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value(filter));
        verify(caseRepository).findByStatusIgnoreCase(filter);
        verifyNoMoreInteractions(caseRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "pending", "in progress", "inprogress", "open,closed"})
    void rejectsUnsupportedStatusFilter(String filter) throws Exception {
        mockMvc.perform(get("/cases").param("status", filter))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void returnsEmptyArrayWhenNoTasksExist() throws Exception {
        when(caseRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/cases"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[]"));

        verify(caseRepository).findAll();
    }
}
