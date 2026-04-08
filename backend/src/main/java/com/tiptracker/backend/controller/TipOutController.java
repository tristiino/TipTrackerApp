package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.TipOutRecordDTO;
import com.tiptracker.backend.dto.TipOutRoleDTO;
import com.tiptracker.backend.service.TipOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for tip-out role templates and per-shift record overrides.
 *
 * Identity is always resolved from the JWT Principal — never from a request body
 * or path variable — to prevent IDOR attacks.
 *
 * Validation errors are handled globally by GlobalExceptionHandler,
 * which converts IllegalArgumentException → 400 and SecurityException → 403.
 */
@RestController
@RequestMapping("/api/tip-out-roles")
@RequiredArgsConstructor
public class TipOutController {

    private final TipOutService tipOutService;

    // -------------------------------------------------------------------------
    // Role CRUD  (P2-001)
    // -------------------------------------------------------------------------

    /**
     * GET /api/tip-out-roles
     * Returns all roles for the authenticated user, sorted A→Z.
     */
    @GetMapping
    public ResponseEntity<List<TipOutRoleDTO>> getRoles(Principal principal) {
        return ResponseEntity.ok(tipOutService.getRolesForUser(principal.getName()));
    }

    /**
     * POST /api/tip-out-roles
     * Creates a new tip-out role for the authenticated user.
     * Body: { "name": "Busser", "splitType": "PERCENTAGE", "amount": 5.0 }
     * Returns: 201 Created with the saved role (including its new id).
     */
    @PostMapping
    public ResponseEntity<TipOutRoleDTO> createRole(
            @RequestBody TipOutRoleDTO dto,
            Principal principal) {
        TipOutRoleDTO created = tipOutService.createRole(principal.getName(), dto);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * PUT /api/tip-out-roles/{id}
     * Updates an existing role. Fails with 403 if the role belongs to another user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TipOutRoleDTO> updateRole(
            @PathVariable Long id,
            @RequestBody TipOutRoleDTO dto,
            Principal principal) {
        return ResponseEntity.ok(tipOutService.updateRole(principal.getName(), id, dto));
    }

    /**
     * DELETE /api/tip-out-roles/{id}
     * Deletes a role. Fails with 403 if the role belongs to another user.
     * Historical TipOutRecord rows keep a name snapshot so history is preserved.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id,
            Principal principal) {
        tipOutService.deleteRole(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Override a per-shift record  (P2-004)
    // -------------------------------------------------------------------------

    /**
     * PATCH /api/tip-out-roles/records/{recordId}/override
     * Manually overrides the finalAmount on a specific TipOutRecord.
     *
     * Body: { "finalAmount": 12.50 }
     * Returns: the updated TipOutRecordDTO with isOverridden=true.
     *
     * Why PATCH? We're partially updating one field on an existing resource.
     * PUT would imply replacing the entire record.
     */
    @PatchMapping("/records/{recordId}/override")
    public ResponseEntity<TipOutRecordDTO> overrideRecord(
            @PathVariable Long recordId,
            @RequestBody Map<String, Double> body,
            Principal principal) {

        Double newAmount = body.get("finalAmount");
        if (newAmount == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                tipOutService.overrideRecord(principal.getName(), recordId, newAmount)
        );
    }
}
