package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByUserUsernameOrderByNameAsc(String username);

    Optional<Job> findByIdAndUserUsername(Long id, String username);

    int countByUserUsername(String username);
}
