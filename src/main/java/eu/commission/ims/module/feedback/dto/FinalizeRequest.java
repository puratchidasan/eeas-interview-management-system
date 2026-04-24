package eu.commission.ims.module.feedback.dto;

import jakarta.validation.constraints.NotNull;
import eu.commission.ims.module.feedback.entity.FinalDecision;
import lombok.Data;

/**
 * Request body for finalizing a feedback with a hiring decision.
 */
@Data
public class FinalizeRequest {

    @NotNull(message = "Final decision is required")
    private FinalDecision finalDecision;

    private String recommendation;
}
