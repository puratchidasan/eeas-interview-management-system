package eu.commission.ims.module.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for creating a new candidate + resume submission.
 */
@Data
public class ResumeRequest {

    // ---- Candidate fields ----
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Nationality (2-letter ISO code) is required")
    @Size(min = 2, max = 2, message = "Nationality must be a 2-letter ISO country code")
    private String nationality;

    @Size(max = 30)
    private String phoneNumber;

    // ---- Resume fields ----
    @NotBlank(message = "Position title is required")
    @Size(max = 200)
    private String positionTitle;

    @NotBlank(message = "Department is required")
    @Size(max = 200)
    private String department;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience value seems unrealistic")
    private Integer yearsOfExperience;

    @Size(max = 500)
    private String linkedInProfile;

    @Size(max = 5000, message = "Cover letter cannot exceed 5000 characters")
    private String coverLetter;
}
