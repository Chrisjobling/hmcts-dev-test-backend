package uk.gov.hmcts.reform.dev.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.dev.models.serialization.StrictStringDeserializer;

import java.time.Instant;

public record CreateCaseRequest(
    @NotBlank @Size(max = 255) @JsonDeserialize(using = StrictStringDeserializer.class) String caseNumber,
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotBlank @Size(max = 255) String status,
    @NotNull Instant dueDateTime
) {
}
