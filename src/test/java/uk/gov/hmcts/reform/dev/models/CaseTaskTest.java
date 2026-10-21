package uk.gov.hmcts.reform.dev.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CaseTaskTest {

    @Test
    void copiesFieldsFromCreationRequest() {
        LocalDateTime dueDateTime = LocalDateTime.of(2026, 11, 1, 10, 30);
        CreateCaseRequest request = new CreateCaseRequest(
            "ABC12345",
            "Review evidence",
            "Read the witness statement",
            "Accepted",
            dueDateTime
        );

        Instant beforeCreation = Instant.now().minusMillis(1);
        CaseTask task = new CaseTask(request);
        Instant afterCreation = Instant.now();

        assertFalse(task.getCreatedDate().isBefore(beforeCreation));
        assertFalse(task.getCreatedDate().isAfter(afterCreation));
        assertNull(task.getId());
        assertEquals("ABC12345", task.getCaseNumber());
        assertEquals(request.title(), task.getTitle());
        assertEquals(request.description(), task.getDescription());
        assertEquals(request.status(), task.getStatus());
        assertEquals(dueDateTime, task.getDueDateTime());
    }

    @Test
    void updatingStatusRepeatedlyPreservesOtherFields() {
        CreateCaseRequest request = new CreateCaseRequest(
            "ABC12345", "Review evidence", "Read statement", "Accepted",
            LocalDateTime.of(2026, 11, 1, 10, 30)
        );
        CaseTask task = new CaseTask(request);
        final Instant createdDate = task.getCreatedDate();

        task.updateStatus("Completed");
        assertEquals("Completed", task.getStatus());
        task.updateStatus("Completed");

        assertEquals("Completed", task.getStatus());
        assertEquals(createdDate, task.getCreatedDate());
        assertEquals(request.caseNumber(), task.getCaseNumber());
        assertEquals(request.title(), task.getTitle());
        assertEquals(request.description(), task.getDescription());
        assertEquals(request.dueDateTime(), task.getDueDateTime());
    }

    @Test
    void allowsMissingDescription() {
        CreateCaseRequest request = new CreateCaseRequest(
            "ABC12345",
            "Review evidence",
            null,
            "Accepted",
            LocalDateTime.of(2026, 11, 1, 10, 30)
        );

        CaseTask task = new CaseTask(request);

        assertNull(task.getDescription());
    }
}
