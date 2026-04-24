package eu.commission.ims.module.interview.controller;

import eu.commission.ims.common.dto.ApiResponse;
import eu.commission.ims.module.interview.dto.InterviewRequest;
import eu.commission.ims.module.interview.dto.InterviewResponse;
import eu.commission.ims.module.interview.dto.RescheduleRequest;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Technical Interview module.
 * Base path: /api/v1/interviews
 */
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Tag(name = "Technical Interview", description = "Schedule and manage technical interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @Operation(summary = "Schedule a new interview")
    public ResponseEntity<ApiResponse<InterviewResponse>> scheduleInterview(
            @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(interviewService.scheduleInterview(request), "Interview scheduled"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get interview by ID")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewById(id)));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "List upcoming interviews", description = "Returns interviews scheduled in the next 30 days")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getUpcoming() {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getUpcomingInterviews()));
    }

    @GetMapping
    @Operation(summary = "List interviews by status")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getByStatus(
            @RequestParam(required = false, defaultValue = "SCHEDULED") InterviewStatus status) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewsByStatus(status)));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an interview")
    public ResponseEntity<ApiResponse<InterviewResponse>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.rescheduleInterview(id, request), "Interview rescheduled"));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark interview as completed")
    public ResponseEntity<ApiResponse<InterviewResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.completeInterview(id), "Interview marked as completed"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an interview")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancel(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.cancelInterview(id, reason), "Interview cancelled"));
    }
}
