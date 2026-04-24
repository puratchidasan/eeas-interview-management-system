package eu.commission.ims.module.interview.repository;

import eu.commission.ims.module.interview.entity.Interview;
import eu.commission.ims.module.interview.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("SELECT i FROM Interview i JOIN FETCH i.screening s JOIN FETCH s.resume r JOIN FETCH r.candidate WHERE i.id = :id")
    Optional<Interview> findByIdWithDetails(Long id);

    List<Interview> findAllByStatus(InterviewStatus status);

    @Query("SELECT i FROM Interview i WHERE i.scheduledAt >= :from AND i.scheduledAt <= :to")
    List<Interview> findUpcoming(LocalDateTime from, LocalDateTime to);

    boolean existsByScreeningId(Long screeningId);

    List<Interview> findByScreeningId(Long screeningId);
}
