package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.JobDTO;
import com.tiptracker.backend.model.Job;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.JobRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final int MAX_JOBS = 10;

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public List<JobDTO> getJobsForUser(String email) {
        User user = resolveUser(email);
        return jobRepository.findByUserUsernameOrderByNameAsc(user.getUsername())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public JobDTO createJob(String email, JobDTO dto) {
        User user = resolveUser(email);
        if (jobRepository.countByUserUsername(user.getUsername()) >= MAX_JOBS) {
            throw new IllegalArgumentException("Maximum of " + MAX_JOBS + " jobs allowed.");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Job name is required.");
        }

        Job job = new Job();
        job.setUser(user);
        populateJobFromDTO(job, dto);

        return toDTO(jobRepository.save(job));
    }

    public JobDTO updateJob(String email, Long id, JobDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Job name is required.");
        }

        User user = resolveUser(email);
        Job job = jobRepository.findByIdAndUserUsername(id, user.getUsername())
                .orElseThrow(() -> new SecurityException("Job not found or access denied."));

        populateJobFromDTO(job, dto);
        return toDTO(jobRepository.save(job));
    }

    public void deleteJob(String email, Long id) {
        User user = resolveUser(email);
        Job job = jobRepository.findByIdAndUserUsername(id, user.getUsername())
                .orElseThrow(() -> new SecurityException("Job not found or access denied."));
        jobRepository.delete(job);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private void populateJobFromDTO(Job job, JobDTO dto) {
        job.setName(dto.getName().trim());
        job.setLocation(dto.getLocation());
        job.setHourlyWage(dto.getHourlyWage());
    }

    public JobDTO toDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setName(job.getName());
        dto.setLocation(job.getLocation());
        dto.setHourlyWage(job.getHourlyWage());
        return dto;
    }
}
