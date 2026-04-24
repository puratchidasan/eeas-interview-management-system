package eu.commission.ims.module.resume.service;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.resume.dto.ResumeRequest;
import eu.commission.ims.module.resume.dto.ResumeResponse;
import eu.commission.ims.module.resume.dto.StatusUpdateRequest;
import eu.commission.ims.module.resume.entity.Candidate;
import eu.commission.ims.module.resume.entity.Resume;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import eu.commission.ims.module.resume.repository.CandidateRepository;
import eu.commission.ims.module.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Resume Submission module.
 * Handles all business logic for candidate registration and resume lifecycle management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;

    /**
     * Submits a new resume. Creates a new candidate if email is not registered.
     * Prevents duplicate active submissions from the same candidate.
     *
     * @param request the resume submission data
     * @return the created resume as a response DTO
     */
    @Transactional
    public ResumeResponse submitResume(ResumeRequest request) {
        log.info("Processing resume submission for email: {}", request.getEmail());

        // Resolve or create candidate
        Candidate candidate = candidateRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    Candidate newCandidate = Candidate.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .nationality(request.getNationality())
                            .phoneNumber(request.getPhoneNumber())
                            .build();
                    return candidateRepository.save(newCandidate);
                });

        // Prevent duplicate active submissions
        boolean hasActiveSubmission = resumeRepository
                .existsByCandidateIdAndStatus(candidate.getId(), SubmissionStatus.SUBMITTED);
        if (hasActiveSubmission) {
            throw new BusinessException(
                    "Candidate already has an active resume submission in progress.",
                    "DUPLICATE_SUBMISSION");
        }

        Resume resume = Resume.builder()
                .candidate(candidate)
                .positionTitle(request.getPositionTitle())
                .department(request.getDepartment())
                .yearsOfExperience(request.getYearsOfExperience())
                .linkedInProfile(request.getLinkedInProfile())
                .coverLetter(request.getCoverLetter())
                .build();
        resume.submit();

        Resume saved = resumeRepository.save(resume);
        log.info("Resume submitted successfully. ID: {}, Candidate: {}", saved.getId(), candidate.getEmail());
        return toResponse(saved);
    }

    /**
     * Retrieves a resume by its ID.
     */
    public ResumeResponse getResumeById(Long id) {
        Resume resume = resumeRepository.findByIdWithCandidate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));
        return toResponse(resume);
    }

    /**
     * Returns a paginated list of resumes, optionally filtered by status.
     */
    public Page<ResumeResponse> getAllResumes(SubmissionStatus status, Pageable pageable) {
        if (status != null) {
            return resumeRepository.findAllByStatus(status, pageable).map(this::toResponse);
        }
        return resumeRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Updates the status of a resume (e.g., UNDER_REVIEW, ACCEPTED, REJECTED).
     */
    @Transactional
    public ResumeResponse updateResumeStatus(Long id, StatusUpdateRequest request) {
        Resume resume = resumeRepository.findByIdWithCandidate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));

        if (resume.getStatus() == SubmissionStatus.WITHDRAWN) {
            throw new BusinessException("Cannot update status of a withdrawn resume.", "INVALID_STATUS_TRANSITION");
        }

        log.info("Updating resume {} status: {} -> {}", id, resume.getStatus(), request.getStatus());
        resume.setStatus(request.getStatus());
        return toResponse(resumeRepository.save(resume));
    }

    /**
     * Withdraws a resume by marking it as WITHDRAWN.
     */
    @Transactional
    public void withdrawResume(Long id) {
        Resume resume = resumeRepository.findByIdWithCandidate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));

        if (resume.getStatus() == SubmissionStatus.WITHDRAWN) {
            throw new BusinessException("Resume is already withdrawn.", "ALREADY_WITHDRAWN");
        }
        resume.setStatus(SubmissionStatus.WITHDRAWN);
        resumeRepository.save(resume);
        log.info("Resume {} withdrawn.", id);
    }

    /**
     * Returns all resumes for a given candidate.
     */
    public List<ResumeResponse> getResumesByCandidate(Long candidateId) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new ResourceNotFoundException("Candidate", "id", candidateId);
        }
        return resumeRepository.findByCandidateId(candidateId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ====== Mapper ======

    private ResumeResponse toResponse(Resume resume) {
        Candidate c = resume.getCandidate();
        return ResumeResponse.builder()
                .id(resume.getId())
                .candidateId(c.getId())
                .candidateFullName(c.getFirstName() + " " + c.getLastName())
                .candidateEmail(c.getEmail())
                .candidateNationality(c.getNationality())
                .positionTitle(resume.getPositionTitle())
                .department(resume.getDepartment())
                .yearsOfExperience(resume.getYearsOfExperience())
                .linkedInProfile(resume.getLinkedInProfile())
                .coverLetter(resume.getCoverLetter())
                .status(resume.getStatus())
                .submittedAt(resume.getSubmittedAt())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}
