package com.tiptracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A job profile owned by a user. Users can have up to 10 jobs.
 * Each job has a name and optional location/hourly wage.
 * Tip entries can be linked to a job via a nullable FK.
 */
@Data
@NoArgsConstructor
@Entity
@Table(
    name = "job",
    indexes = @Index(name = "idx_job_user", columnList = "user_id")
)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String location;

    @Column(nullable = true)
    private Double hourlyWage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
