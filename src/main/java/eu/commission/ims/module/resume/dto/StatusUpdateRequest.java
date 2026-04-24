package eu.commission.ims.module.resume.dto;

import jakarta.validation.constraints.NotNull;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import lombok.Data;

/**
 * Request body for updating a resume's status.
 */
@Data
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private SubmissionStatus status;
}
