package eu.commission.ims.module.interview.service;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.interview.dto.InterviewRequest;
import eu.commission.ims.module.interview.dto.InterviewResponse;
import eu.commission.ims.module.interview.dto.RescheduleRequest;
import eu.commission.ims.module.interview.entity.Interview;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.repository.InterviewRepository;
import eu.commission.ims.module.screening.entity.Screening;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
import eu.commission.ims.module.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for the Technical Interview module.
 * Only candidates who PASSED screening can be invited for a technical interview.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ScreeningRepository screeningRepository;

    /**
     * Schedules a new technical interview for a passed screening.
     */
    @Transactional
    public InterviewResponse scheduleInterview(InterviewRequest request) {
        log.info("Scheduling interview for screening ID: {}", request.getScreeningId());

        Screening screening = screeningRepository.findByIdWithDetails(request.getScreeningId())
                .orElseThrow(() -> new ResourceNotFoundException("Screening", "id", request.getScreeningId()));

        if (screening.getDecision() != ScreeningDecision.PASSED) {
            throw new BusinessException(
                    "Only candidates who PASSED screening can be invited for interview. Decision: "
                            + screening.getDecision(),
                    "SCREENING_NOT_PASSED");
        }

        Interview interview = Interview.builder()
                .screening(screening)
                .interviewerName(request.getInterviewerName())
                .interviewerEmail(request.getInterviewerEmail())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .type(request.getType())
                .location(request.getLocation())
                .meetingLink(request.getMeetingLink())
                .build();

        Interview saved = interviewRepository.save(interview);
        log.info("Interview scheduled. ID: {}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Retrieves interview details by ID.
     */
    public InterviewResponse getInterviewById(Long id) {
        Interview interview = interviewRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));
        return toResponse(interview);
    }

    /**
     * Returns all upcoming interviews (within next 30 days).
     */
    public List<InterviewResponse> getUpcomingInterviews() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(30);
        return interviewRepository.findUpcoming(now, future)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Returns all interviews by status.
     */
    public List<InterviewResponse> getInterviewsByStatus(InterviewStatus status) {
        return interviewRepository.findAllByStatus(status)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Reschedules an existing interview.
     */
    @Transactional
    public InterviewResponse rescheduleInterview(Long id, RescheduleRequest request) {
        Interview interview = interviewRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));

        if (interview.getStatus() == InterviewStatus.COMPLETED
                || interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot reschedule a " + interview.getStatus() + " interview.",
                    "INVALID_STATUS_TRANSITION");
        }

        if (request.getScheduledAt() != null) interview.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) interview.setDurationMinutes(request.getDurationMinutes());
        if (request.getLocation() != null) interview.setLocation(request.getLocation());
        if (request.getMeetingLink() != null) interview.setMeetingLink(request.getMeetingLink());
        interview.setStatus(InterviewStatus.RESCHEDULED);

        log.info("Interview {} rescheduled to {}", id, interview.getScheduledAt());
        return toResponse(interviewRepository.save(interview));
    }

    /**
     * Marks an interview as completed.
     */
    @Transactional
    public InterviewResponse completeInterview(Long id) {
        Interview interview = interviewRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new BusinessException("Cannot complete a cancelled interview.", "INVALID_STATUS_TRANSITION");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BusinessException("Interview is already completed.", "ALREADY_COMPLETED");
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        log.info("Interview {} marked as COMPLETED.", id);
        return toResponse(interviewRepository.save(interview));
    }

    /**
     * Cancels an interview.
     */
    @Transactional
    public InterviewResponse cancelInterview(Long id, String reason) {
        Interview interview = interviewRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed interview.", "INVALID_STATUS_TRANSITION");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);
        log.info("Interview {} cancelled. Reason: {}", id, reason);
        return toResponse(interviewRepository.save(interview));
    }

    // ====== Mapper ======

    private InterviewResponse toResponse(Interview i) {
        var screening = i.getScreening();
        var resume = screening.getResume();
        var candidate = resume.getCandidate();
        return InterviewResponse.builder()
                .id(i.getId())
                .screeningId(screening.getId())
                .candidateFullName(candidate.getFirstName() + " " + candidate.getLastName())
                .candidateEmail(candidate.getEmail())
                .positionTitle(resume.getPositionTitle())
                .interviewerName(i.getInterviewerName())
                .interviewerEmail(i.getInterviewerEmail())
                .scheduledAt(i.getScheduledAt())
                .durationMinutes(i.getDurationMinutes())
                .type(i.getType())
                .location(i.getLocation())
                .meetingLink(i.getMeetingLink())
                .status(i.getStatus())
                .completedAt(i.getCompletedAt())
                .cancellationReason(i.getCancellationReason())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
