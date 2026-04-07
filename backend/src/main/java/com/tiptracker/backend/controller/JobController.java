package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.JobDTO;
import com.tiptracker.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST endpoints for job profile management.
 *
 * GET    /api/jobs          — list all jobs for the authenticated user
 * POST   /api/jobs          — create a new job (max 10 per user)
 * PUT    /api/jobs/{id}     — update an existing job
 * DELETE /api/jobs/{id}     — delete a job
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<JobDTO>> getJobs(Principal principal) {
        return ResponseEntity.ok(jobService.getJobsForUser(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<JobDTO> createJob(@RequestBody JobDTO dto, Principal principal) {
        return ResponseEntity.status(201).body(jobService.createJob(principal.getName(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDTO> updateJob(
            @PathVariable Long id,
            @RequestBody JobDTO dto,
            Principal principal) {
        return ResponseEntity.ok(jobService.updateJob(principal.getName(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, Principal principal) {
        jobService.deleteJob(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
