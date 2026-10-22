package uk.gov.hmcts.reform.dev.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.Instant;
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

    private static PageRequest defaultPage() {
        return PageRequest.of(0, 10, Sort.by("id").ascending());
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseRepository caseRepository;

    @Test
    void retrievesRequestedPageWithTotals() throws Exception {
        PageRequest pageable = PageRequest.of(2, 5, Sort.by("id").ascending());
        when(caseRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 10));

        mockMvc.perform(get("/cases").param("page", "2").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(10))
            .andExpect(jsonPath("$.totalPages").value(2));
        verify(caseRepository).findAll(pageable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "2147483648"})
    void rejectsInvalidPage(String page) throws Exception {
        mockMvc.perform(get("/cases").param("page", page)).andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "51", "abc", "2147483648"})
    void rejectsInvalidSize(String size) throws Exception {
        mockMvc.perform(get("/cases").param("size", size)).andExpect(status().isBadRequest());
        verifyNoInteractions(caseRepository);
    }

    @Test
    void retrievesAllTasks() throws Exception {
        CaseTask first = new CaseTask(new CreateCaseRequest(
            "ABC12345",
            "Review evidence", "Read the witness statement", "Accepted",
            Instant.parse("2026-11-01T10:30:00Z")
        ));
        CaseTask second = new CaseTask(new CreateCaseRequest(
            "00054321",
            "Schedule hearing", null, "Pending", Instant.parse("2026-11-02T14:00:00Z")
        ));
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        when(caseRepository.findAll(defaultPage()))
            .thenReturn(new PageImpl<>(List.of(first, second), defaultPage(), 2));

        mockMvc.perform(get("/cases"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].caseNumber").value("ABC12345"))
            .andExpect(jsonPath("$.content[0].title").value("Review evidence"))
            .andExpect(jsonPath("$.content[0].description").value("Read the witness statement"))
            .andExpect(jsonPath("$.content[0].status").value("Accepted"))
            .andExpect(jsonPath("$.content[0].dueDateTime").value("2026-11-01T10:30:00Z"))
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].caseNumber").value("00054321"))
            .andExpect(jsonPath("$.content[1].title").value("Schedule hearing"))
            .andExpect(jsonPath("$.content[1].description").doesNotExist())
            .andExpect(jsonPath("$.content[1].status").value("Pending"))
            .andExpect(jsonPath("$.content[1].dueDateTime").value("2026-11-02T14:00:00Z"));

        verify(caseRepository).findAll(defaultPage());
    }

    @Test
    void returnsEmptyArrayWhenFilterHasNoMatches() throws Exception {
        when(caseRepository.findByStatusIgnoreCase("open", defaultPage())).thenReturn(Page.empty(defaultPage()));
        mockMvc.perform(get("/cases").param("status", "open"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
        verify(caseRepository).findByStatusIgnoreCase("open", defaultPage());
        verifyNoMoreInteractions(caseRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"open", "closed", "in-progress", "OPEN", "Closed", "In-Progress"})
    void retrievesTasksMatchingStatus(String filter) throws Exception {
        CaseTask task = new CaseTask(new CreateCaseRequest(
            "ABC123", "Review evidence", null, filter, Instant.parse("2026-11-01T10:30:00Z")
        ));
        when(caseRepository.findByStatusIgnoreCase(filter, defaultPage()))
            .thenReturn(new PageImpl<>(List.of(task), defaultPage(), 1));
        mockMvc.perform(get("/cases").param("status", filter))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value(filter));
        verify(caseRepository).findByStatusIgnoreCase(filter, defaultPage());
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
    void paginatesFilteredResultsAtMaximumSize() throws Exception {
        PageRequest pageable = PageRequest.of(1, 50, Sort.by("id").ascending());
        when(caseRepository.findByStatusIgnoreCase("open", pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 50));
        mockMvc.perform(get("/cases").param("status", "open").param("page", "1").param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(50))
            .andExpect(jsonPath("$.totalPages").value(1));
        verify(caseRepository).findByStatusIgnoreCase("open", pageable);
        verifyNoMoreInteractions(caseRepository);
    }

    @Test
    void returnsEmptyArrayWhenNoTasksExist() throws Exception {
        when(caseRepository.findAll(defaultPage())).thenReturn(Page.empty(defaultPage()));

        mockMvc.perform(get("/cases"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.content").isEmpty());

        verify(caseRepository).findAll(defaultPage());
    }
}
