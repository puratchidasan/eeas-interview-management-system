package eu.commission.ims.unit;

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
import eu.commission.ims.module.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ResumeService}.
 * Uses Mockito to mock all repository dependencies.
 * Coverage target: service logic only (no Spring context loaded).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService Unit Tests")
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private ResumeService resumeService;

    private Candidate testCandidate;
    private Resume testResume;
    private ResumeRequest validRequest;

    @BeforeEach
    void setUp() {
        testCandidate = Candidate.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Fontaine")
                .email("alice@ec.europa.eu")
                .nationality("FR")
                .phoneNumber("+33612345678")
                .build();

        testResume = Resume.builder()
                .id(1L)
                .candidate(testCandidate)
                .positionTitle("Software Engineer")
                .department("Digital Infrastructure")
                .yearsOfExperience(5)
                .linkedInProfile("https://linkedin.com/in/alice")
                .coverLetter("Motivated candidate")
                .status(SubmissionStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        validRequest = new ResumeRequest();
        validRequest.setFirstName("Alice");
        validRequest.setLastName("Fontaine");
        validRequest.setEmail("alice@ec.europa.eu");
        validRequest.setNationality("FR");
        validRequest.setPhoneNumber("+33612345678");
        validRequest.setPositionTitle("Software Engineer");
        validRequest.setDepartment("Digital Infrastructure");
        validRequest.setYearsOfExperience(5);
        validRequest.setCoverLetter("Motivated candidate");
    }

    // =========================================================
    // submitResume
    // =========================================================
    @Nested
    @DisplayName("submitResume()")
    class SubmitResumeTests {

        @Test
        @DisplayName("Should create new candidate and submit resume when email is new")
        void submitResume_NewCandidate_CreatesAndSaves() {
            when(candidateRepository.findByEmail("alice@ec.europa.eu")).thenReturn(Optional.empty());
            when(candidateRepository.save(any(Candidate.class))).thenReturn(testCandidate);
            when(resumeRepository.existsByCandidateIdAndStatus(1L, SubmissionStatus.SUBMITTED)).thenReturn(false);
            when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

            ResumeResponse result = resumeService.submitResume(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getCandidateEmail()).isEqualTo("alice@ec.europa.eu");
            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(result.getPositionTitle()).isEqualTo("Software Engineer");

            verify(candidateRepository).save(any(Candidate.class));
            verify(resumeRepository).save(any(Resume.class));
        }

        @Test
        @DisplayName("Should reuse existing candidate when email is already registered")
        void submitResume_ExistingCandidate_ReusesCandidate() {
            when(candidateRepository.findByEmail("alice@ec.europa.eu")).thenReturn(Optional.of(testCandidate));
            when(resumeRepository.existsByCandidateIdAndStatus(1L, SubmissionStatus.SUBMITTED)).thenReturn(false);
            when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

            resumeService.submitResume(validRequest);

            verify(candidateRepository, never()).save(any(Candidate.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when candidate already has active submission")
        void submitResume_DuplicateSubmission_ThrowsBusinessException() {
            when(candidateRepository.findByEmail("alice@ec.europa.eu")).thenReturn(Optional.of(testCandidate));
            when(resumeRepository.existsByCandidateIdAndStatus(1L, SubmissionStatus.SUBMITTED)).thenReturn(true);

            assertThatThrownBy(() -> resumeService.submitResume(validRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("active resume submission");
        }
    }

    // =========================================================
    // getResumeById
    // =========================================================
    @Nested
    @DisplayName("getResumeById()")
    class GetResumeByIdTests {

        @Test
        @DisplayName("Should return resume when found")
        void getResumeById_Found_ReturnsResponse() {
            when(resumeRepository.findByIdWithCandidate(1L)).thenReturn(Optional.of(testResume));

            ResumeResponse result = resumeService.getResumeById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCandidateFullName()).isEqualTo("Alice Fontaine");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when resume not found")
        void getResumeById_NotFound_ThrowsException() {
            when(resumeRepository.findByIdWithCandidate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resumeService.getResumeById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resume")
                    .hasMessageContaining("99");
        }
    }

    // =========================================================
    // getAllResumes
    // =========================================================
    @Nested
    @DisplayName("getAllResumes()")
    class GetAllResumesTests {

        @Test
        @DisplayName("Should return paginated resumes without status filter")
        void getAllResumes_NoFilter_ReturnsPaged() {
            Page<Resume> page = new PageImpl<>(List.of(testResume));
            when(resumeRepository.findAll(any(PageRequest.class))).thenReturn(page);

            Page<ResumeResponse> result = resumeService.getAllResumes(null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter by status when provided")
        void getAllResumes_WithStatusFilter_FiltersCorrectly() {
            Page<Resume> page = new PageImpl<>(List.of(testResume));
            when(resumeRepository.findAllByStatus(eq(SubmissionStatus.SUBMITTED), any())).thenReturn(page);

            Page<ResumeResponse> result = resumeService.getAllResumes(SubmissionStatus.SUBMITTED, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            verify(resumeRepository).findAllByStatus(eq(SubmissionStatus.SUBMITTED), any());
        }
    }

    // =========================================================
    // updateResumeStatus
    // =========================================================
    @Nested
    @DisplayName("updateResumeStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update resume status successfully")
        void updateResumeStatus_ValidTransition_UpdatesStatus() {
            StatusUpdateRequest request = new StatusUpdateRequest();
            request.setStatus(SubmissionStatus.UNDER_REVIEW);

            when(resumeRepository.findByIdWithCandidate(1L)).thenReturn(Optional.of(testResume));
            when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

            ResumeResponse result = resumeService.updateResumeStatus(1L, request);

            assertThat(result).isNotNull();
            verify(resumeRepository).save(testResume);
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to update WITHDRAWN resume")
        void updateResumeStatus_WithdrawnResume_ThrowsException() {
            testResume.setStatus(SubmissionStatus.WITHDRAWN);
            StatusUpdateRequest request = new StatusUpdateRequest();
            request.setStatus(SubmissionStatus.SUBMITTED);

            when(resumeRepository.findByIdWithCandidate(1L)).thenReturn(Optional.of(testResume));

            assertThatThrownBy(() -> resumeService.updateResumeStatus(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("withdrawn");
        }
    }

    // =========================================================
    // withdrawResume
    // =========================================================
    @Nested
    @DisplayName("withdrawResume()")
    class WithdrawResumeTests {

        @Test
        @DisplayName("Should withdraw resume successfully")
        void withdrawResume_ValidResume_WithdrawsSuccessfully() {
            when(resumeRepository.findByIdWithCandidate(1L)).thenReturn(Optional.of(testResume));
            when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

            resumeService.withdrawResume(1L);

            assertThat(testResume.getStatus()).isEqualTo(SubmissionStatus.WITHDRAWN);
            verify(resumeRepository).save(testResume);
        }

        @Test
        @DisplayName("Should throw BusinessException when resume is already withdrawn")
        void withdrawResume_AlreadyWithdrawn_ThrowsException() {
            testResume.setStatus(SubmissionStatus.WITHDRAWN);
            when(resumeRepository.findByIdWithCandidate(1L)).thenReturn(Optional.of(testResume));

            assertThatThrownBy(() -> resumeService.withdrawResume(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already withdrawn");
        }
    }
}
