package eu.commission.ims.module.screening.dto;

import jakarta.validation.constraints.NotNull;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
import lombok.Data;

/**
 * Request body for updating a screening decision.
 */
@Data
public class DecisionRequest {

    @NotNull(message = "Decision is required")
    private ScreeningDecision decision;

    private String notes;
}
