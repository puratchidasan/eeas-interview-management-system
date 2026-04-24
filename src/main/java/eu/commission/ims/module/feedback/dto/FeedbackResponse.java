package eu.commission.ims.module.feedback.dto;

import eu.commission.ims.module.feedback.entity.FinalDecision;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload for feedback-related endpoints.
 */
@Data
@Builder
public class FeedbackResponse {

    private Long id;

    // Interview context
    private Long interviewId;
    private String candidateFullName;
    private String candidateEmail;
    private String positionTitle;
    private String interviewerName;

    // Scores
    private Integer technicalScore;
    private Integer behavioralScore;
    private Integer communicationScore;
    private Double overallScore;

    // Assessment
    private String strengths;
    private String weaknesses;
    private String recommendation;
    private FinalDecision finalDecision;
    private Boolean isFinalized;
    private LocalDateTime finalizedAt;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
