package eu.commission.ims.module.interview.dto;

import eu.commission.ims.module.interview.entity.InterviewType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body for scheduling a new technical interview.
 */
@Data
public class InterviewRequest {

    @NotNull(message = "Screening ID is required")
    private Long screeningId;

    @NotBlank(message = "Interviewer name is required")
    @Size(max = 200)
    private String interviewerName;

    @NotBlank(message = "Interviewer email is required")
    @Email
    private String interviewerEmail;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Interview must be scheduled in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 15, message = "Interview must be at least 15 minutes")
    @Max(value = 480, message = "Interview cannot exceed 8 hours")
    private Integer durationMinutes;

    private InterviewType type = InterviewType.ONLINE;

    @Size(max = 500)
    private String location;

    @Size(max = 500)
    private String meetingLink;
}
