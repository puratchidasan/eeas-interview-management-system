package eu.commission.ims.module.screening.service;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.resume.entity.Resume;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import eu.commission.ims.module.resume.repository.ResumeRepository;
import eu.commission.ims.module.screening.dto.DecisionRequest;
import eu.commission.ims.module.screening.dto.ScreeningRequest;
import eu.commission.ims.module.screening.dto.ScreeningResponse;
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
 * Service layer for the Screening module.
 * Enforces that only SUBMITTED resumes can be screened,
 * and tracks the pass/fail decision per screening.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final ResumeRepository resumeRepository;

    /**
     * Creates a new screening for a submitted resume.
     * Resumes must be in SUBMITTED or UNDER_REVIEW status.
     */
    @Transactional
    public ScreeningResponse createScreening(ScreeningRequest request) {
        log.info("Creating screening for resume ID: {}", request.getResumeId());

        Resume resume = resumeRepository.findByIdWithCandidate(request.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", request.getResumeId()));

        if (resume.getStatus() != SubmissionStatus.SUBMITTED
                && resume.getStatus() != SubmissionStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    "Only SUBMITTED or UNDER_REVIEW resumes can be screened. Current status: " + resume.getStatus(),
                    "INVALID_RESUME_STATUS");
        }

        if (screeningRepository.existsByResumeId(request.getResumeId())) {
            throw new BusinessException(
                    "A screening already exists for resume ID: " + request.getResumeId(),
                    "DUPLICATE_SCREENING");
        }

        // Mark resume as under review
        resume.setStatus(SubmissionStatus.UNDER_REVIEW);
        resumeRepository.save(resume);

        Screening screening = Screening.builder()
                .resume(resume)
                .screenerName(request.getScreenerName())
                .eligibilityScore(request.getEligibilityScore())
                .notes(request.getNotes())
                .build();

        Screening saved = screeningRepository.save(screening);
        log.info("Screening created. ID: {}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Gets screening details by screening ID.
     */
    public ScreeningResponse getScreeningById(Long id) {
        Screening screening = screeningRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening", "id", id));
        return toResponse(screening);
    }

    /**
     * Gets the screening for a specific resume.
     */
    public ScreeningResponse getScreeningByResumeId(Long resumeId) {
        Screening screening = screeningRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening", "resumeId", resumeId));
        return toResponse(screening);
    }

    /**
     * Returns all screenings with PENDING decision.
     */
    public List<ScreeningResponse> getPendingScreenings() {
        return screeningRepository.findAllByDecision(ScreeningDecision.PENDING)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Records the pass/fail decision for a screening.
     * If PASSED, the resume is promoted to ACCEPTED status.
     * If FAILED, the resume is marked as REJECTED.
     */
    @Transactional
    public ScreeningResponse recordDecision(Long id, DecisionRequest request) {
        Screening screening = screeningRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening", "id", id));

        if (screening.getDecision() != ScreeningDecision.PENDING) {
            throw new BusinessException(
                    "Decision already recorded for this screening: " + screening.getDecision(),
                    "DECISION_ALREADY_SET");
        }

        screening.setDecision(request.getDecision());
        screening.setScreenedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            screening.setNotes(request.getNotes());
        }

        // Update resume status based on decision
        Resume resume = screening.getResume();
        if (request.getDecision() == ScreeningDecision.PASSED) {
            resume.setStatus(SubmissionStatus.ACCEPTED);
        } else if (request.getDecision() == ScreeningDecision.FAILED) {
            resume.setStatus(SubmissionStatus.REJECTED);
        }
        resumeRepository.save(resume);

        Screening updated = screeningRepository.save(screening);
        log.info("Screening {} decision recorded: {}", id, request.getDecision());
        return toResponse(updated);
    }

    // ====== Mapper ======

    private ScreeningResponse toResponse(Screening s) {
        var resume = s.getResume();
        var candidate = resume.getCandidate();
        return ScreeningResponse.builder()
                .id(s.getId())
                .resumeId(resume.getId())
                .candidateFullName(candidate.getFirstName() + " " + candidate.getLastName())
                .candidateEmail(candidate.getEmail())
                .positionTitle(resume.getPositionTitle())
                .screenerName(s.getScreenerName())
                .eligibilityScore(s.getEligibilityScore())
                .notes(s.getNotes())
                .decision(s.getDecision())
                .screenedAt(s.getScreenedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
