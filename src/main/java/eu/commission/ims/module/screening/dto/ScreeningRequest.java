package eu.commission.ims.module.screening.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for creating a new screening record.
 */
@Data
public class ScreeningRequest {

    @NotNull(message = "Resume ID is required")
    private Long resumeId;

    @NotBlank(message = "Screener name is required")
    @Size(max = 200)
    private String screenerName;

    @Min(value = 0, message = "Score cannot be negative")
    @Max(value = 100, message = "Score cannot exceed 100")
    private Integer eligibilityScore;

    @Size(max = 5000)
    private String notes;
}
