package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.models.CaseTask;
import uk.gov.hmcts.reform.dev.models.CasePage;
import uk.gov.hmcts.reform.dev.models.CreateCaseRequest;
import uk.gov.hmcts.reform.dev.models.ExampleCase;
import uk.gov.hmcts.reform.dev.models.UpdateStatusRequest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.Instant;

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
    @ApiResponse(responseCode = "400", description = "Invalid status filter or pagination parameters",
                 content = @Content)
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                 content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = CasePage.class)))
    @GetMapping(value = "/cases", produces = "application/json")
    public ResponseEntity<CasePage> getAllCases(
        @Parameter(description = "Optional status filter (case-insensitive)",
                   schema = @Schema(allowableValues = {"open", "in-progress", "closed"}))
        @RequestParam(name = "status", required = false)
        @Pattern(regexp = "open|in-progress|closed", flags = Pattern.Flag.CASE_INSENSITIVE)
        String status,
        @Parameter(description = "Zero-based page number")
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Cases per page, from 1 to 50")
        @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ok(CasePage.from(status == null
                                   ? caseRepository.findAll(pageable)
                                   : caseRepository.findByStatusIgnoreCase(status, pageable)));
    }

    @Operation(summary = "Update a task's status")
    @ApiResponse(responseCode = "200", description = "Status updated",
                 content = @Content(mediaType = "application/json", schema = @Schema(implementation = CaseTask.class)))
    @ApiResponse(responseCode = "400", description = "Invalid ID or status", content = @Content)
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    @PatchMapping(value = "/cases/{id}/status", consumes = "application/json", produces = "application/json")
    @Transactional
    public ResponseEntity<CaseTask> updateStatus(@PathVariable("id") Long id,
                                                @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.of(caseRepository.findById(id).map(task -> {
            task.updateStatus(request.status());
            return caseRepository.save(task);
        }));
    }

    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "204", description = "Task deleted", content = @Content)
    @ApiResponse(responseCode = "400", description = "ID must be a valid 64-bit integer", content = @Content)
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    @DeleteMapping("/cases/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCase(@PathVariable("id") Long id) {
        return caseRepository.findById(id).map(task -> {
            caseRepository.delete(task);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<ExampleCase> getExampleCase() {
        return ok(new ExampleCase(1, "ABC12345", "Case Title",
                                  "Case Description", "Case Status", Instant.now()
        ));
    }
}
