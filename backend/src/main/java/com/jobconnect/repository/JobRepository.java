package com.jobconnect.repository;

import com.jobconnect.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j WHERE " +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:category IS NULL OR j.category = :category) AND j.active = true")
    List<Job> search(@Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("category") String category);

    List<Job> findByPostedBy(String postedBy);
    long countByPostedBy(String postedBy);
}
