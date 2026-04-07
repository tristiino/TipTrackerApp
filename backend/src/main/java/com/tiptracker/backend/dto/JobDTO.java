package com.tiptracker.backend.dto;

import lombok.Data;

/**
 * Request body for POST/PUT /api/jobs, and response shape for all job endpoints.
 */
@Data
public class JobDTO {
    private Long id;
    private String name;
    private String location;
    private Double hourlyWage;
}
