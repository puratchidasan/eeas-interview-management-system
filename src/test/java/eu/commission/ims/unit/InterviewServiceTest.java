package eu.commission.ims.unit;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.interview.dto.InterviewRequest;
import eu.commission.ims.module.interview.dto.InterviewResponse;
import eu.commission.ims.module.interview.entity.Interview;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.entity.InterviewType;
import eu.commission.ims.module.interview.repository.InterviewRepository;
import eu.commission.ims.module.interview.service.InterviewService;
import eu.commission.ims.module.resume.entity.Candidate;
import eu.commission.ims.module.resume.entity.Resume;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import eu.commission.ims.module.screening.entity.Screening;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
import eu.commission.ims.module.screening.repository.ScreeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService Unit Tests")
class InterviewServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private ScreeningRepository screeningRepository;
    @InjectMocks private InterviewService interviewService;

    private Screening passedScreening;
    private Interview scheduledInterview;

    @BeforeEach
    void setUp() {
        Candidate c = Candidate.builder().id(1L).firstName("Carla").lastName("Rossi")
                .email("carla@ec.europa.eu").nationality("IT").build();
        Resume r = Resume.builder().id(1L).candidate(c).positionTitle("Policy Officer")
                .department("External").yearsOfExperience(5).status(SubmissionStatus.ACCEPTED).build();

        passedScreening = Screening.builder().id(1L).resume(r)
                .screenerName("HR").eligibilityScore(90)
                .decision(ScreeningDecision.PASSED).build();

        scheduledInterview = Interview.builder()
                .id(1L).screening(passedScreening)
                .interviewerName("Dr. Weber").interviewerEmail("weber@ec.europa.eu")
                .scheduledAt(LocalDateTime.now().plusDays(5)).durationMinutes(60)
                .type(InterviewType.ONLINE).status(InterviewStatus.SCHEDULED).build();
    }

    @Nested
    @DisplayName("scheduleInterview()")
    class ScheduleTests {

        @Test
        @DisplayName("Should schedule interview for PASSED screening")
        void scheduleInterview_PassedScreening_Succeeds() {
            InterviewRequest req = new InterviewRequest();
            req.setScreeningId(1L);
            req.setInterviewerName("Dr. Weber");
            req.setInterviewerEmail("weber@ec.europa.eu");
            req.setScheduledAt(LocalDateTime.now().plusDays(5));
            req.setDurationMinutes(60);
            req.setType(InterviewType.ONLINE);

            when(screeningRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(passedScreening));
            when(interviewRepository.save(any())).thenReturn(scheduledInterview);

            InterviewResponse result = interviewService.scheduleInterview(req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        }

        @Test
        @DisplayName("Should throw BusinessException when screening is PENDING")
        void scheduleInterview_NotPassedScreening_ThrowsException() {
            passedScreening.setDecision(ScreeningDecision.PENDING);
            InterviewRequest req = new InterviewRequest();
            req.setScreeningId(1L);

            when(screeningRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(passedScreening));

            assertThatThrownBy(() -> interviewService.scheduleInterview(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PASSED");
        }
    }

    @Nested
    @DisplayName("completeInterview()")
    class CompleteTests {

        @Test
        @DisplayName("Should mark SCHEDULED interview as COMPLETED")
        void completeInterview_Scheduled_CompletesSuccessfully() {
            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(scheduledInterview));
            when(interviewRepository.save(any())).thenReturn(scheduledInterview);

            InterviewResponse result = interviewService.completeInterview(1L);

            assertThat(scheduledInterview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
            assertThat(scheduledInterview.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw BusinessException when interview is already COMPLETED")
        void completeInterview_AlreadyCompleted_ThrowsException() {
            scheduledInterview.setStatus(InterviewStatus.COMPLETED);
            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(scheduledInterview));

            assertThatThrownBy(() -> interviewService.completeInterview(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("Should throw BusinessException when interview is CANCELLED")
        void completeInterview_Cancelled_ThrowsException() {
            scheduledInterview.setStatus(InterviewStatus.CANCELLED);
            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(scheduledInterview));

            assertThatThrownBy(() -> interviewService.completeInterview(1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("cancelInterview()")
    class CancelTests {

        @Test
        @DisplayName("Should cancel a SCHEDULED interview")
        void cancelInterview_Scheduled_CancelsSuccessfully() {
            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(scheduledInterview));
            when(interviewRepository.save(any())).thenReturn(scheduledInterview);

            interviewService.cancelInterview(1L, "Candidate withdrew");

            assertThat(scheduledInterview.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
            assertThat(scheduledInterview.getCancellationReason()).isEqualTo("Candidate withdrew");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to cancel a COMPLETED interview")
        void cancelInterview_Completed_ThrowsException() {
            scheduledInterview.setStatus(InterviewStatus.COMPLETED);
            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(scheduledInterview));

            assertThatThrownBy(() -> interviewService.cancelInterview(1L, "test"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getInterviewById()")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown ID")
        void getInterviewById_NotFound_ThrowsException() {
            when(interviewRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> interviewService.getInterviewById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Interview");
        }
    }
}
