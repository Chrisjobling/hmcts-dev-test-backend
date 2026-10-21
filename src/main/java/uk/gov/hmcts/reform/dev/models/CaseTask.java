package uk.gov.hmcts.reform.dev.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime dueDateTime;

    public CaseTask(CreateCaseRequest request) {
        this.title = request.title();
        this.description = request.description();
        this.status = request.status();
        this.dueDateTime = request.dueDateTime();
    }
}
