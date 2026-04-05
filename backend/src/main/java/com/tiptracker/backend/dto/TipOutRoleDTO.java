package com.tiptracker.backend.dto;

import com.tiptracker.backend.model.TipOutSource;
import com.tiptracker.backend.model.TipOutType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for TipOutRole.
 * Used for both request bodies (create/update) and response bodies (list).
 *
 * The user.id is never included — identity always comes from the JWT.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipOutRoleDTO {

    private Long id;          // null on create requests, populated on responses
    private String name;
    private TipOutType splitType;
    private double amount;

    /** Which tip pool this role pulls from: CASH, CREDIT, or BOTH (default). */
    private TipOutSource source;
}
