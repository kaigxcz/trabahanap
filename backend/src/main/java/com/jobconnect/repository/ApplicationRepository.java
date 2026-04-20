package com.jobconnect.repository;

import com.jobconnect.model.Application;
import com.jobconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUser(User user);
    boolean existsByUserAndJobTitleAndCompany(User user, String jobTitle, String company);
    long countByUser(User user);
    List<Application> findByJobId(Long jobId);
    long countByJobId(Long jobId);
}
