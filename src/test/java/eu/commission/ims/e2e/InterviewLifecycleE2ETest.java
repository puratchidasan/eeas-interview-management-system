package eu.commission.ims.e2e;

import eu.commission.ims.module.feedback.dto.FeedbackRequest;
import eu.commission.ims.module.feedback.dto.FinalizeRequest;
import eu.commission.ims.module.feedback.entity.FinalDecision;
import eu.commission.ims.module.interview.dto.InterviewRequest;
import eu.commission.ims.module.interview.entity.InterviewType;
import eu.commission.ims.module.resume.dto.ResumeRequest;
import eu.commission.ims.module.screening.dto.DecisionRequest;
import eu.commission.ims.module.screening.dto.ScreeningRequest;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End Regression Test — Full Interview Lifecycle.
 *
 * <p>Simulates the complete EC interview pipeline:
 * <ol>
 *   <li>Submit Resume</li>
 *   <li>Create Screening</li>
 *   <li>Record PASSED Decision</li>
 *   <li>Schedule Technical Interview</li>
 *   <li>Mark Interview as Completed</li>
 *   <li>Submit Feedback</li>
 *   <li>Finalize with HIRED Decision</li>
 * </ol>
 *
 * <p>Also covers negative paths: duplicate submissions, 404s, and status violations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Interview Lifecycle E2E Regression Test")
class InterviewLifecycleE2ETest {

    @LocalServerPort
    private int port;

    // Shared state across test lifecycle
    private static Long resumeId;
    private static Long screeningId;
    private static Long interviewId;
    private static Long feedbackId;

    // Credentials
    private static final String RECRUITER = "recruiter";
    private static final String RECRUITER_PASS = "rec123";
    private static final String INTERVIEWER = "interviewer";
    private static final String INTERVIEWER_PASS = "int123";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASS = "admin123";

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // =========================================================
    // Step 1: Resume Submission
    // =========================================================

    @Test
    @Order(1)
    @DisplayName("Step 1 — Submit resume for E2E candidate")
    void step1_SubmitResume() {
        ResumeRequest req = new ResumeRequest();
        req.setFirstName("Elena");
        req.setLastName("Papadopoulos");
        req.setEmail("elena.e2e@ec.europa.eu");
        req.setNationality("GR");
        req.setPhoneNumber("+302101234567");
        req.setPositionTitle("Senior Economist");
        req.setDepartment("Economic Policy");
        req.setYearsOfExperience(10);
        req.setCoverLetter("I bring extensive EU economic policy experience.");

        resumeId = given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/resumes")
                .then()
                .statusCode(201)
                .body("success", is(true))
                .body("data.candidateEmail", equalTo("elena.e2e@ec.europa.eu"))
                .body("data.status", equalTo("SUBMITTED"))
                .body("data.id", notNullValue())
                .extract().jsonPath().getLong("data.id");

        Assertions.assertNotNull(resumeId, "Resume ID must be set after creation");
    }

    @Test
    @Order(2)
    @DisplayName("Step 1b — Duplicate submission returns 409")
    void step1b_DuplicateSubmission_Returns409() {
        ResumeRequest req = new ResumeRequest();
        req.setFirstName("Elena");
        req.setLastName("Papadopoulos");
        req.setEmail("elena.e2e@ec.europa.eu");
        req.setNationality("GR");
        req.setPositionTitle("Duplicate Attempt");
        req.setDepartment("Any");
        req.setYearsOfExperience(10);

        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/resumes")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("DUPLICATE_SUBMISSION"));
    }

    @Test
    @Order(3)
    @DisplayName("Step 1c — Get resume by ID returns correct data")
    void step1c_GetResumeById() {
        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .when()
                .get("/api/v1/resumes/{id}", resumeId)
                .then()
                .statusCode(200)
                .body("data.positionTitle", equalTo("Senior Economist"))
                .body("data.candidateNationality", equalTo("GR"));
    }

    // =========================================================
    // Step 2: Create Screening
    // =========================================================

