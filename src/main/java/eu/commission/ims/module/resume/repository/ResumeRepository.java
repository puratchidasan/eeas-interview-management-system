package eu.commission.ims.module.resume.repository;

import eu.commission.ims.module.resume.entity.Resume;
import eu.commission.ims.module.resume.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Page<Resume> findAllByStatus(SubmissionStatus status, Pageable pageable);

    List<Resume> findByCandidateId(Long candidateId);

    @Query("SELECT r FROM Resume r JOIN FETCH r.candidate WHERE r.id = :id")
    java.util.Optional<Resume> findByIdWithCandidate(Long id);

    boolean existsByCandidateIdAndStatus(Long candidateId, SubmissionStatus status);
}
