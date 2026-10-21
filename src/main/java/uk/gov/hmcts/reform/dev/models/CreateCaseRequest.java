package uk.gov.hmcts.reform.dev.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateCaseRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotBlank @Size(max = 255) String status,
    @NotNull LocalDateTime dueDateTime
) {
}
