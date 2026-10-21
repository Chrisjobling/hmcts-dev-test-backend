package uk.gov.hmcts.reform.dev.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "case_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caseNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime dueDateTime;

    @Schema(description = "Server-generated creation timestamp in UTC", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    public CaseTask(CreateCaseRequest request) {
        this.caseNumber = request.caseNumber();
        this.title = request.title();
        this.description = request.description();
        this.status = request.status();
        this.dueDateTime = request.dueDateTime();
        this.createdDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
