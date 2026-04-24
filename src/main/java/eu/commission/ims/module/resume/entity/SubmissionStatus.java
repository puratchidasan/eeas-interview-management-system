package eu.commission.ims.module.resume.entity;

/**
 * Tracks the lifecycle state of a candidate's resume submission.
 */
public enum SubmissionStatus {

    /** Candidate has started but not submitted */
    DRAFT,

    /** Formally submitted for HR review */
    SUBMITTED,

    /** Currently being assessed by HR */
    UNDER_REVIEW,

    /** Passed initial screening — moved to interview stage */
    ACCEPTED,

    /** Did not meet eligibility criteria */
    REJECTED,

    /** Withdrawn by the candidate */
    WITHDRAWN
}
