package eu.commission.ims.module.interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body for rescheduling an interview.
 */
@Data
public class RescheduleRequest {

    @Future(message = "New interview time must be in the future")
    private LocalDateTime scheduledAt;

    @Min(15)
    @Max(480)
    private Integer durationMinutes;

    private String location;
    private String meetingLink;
    private String reason;
}
