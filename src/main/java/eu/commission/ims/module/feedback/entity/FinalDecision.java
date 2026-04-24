package eu.commission.ims.module.feedback.entity;

public enum FinalDecision {
    /** Candidate is selected for the position */
    HIRED,
    /** Candidate is not suitable for the position */
    REJECTED,
    /** Keep on hold — reserve list or pending decision */
    ON_HOLD
}
