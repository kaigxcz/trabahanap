package com.jobconnect.repository;

import com.jobconnect.model.Interview;
import com.jobconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByCandidate(User candidate);
    List<Interview> findByApplication_JobId(Long jobId);
    Optional<Interview> findByApplicationId(Long applicationId);
}
