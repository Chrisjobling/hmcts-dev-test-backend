package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.models.ExampleCase;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    @Operation(summary = "Retrieve a task by ID")
    @ApiResponse(responseCode = "200", description = "Task found",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = CaseTask.class)))
    @ApiResponse(responseCode = "400", description = "ID must be a valid 64-bit integer", content = @Content)
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    @GetMapping(value = "/cases/{id}", produces = "application/json")
    public ResponseEntity<CaseTask> getCaseById(@PathVariable("id") Long id) {
        return ResponseEntity.of(caseRepository.findById(id));
    }

    @Operation(summary = "Retrieve all tasks")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                 content = @Content(mediaType = "application/json",
                     array = @ArraySchema(schema = @Schema(implementation = CaseTask.class))))
    @GetMapping(value = "/cases", produces = "application/json")
    public ResponseEntity<List<CaseTask>> getAllCases() {
        return ok(caseRepository.findAll());
    }

    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<ExampleCase> getExampleCase() {
        return ok(new ExampleCase(1, "ABC12345", "Case Title",
                                  "Case Description", "Case Status", LocalDateTime.now()
        ));
    }
}
