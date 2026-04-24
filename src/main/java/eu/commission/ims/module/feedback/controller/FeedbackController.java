package eu.commission.ims.module.feedback.controller;

import eu.commission.ims.common.dto.ApiResponse;
import eu.commission.ims.module.feedback.dto.FeedbackRequest;
import eu.commission.ims.module.feedback.dto.FeedbackResponse;
import eu.commission.ims.module.feedback.dto.FinalizeRequest;
import eu.commission.ims.module.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Feedback & Decision module.
 * Base path: /api/v1/feedback
 */
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback & Decision", description = "Post-interview feedback and final hiring decisions")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "Submit interview feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(feedbackService.submitFeedback(request), "Feedback submitted"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feedback by ID")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getFeedbackById(id)));
    }

    @GetMapping("/interview/{interviewId}")
    @Operation(summary = "Get feedback by interview ID")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getByInterview(@PathVariable Long interviewId) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getFeedbackByInterviewId(interviewId)));
    }

    @GetMapping("/candidate/{candidateId}/summary")
    @Operation(summary = "Get feedback summary for a candidate")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getCandidateSummary(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getFeedbackByCandidateId(candidateId)));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending (unfinalized) feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getPendingFeedbacks()));
    }

    @PutMapping("/{id}/finalize")
    @Operation(summary = "Finalize feedback with hiring decision",
               description = "Records the final HIRED / REJECTED / ON_HOLD decision. Cannot be changed after finalization.")
    public ResponseEntity<ApiResponse<FeedbackResponse>> finalize(
            @PathVariable Long id,
            @Valid @RequestBody FinalizeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackService.finalizeFeedback(id, request), "Feedback finalized"));
    }
}
