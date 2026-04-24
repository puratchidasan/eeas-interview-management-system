package eu.commission.ims.module.interview.dto;

import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.entity.InterviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload for interview-related endpoints.
 */
@Data
@Builder
public class InterviewResponse {

    private Long id;

    // Candidate info (via screening -> resume -> candidate)
    private Long screeningId;
    private String candidateFullName;
    private String candidateEmail;
    private String positionTitle;

    // Interview details
    private String interviewerName;
    private String interviewerEmail;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private InterviewType type;
    private String location;
    private String meetingLink;
    private InterviewStatus status;
    private LocalDateTime completedAt;
    private String cancellationReason;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
