package eu.commission.ims.module.feedback.repository;

import eu.commission.ims.module.feedback.entity.Feedback;
import eu.commission.ims.module.feedback.entity.FinalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByInterviewId(Long interviewId);

    boolean existsByInterviewId(Long interviewId);

    List<Feedback> findAllByFinalDecision(FinalDecision decision);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.interview i JOIN FETCH i.screening s JOIN FETCH s.resume r JOIN FETCH r.candidate WHERE f.id = :id")
    Optional<Feedback> findByIdWithDetails(Long id);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.interview i JOIN FETCH i.screening s JOIN FETCH s.resume r JOIN FETCH r.candidate WHERE r.candidate.id = :candidateId")
    List<Feedback> findByCandidateId(Long candidateId);

    List<Feedback> findAllByIsFinalized(Boolean isFinalized);
}
