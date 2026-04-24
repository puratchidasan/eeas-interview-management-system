package eu.commission.ims.module.screening.entity;

public enum ScreeningDecision {
    /** Not yet assessed */
    PENDING,
    /** Candidate meets eligibility criteria — proceeds to interview */
    PASSED,
    /** Candidate does not meet minimum requirements */
    FAILED
}
