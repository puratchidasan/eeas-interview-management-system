package eu.commission.ims.module.feedback.entity;

import eu.commission.ims.common.audit.AuditableEntity;
import eu.commission.ims.module.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents interviewer feedback submitted after a completed technical interview.
 * One feedback record per interview (finalized after completion).
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private Interview interview;

    @Column(name = "technical_score", nullable = false)
    private Integer technicalScore;

    @Column(name = "behavioral_score", nullable = false)
    private Integer behavioralScore;

    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_decision", length = 20)
    private FinalDecision finalDecision;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    /** Calculates the weighted overall score */
    @PrePersist
    @PreUpdate
    public void calculateOverallScore() {
        this.overallScore = (technicalScore * 0.5) + (behavioralScore * 0.3) + (communicationScore * 0.2);
    }
}
