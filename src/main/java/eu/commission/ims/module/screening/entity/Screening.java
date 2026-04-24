package eu.commission.ims.module.screening.entity;

import eu.commission.ims.common.audit.AuditableEntity;
import eu.commission.ims.module.resume.entity.Resume;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents the HR eligibility screening performed on a submitted resume.
 * One screening per resume.
 */
@Entity
@Table(name = "screenings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Screening extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Column(name = "screener_name", nullable = false, length = 200)
    private String screenerName;

    @Column(name = "eligibility_score")
    private Integer eligibilityScore;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    @Builder.Default
    private ScreeningDecision decision = ScreeningDecision.PENDING;

    @Column(name = "screened_at")
    private LocalDateTime screenedAt;
}
