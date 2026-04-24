package eu.commission.ims.module.screening.dto;

import eu.commission.ims.module.screening.entity.ScreeningDecision;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload for screening-related endpoints.
 */
@Data
@Builder
public class ScreeningResponse {

    private Long id;

    // Linked resume info
    private Long resumeId;
    private String candidateFullName;
    private String candidateEmail;
    private String positionTitle;

    // Screening details
    private String screenerName;
    private Integer eligibilityScore;
    private String notes;
    private ScreeningDecision decision;
    private LocalDateTime screenedAt;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
