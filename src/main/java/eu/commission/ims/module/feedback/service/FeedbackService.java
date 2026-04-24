package eu.commission.ims.module.feedback.service;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.feedback.dto.FeedbackRequest;
import eu.commission.ims.module.feedback.dto.FeedbackResponse;
import eu.commission.ims.module.feedback.dto.FinalizeRequest;
import eu.commission.ims.module.feedback.entity.Feedback;
import eu.commission.ims.module.feedback.repository.FeedbackRepository;
import eu.commission.ims.module.interview.entity.Interview;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for the Feedback & Decision module.
 * Feedback can only be submitted for COMPLETED interviews.
 * Once finalized, feedback cannot be changed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;

    /**
     * Submits feedback for a completed interview.
     */
    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request) {
        log.info("Submitting feedback for interview ID: {}", request.getInterviewId());

        Interview interview = interviewRepository.findByIdWithDetails(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", request.getInterviewId()));

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new BusinessException(
                    "Feedback can only be submitted for COMPLETED interviews. Current status: "
                            + interview.getStatus(),
                    "INTERVIEW_NOT_COMPLETED");
        }

        if (feedbackRepository.existsByInterviewId(request.getInterviewId())) {
            throw new BusinessException(
                    "Feedback already exists for interview ID: " + request.getInterviewId(),
                    "DUPLICATE_FEEDBACK");
        }

        Feedback feedback = Feedback.builder()
                .interview(interview)
                .technicalScore(request.getTechnicalScore())
                .behavioralScore(request.getBehavioralScore())
                .communicationScore(request.getCommunicationScore())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .recommendation(request.getRecommendation())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback submitted. ID: {}. Overall score: {}", saved.getId(), saved.getOverallScore());
        return toResponse(saved);
    }

    /**
     * Retrieves feedback by ID.
     */
    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", id));
        return toResponse(feedback);
    }

    /**
     * Retrieves feedback for a specific interview.
     */
    public FeedbackResponse getFeedbackByInterviewId(Long interviewId) {
        Feedback feedback = feedbackRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "interviewId", interviewId));
        return toResponse(feedback);
    }

    /**
     * Returns feedback summary for a candidate across all interviews.
     */
    public List<FeedbackResponse> getFeedbackByCandidateId(Long candidateId) {
        return feedbackRepository.findByCandidateId(candidateId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Finalizes feedback with a hiring decision (HIRED / REJECTED / ON_HOLD).
     * Once finalized, feedback is locked.
     */
    @Transactional
    public FeedbackResponse finalizeFeedback(Long id, FinalizeRequest request) {
        Feedback feedback = feedbackRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", id));

        if (Boolean.TRUE.equals(feedback.getIsFinalized())) {
            throw new BusinessException(
                    "Feedback has already been finalized with decision: " + feedback.getFinalDecision(),
                    "ALREADY_FINALIZED");
        }

        feedback.setFinalDecision(request.getFinalDecision());
        if (request.getRecommendation() != null) {
            feedback.setRecommendation(request.getRecommendation());
        }
        feedback.setIsFinalized(true);
        feedback.setFinalizedAt(LocalDateTime.now());

        Feedback updated = feedbackRepository.save(feedback);
        log.info("Feedback {} finalized. Decision: {}", id, request.getFinalDecision());
        return toResponse(updated);
    }

    /**
     * Returns all pending (not yet finalized) feedbacks.
     */
    public List<FeedbackResponse> getPendingFeedbacks() {
        return feedbackRepository.findAllByIsFinalized(false)
                .stream().map(this::toResponse).toList();
    }

    // ====== Mapper ======

    private FeedbackResponse toResponse(Feedback f) {
        var interview = f.getInterview();
        var screening = interview.getScreening();
        var resume = screening.getResume();
        var candidate = resume.getCandidate();
        return FeedbackResponse.builder()
                .id(f.getId())
                .interviewId(interview.getId())
                .candidateFullName(candidate.getFirstName() + " " + candidate.getLastName())
                .candidateEmail(candidate.getEmail())
                .positionTitle(resume.getPositionTitle())
                .interviewerName(interview.getInterviewerName())
                .technicalScore(f.getTechnicalScore())
                .behavioralScore(f.getBehavioralScore())
                .communicationScore(f.getCommunicationScore())
                .overallScore(f.getOverallScore())
                .strengths(f.getStrengths())
                .weaknesses(f.getWeaknesses())
                .recommendation(f.getRecommendation())
                .finalDecision(f.getFinalDecision())
                .isFinalized(f.getIsFinalized())
                .finalizedAt(f.getFinalizedAt())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