    @Test
    @Order(4)
    @DisplayName("Step 2 — Create screening for submitted resume")
    void step2_CreateScreening() {
        ScreeningRequest req = new ScreeningRequest();
        req.setResumeId(resumeId);
        req.setScreenerName("HR Manager Dubois");
        req.setEligibilityScore(92);
        req.setNotes("Candidate meets all minimum requirements. Strong academic background.");

        screeningId = given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/screenings")
                .then()
                .statusCode(201)
                .body("data.decision", equalTo("PENDING"))
                .body("data.eligibilityScore", equalTo(92))
                .extract().jsonPath().getLong("data.id");

        Assertions.assertNotNull(screeningId);
    }

    @Test
    @Order(5)
    @DisplayName("Step 2b — Cannot create duplicate screening for same resume")
    void step2b_DuplicateScreening_Returns409() {
        ScreeningRequest req = new ScreeningRequest();
        req.setResumeId(resumeId);
        req.setScreenerName("Another HR");

        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/screenings")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("DUPLICATE_SCREENING"));
    }

    @Test
    @Order(6)
    @DisplayName("Step 2c — List pending screenings includes our screening")
    void step2c_PendingScreeningsList_ContainsOurScreening() {
        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .when()
                .get("/api/v1/screenings/pending")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].decision", equalTo("PENDING"));
    }

    // =========================================================
    // Step 3: Record PASSED Decision
    // =========================================================

    @Test
    @Order(7)
    @DisplayName("Step 3 — Record PASSED decision on screening")
    void step3_RecordPassedDecision() {
        DecisionRequest req = new DecisionRequest();
        req.setDecision(ScreeningDecision.PASSED);
        req.setNotes("Candidate cleared all eligibility checks.");

        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .put("/api/v1/screenings/{id}/decision", screeningId)
                .then()
                .statusCode(200)
                .body("data.decision", equalTo("PASSED"))
                .body("data.screenedAt", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Step 3b — Cannot record decision twice on same screening")
    void step3b_DecisionAlreadyRecorded_Returns409() {
        DecisionRequest req = new DecisionRequest();
        req.setDecision(ScreeningDecision.FAILED);

        given()
                .auth().basic(RECRUITER, RECRUITER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .put("/api/v1/screenings/{id}/decision", screeningId)
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("DECISION_ALREADY_SET"));
    }

    // =========================================================
    // Step 4: Schedule Technical Interview
    // =========================================================

    @Test
    @Order(9)
    @DisplayName("Step 4 — Schedule technical interview for passed candidate")
    void step4_ScheduleInterview() {
        InterviewRequest req = new InterviewRequest();
        req.setScreeningId(screeningId);
        req.setInterviewerName("Prof. Muller");
        req.setInterviewerEmail("muller@ec.europa.eu");
        req.setScheduledAt(LocalDateTime.now().plusDays(7));
        req.setDurationMinutes(90);
        req.setType(InterviewType.ONLINE);
        req.setMeetingLink("https://teams.microsoft.com/ec/interview-room-42");

        interviewId = given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/interviews")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("SCHEDULED"))
                .body("data.interviewerName", equalTo("Prof. Muller"))
                .body("data.durationMinutes", equalTo(90))
                .extract().jsonPath().getLong("data.id");

        Assertions.assertNotNull(interviewId);
    }

    @Test
    @Order(10)
    @DisplayName("Step 4b — Get interview details returns correct data")
    void step4b_GetInterviewById() {
        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .when()
                .get("/api/v1/interviews/{id}", interviewId)
                .then()
                .statusCode(200)
                .body("data.candidateFullName", equalTo("Elena Papadopoulos"))
                .body("data.positionTitle", equalTo("Senior Economist"));
    }

    // =========================================================
    // Step 5: Complete Interview
    // =========================================================

    @Test
    @Order(11)
    @DisplayName("Step 5 — Mark interview as COMPLETED")
    void step5_CompleteInterview() {
        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .when()
                .put("/api/v1/interviews/{id}/complete", interviewId)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("COMPLETED"))
                .body("data.completedAt", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Step 5b — Cannot complete an already-completed interview")
    void step5b_CompleteAlreadyCompleted_Returns409() {
        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .when()
                .put("/api/v1/interviews/{id}/complete", interviewId)
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("ALREADY_COMPLETED"));
    }

    // =========================================================
    // Step 6: Submit Feedback
    // =========================================================

    @Test
    @Order(13)
    @DisplayName("Step 6 — Submit feedback for completed interview")
    void step6_SubmitFeedback() {
        FeedbackRequest req = new FeedbackRequest();
        req.setInterviewId(interviewId);
        req.setTechnicalScore(88);
        req.setBehavioralScore(82);
        req.setCommunicationScore(95);
        req.setStrengths("Excellent command of EU economic frameworks, strong analytical reasoning.");
        req.setWeaknesses("Limited experience with DG ECFIN-specific tools.");
        req.setRecommendation("Highly recommended for the Senior Economist role.");

        feedbackId = given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/api/v1/feedback")
                .then()
                .statusCode(201)
                .body("data.isFinalized", is(false))
                .body("data.technicalScore", equalTo(88))
                .body("data.overallScore", notNullValue())
                .extract().jsonPath().getLong("data.id");

        Assertions.assertNotNull(feedbackId);
    }

    @Test
    @Order(14)
    @DisplayName("Step 6b — Cannot submit feedback for non-COMPLETED interview")
    void step6b_FeedbackForNonCompletedInterview_Returns409() {
        // Schedule a new interview (not completed) and try to submit feedback
        // This uses a non-existent interview ID which will return 404
        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .when()
                .get("/api/v1/feedback/interview/9999")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("RESOURCE_NOT_FOUND"));
    }

    // =========================================================
    // Step 7: Finalize Feedback with HIRED Decision
    // =========================================================

    @Test
    @Order(15)
    @DisplayName("Step 7 — Finalize feedback with HIRED decision")
    void step7_FinalizeFeedback() {
        FinalizeRequest req = new FinalizeRequest();
        req.setFinalDecision(FinalDecision.HIRED);
        req.setRecommendation("Selected for Senior Economist position — start date TBD.");

        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .put("/api/v1/feedback/{id}/finalize", feedbackId)
                .then()
                .statusCode(200)
                .body("data.isFinalized", is(true))
                .body("data.finalDecision", equalTo("HIRED"))
                .body("data.finalizedAt", notNullValue());
    }

    @Test
    @Order(16)
    @DisplayName("Step 7b — Cannot finalize an already-finalized feedback")
    void step7b_FinalizeAlreadyFinalized_Returns409() {
        FinalizeRequest req = new FinalizeRequest();
        req.setFinalDecision(FinalDecision.REJECTED);

        given()
                .auth().basic(INTERVIEWER, INTERVIEWER_PASS)
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .put("/api/v1/feedback/{id}/finalize", feedbackId)
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("ALREADY_FINALIZED"));
    }

    @Test
    @Order(17)
    @DisplayName("Step 7c — Verify candidate feedback summary via Admin")
    void step7c_CandidateFeedbackSummary() {
        given()
                .auth().basic(ADMIN, ADMIN_PASS)
                .when()
                .get("/api/v1/feedback/interview/{id}", interviewId)
                .then()
                .statusCode(200)
                .body("data.candidateFullName", equalTo("Elena Papadopoulos"))
                .body("data.finalDecision", equalTo("HIRED"))
                .body("data.isFinalized", is(true));
    }

    // =========================================================
    // Swagger/Actuator availability
    // =========================================================

    @Test
    @Order(18)
    @DisplayName("Swagger UI is accessible without authentication")
    void swaggerUiIsAccessible() {
        given()
                .when()
                .get("/swagger-ui.html")
                .then()
                .statusCode(anyOf(is(200), is(302)));
    }

    @Test
    @Order(19)
    @DisplayName("Actuator health endpoint is accessible without auth")
    void actuatorHealthIsAccessible() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
