package eu.commission.ims.module.resume.dto;

import eu.commission.ims.module.resume.entity.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload for resume-related endpoints.
 */
@Data
@Builder
public class ResumeResponse {

    private Long id;

    // Candidate info
    private Long candidateId;
    private String candidateFullName;
    private String candidateEmail;
    private String candidateNationality;

    // Resume info
    private String positionTitle;
    private String department;
    private Integer yearsOfExperience;
    private String linkedInProfile;
    private String coverLetter;
    private SubmissionStatus status;
    private LocalDateTime submittedAt;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
