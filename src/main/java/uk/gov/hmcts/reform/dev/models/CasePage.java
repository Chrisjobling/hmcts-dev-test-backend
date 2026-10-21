package uk.gov.hmcts.reform.dev.models;

import org.springframework.data.domain.Page;

import java.util.List;

public record CasePage(List<CaseTask> content, int page, int size, long totalElements, int totalPages) {

    public static CasePage from(Page<CaseTask> result) {
        return new CasePage(result.getContent(), result.getNumber(), result.getSize(),
                            result.getTotalElements(), result.getTotalPages());
    }
}
