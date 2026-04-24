package eu.commission.ims.module.screening.repository;

import eu.commission.ims.module.screening.entity.Screening;
import eu.commission.ims.module.screening.entity.ScreeningDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    Optional<Screening> findByResumeId(Long resumeId);

    boolean existsByResumeId(Long resumeId);

    List<Screening> findAllByDecision(ScreeningDecision decision);

    @Query("SELECT s FROM Screening s JOIN FETCH s.resume r JOIN FETCH r.candidate WHERE s.id = :id")
    Optional<Screening> findByIdWithDetails(Long id);
}
