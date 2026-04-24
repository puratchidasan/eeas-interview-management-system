package eu.commission.ims.module.resume.controller;

import eu.commission.ims.common.dto.ApiResponse;
import eu.commission.ims.module.resume.dto.ResumeRequest;
import eu.commission.ims.module.resume.dto.ResumeResponse;
import eu.commission.ims.module.resume.dto.StatusUpdateRequest;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import eu.commission.ims.module.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Resume Submission module.
 * Base path: /api/v1/resumes
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume Submission", description = "Manage candidate resume submissions")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    @Operation(summary = "Submit a new resume", description = "Creates a candidate (if new) and submits their resume for review")
    public ResponseEntity<ApiResponse<ResumeResponse>> submitResume(
            @Valid @RequestBody ResumeRequest request) {
        ResumeResponse response = resumeService.submitResume(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Resume submitted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resume by ID")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(
            @Parameter(description = "Resume ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(resumeService.getResumeById(id)));
    }

    @GetMapping
    @Operation(summary = "List all resumes", description = "Returns a paginated list of resumes, optionally filtered by status")
    public ResponseEntity<ApiResponse<Page<ResumeResponse>>> getAllResumes(
            @Parameter(description = "Filter by submission status") @RequestParam(required = false) SubmissionStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(resumeService.getAllResumes(status, pageable)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update resume status", description = "Change the status of a resume (e.g., UNDER_REVIEW, ACCEPTED, REJECTED)")
    public ResponseEntity<ApiResponse<ResumeResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(resumeService.updateResumeStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Withdraw a resume", description = "Marks the resume as WITHDRAWN — candidate withdraws their application")
    public ResponseEntity<ApiResponse<Void>> withdrawResume(@PathVariable Long id) {
        resumeService.withdrawResume(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Resume withdrawn successfully"));
    }

    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get resumes by candidate", description = "Returns all resumes submitted by a specific candidate")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getByCandidate(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(resumeService.getResumesByCandidate(candidateId)));
    }
}
