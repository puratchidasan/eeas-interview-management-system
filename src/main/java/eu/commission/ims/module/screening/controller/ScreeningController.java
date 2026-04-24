package eu.commission.ims.module.screening.controller;

import eu.commission.ims.common.dto.ApiResponse;
import eu.commission.ims.module.screening.dto.DecisionRequest;
import eu.commission.ims.module.screening.dto.ScreeningRequest;
import eu.commission.ims.module.screening.dto.ScreeningResponse;
import eu.commission.ims.module.screening.service.ScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Screening module.
 * Base path: /api/v1/screenings
 */
@RestController
@RequestMapping("/api/v1/screenings")
@RequiredArgsConstructor
@Tag(name = "Screening", description = "HR eligibility screening management")
public class ScreeningController {

    private final ScreeningService screeningService;

    @PostMapping
    @Operation(summary = "Create a screening", description = "Initiates an HR screening for a submitted resume")
    public ResponseEntity<ApiResponse<ScreeningResponse>> createScreening(
            @Valid @RequestBody ScreeningRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(screeningService.createScreening(request), "Screening created"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get screening by ID")
    public ResponseEntity<ApiResponse<ScreeningResponse>> getScreening(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(screeningService.getScreeningById(id)));
    }

    @GetMapping("/resume/{resumeId}")
    @Operation(summary = "Get screening by resume ID")
    public ResponseEntity<ApiResponse<ScreeningResponse>> getByResume(@PathVariable Long resumeId) {
        return ResponseEntity.ok(ApiResponse.success(screeningService.getScreeningByResumeId(resumeId)));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending screenings", description = "Returns all screenings awaiting a decision")
    public ResponseEntity<ApiResponse<List<ScreeningResponse>>> getPendingScreenings() {
        return ResponseEntity.ok(ApiResponse.success(screeningService.getPendingScreenings()));
    }

    @PutMapping("/{id}/decision")
    @Operation(summary = "Record screening decision", description = "Records PASSED or FAILED decision and updates resume status accordingly")
    public ResponseEntity<ApiResponse<ScreeningResponse>> recordDecision(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                screeningService.recordDecision(id, request), "Decision recorded"));
    }
}
