package eu.commission.ims.module.feedback.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for submitting interview feedback.
 */
@Data
public class FeedbackRequest {

    @NotNull(message = "Interview ID is required")
    private Long interviewId;

    @NotNull(message = "Technical score is required")
    @Min(0) @Max(100)
    private Integer technicalScore;

    @NotNull(message = "Behavioral score is required")
    @Min(0) @Max(100)
    private Integer behavioralScore;

    @NotNull(message = "Communication score is required")
    @Min(0) @Max(100)
    private Integer communicationScore;

    @Size(max = 3000)
    private String strengths;

    @Size(max = 3000)
    private String weaknesses;

    @Size(max = 3000)
    private String recommendation;
}
