package uk.gov.hmcts.reform.dev.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(@NotBlank @Size(max = 255) String status) {
}
