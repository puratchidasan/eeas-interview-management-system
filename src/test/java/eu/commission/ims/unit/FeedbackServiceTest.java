package eu.commission.ims.unit;

import eu.commission.ims.common.exception.BusinessException;
import eu.commission.ims.common.exception.ResourceNotFoundException;
import eu.commission.ims.module.feedback.dto.FeedbackRequest;
import eu.commission.ims.module.feedback.dto.FeedbackResponse;
import eu.commission.ims.module.feedback.dto.FinalizeRequest;
import eu.commission.ims.module.feedback.entity.Feedback;
import eu.commission.ims.module.feedback.entity.FinalDecision;
import eu.commission.ims.module.feedback.repository.FeedbackRepository;
import eu.commission.ims.module.feedback.service.FeedbackService;
import eu.commission.ims.module.interview.entity.Interview;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import eu.commission.ims.module.interview.entity.InterviewType;
import eu.commission.ims.module.interview.repository.InterviewRepository;
import eu.commission.ims.module.resume.entity.Candidate;
import eu.commission.ims.module.resume.entity.Resume;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import eu.commission.ims.module.screening.entity.Screening;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
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
@DisplayName("FeedbackService Unit Tests")
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private InterviewRepository interviewRepository;
    @InjectMocks private FeedbackService feedbackService;

    private Interview completedInterview;
    private Feedback draftFeedback;

    @BeforeEach
    void setUp() {
        Candidate c = Candidate.builder().id(1L).firstName("David").lastName("Chen")
                .email("david@ec.europa.eu").nationality("BE").build();
        Resume r = Resume.builder().id(1L).candidate(c).positionTitle("Economist")
                .department("Finance").yearsOfExperience(8).status(SubmissionStatus.ACCEPTED).build();
        Screening s = Screening.builder().id(1L).resume(r).screenerName("HR")
                .decision(ScreeningDecision.PASSED).build();

        completedInterview = Interview.builder()
                .id(1L).screening(s)
                .interviewerName("Prof. Laurent").interviewerEmail("laurent@ec.europa.eu")
                .scheduledAt(LocalDateTime.now().minusDays(1)).durationMinutes(90)
                .type(InterviewType.ONSITE).status(InterviewStatus.COMPLETED)
                .completedAt(LocalDateTime.now()).build();

        draftFeedback = Feedback.builder()
                .id(1L).interview(completedInterview)
                .technicalScore(85).behavioralScore(78).communicationScore(90)
                .strengths("Strong analytical skills").weaknesses("Limited EC-specific experience")
                .isFinalized(false).build();
        draftFeedback.calculateOverallScore();
    }

    @Nested
    @DisplayName("submitFeedback()")
    class SubmitFeedbackTests {

        @Test
        @DisplayName("Should submit feedback for COMPLETED interview")
        void submitFeedback_CompletedInterview_Succeeds() {
            FeedbackRequest req = new FeedbackRequest();
            req.setInterviewId(1L);
            req.setTechnicalScore(85);
            req.setBehavioralScore(78);
            req.setCommunicationScore(90);
            req.setStrengths("Strong analytical skills");

            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(completedInterview));
            when(feedbackRepository.existsByInterviewId(1L)).thenReturn(false);
            when(feedbackRepository.save(any())).thenReturn(draftFeedback);

            FeedbackResponse result = feedbackService.submitFeedback(req);

            assertThat(result).isNotNull();
            assertThat(result.getTechnicalScore()).isEqualTo(85);
            assertThat(result.getOverallScore()).isGreaterThan(0.0);
            assertThat(result.getIsFinalized()).isFalse();
        }

        @Test
        @DisplayName("Should throw BusinessException when interview is not COMPLETED")
        void submitFeedback_NotCompleted_ThrowsException() {
            completedInterview.setStatus(InterviewStatus.SCHEDULED);
            FeedbackRequest req = new FeedbackRequest();
            req.setInterviewId(1L);
            req.setTechnicalScore(80);
            req.setBehavioralScore(80);
            req.setCommunicationScore(80);

            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(completedInterview));

            assertThatThrownBy(() -> feedbackService.submitFeedback(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Should throw BusinessException on duplicate feedback submission")
        void submitFeedback_Duplicate_ThrowsException() {
            FeedbackRequest req = new FeedbackRequest();
            req.setInterviewId(1L);
            req.setTechnicalScore(80);
            req.setBehavioralScore(80);
            req.setCommunicationScore(80);

            when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(completedInterview));
            when(feedbackRepository.existsByInterviewId(1L)).thenReturn(true);

            assertThatThrownBy(() -> feedbackService.submitFeedback(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("finalizeFeedback()")
    class FinalizeFeedbackTests {

        @Test
        @DisplayName("Should finalize with HIRED decision")
        void finalizeFeedback_NotFinalized_FinalizesWithDecision() {
            FinalizeRequest req = new FinalizeRequest();
            req.setFinalDecision(FinalDecision.HIRED);

            when(feedbackRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftFeedback));
            when(feedbackRepository.save(any())).thenReturn(draftFeedback);

            feedbackService.finalizeFeedback(1L, req);

            assertThat(draftFeedback.getFinalDecision()).isEqualTo(FinalDecision.HIRED);
            assertThat(draftFeedback.getIsFinalized()).isTrue();
            assertThat(draftFeedback.getFinalizedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw BusinessException when feedback is already finalized")
        void finalizeFeedback_AlreadyFinalized_ThrowsException() {
            draftFeedback.setIsFinalized(true);
            draftFeedback.setFinalDecision(FinalDecision.REJECTED);
            FinalizeRequest req = new FinalizeRequest();
            req.setFinalDecision(FinalDecision.HIRED);

            when(feedbackRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftFeedback));

            assertThatThrownBy(() -> feedbackService.finalizeFeedback(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already been finalized");
        }
    }

    @Nested
    @DisplayName("getFeedbackById()")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown feedback ID")
        void getFeedbackById_NotFound_ThrowsException() {
            when(feedbackRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.getFeedbackById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should verify weighted overall score calculation")
        void submitFeedback_OverallScoreCalculation_IsCorrect() {
            // technical=80, behavioral=60, communication=100
            // expected = 80*0.5 + 60*0.3 + 100*0.2 = 40 + 18 + 20 = 78.0
            Feedback fb = Feedback.builder()
                    .id(2L).interview(completedInterview)
                    .technicalScore(80).behavioralScore(60).communicationScore(100)
                    .isFinalized(false).build();
            fb.calculateOverallScore();

            assertThat(fb.getOverallScore()).isEqualTo(78.0);
        }
    }
}
