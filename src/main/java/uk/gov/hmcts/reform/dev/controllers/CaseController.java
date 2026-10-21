package uk.gov.hmcts.reform.dev.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.models.ExampleCase;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class CaseController {

    private final CaseRepository caseRepository;

    public CaseController(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @PostMapping(value = "/cases", consumes = "application/json", produces = "application/json")
    public ResponseEntity<CaseTask> createCase(@Valid @RequestBody CreateCaseRequest request) {
        CaseTask createdCase = caseRepository.save(new CaseTask(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCase);
    }

    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<ExampleCase> getExampleCase() {
        return ok(new ExampleCase(1, "ABC12345", "Case Title",
                                  "Case Description", "Case Status", LocalDateTime.now()
        ));
    }
}
