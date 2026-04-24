package eu.commission.ims.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.commission.ims.module.resume.dto.ResumeRequest;
import eu.commission.ims.module.resume.dto.StatusUpdateRequest;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ResumeController.
 * Loads full Spring Boot context with H2 in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Resume Controller Integration Tests")
class ResumeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResumeRequest buildValidRequest(String email) {
        ResumeRequest req = new ResumeRequest();
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail(email);
        req.setNationality("BE");
        req.setPhoneNumber("+3200000000");
        req.setPositionTitle("Test Engineer");
        req.setDepartment("QA");
        req.setYearsOfExperience(3);
        req.setCoverLetter("Test cover letter");
        return req;
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    @DisplayName("POST /api/v1/resumes — Should create resume and return 201")
    void submitResume_ValidRequest_Returns201() throws Exception {
        ResumeRequest req = buildValidRequest("integration-test@ec.europa.eu");

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateEmail").value("integration-test@ec.europa.eu"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    @DisplayName("POST /api/v1/resumes — Should return 400 for missing required fields")
    void submitResume_MissingFields_Returns400() throws Exception {
        ResumeRequest req = new ResumeRequest(); // empty

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    @DisplayName("POST /api/v1/resumes — Should return 409 on duplicate submission")
    void submitResume_DuplicateSubmission_Returns409() throws Exception {
        ResumeRequest req = buildValidRequest("duplicate@ec.europa.eu");

        // First submission
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Duplicate
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_SUBMISSION"));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    @DisplayName("GET /api/v1/resumes/{id} — Should return 404 for unknown ID")
    void getResume_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    @DisplayName("GET /api/v1/resumes — Should return paginated list")
    void getAllResumes_Returns200WithPage() throws Exception {
        mockMvc.perform(get("/api/v1/resumes").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("POST /api/v1/resumes — Should return 401 when unauthenticated")
    void submitResume_Unauthenticated_Returns401() throws Exception {
        ResumeRequest req = buildValidRequest("unauth@ec.europa.eu");

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "interviewer", roles = "INTERVIEWER")
    @DisplayName("POST /api/v1/resumes — Should return 403 for INTERVIEWER role")
    void submitResume_WrongRole_Returns403() throws Exception {
        ResumeRequest req = buildValidRequest("wrongrole@ec.europa.eu");

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
