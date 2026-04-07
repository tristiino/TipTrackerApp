package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.TipOutRecordDTO;
import com.tiptracker.backend.dto.TipOutRoleDTO;
import com.tiptracker.backend.model.*;
import com.tiptracker.backend.repository.JobRepository;
import com.tiptracker.backend.repository.TipOutRecordRepository;
import com.tiptracker.backend.repository.TipOutRoleRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Owns all business logic for tip-out roles and per-shift tip-out records.
 *
 * Two core concepts:
 *
 * 1. TipOutRole  — reusable templates a user defines ("Busser = 5%")
 *                  CRUD lives here (P2-001)
 *
 * 2. TipOutRecord — the actual deduction applied to a specific shift
 *                   Created when roles are applied to a TipEntry (P2-002, P2-003)
 *                   Override logic lives here (P2-004)
 *
 * Validation (P2-005) is enforced before any records are written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TipOutService {

    private final TipOutRoleRepository tipOutRoleRepository;
    private final TipOutRecordRepository tipOutRecordRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    // -------------------------------------------------------------------------
    // Role CRUD (P2-001)
    // -------------------------------------------------------------------------

    /**
     * Returns all tip-out roles for the authenticated user, sorted A→Z by name.
     */
    @Transactional(readOnly = true)
    public List<TipOutRoleDTO> getRolesForUser(String userEmail) {
        User user = resolveUser(userEmail);
        return tipOutRoleRepository.findByUserOrderByNameAsc(user)
                .stream()
                .map(this::toRoleDTO)
                .toList();
    }

    /**
     * Creates a new tip-out role for the authenticated user.
     *
     * Validations:
     * - name must not be blank
     * - amount must be > 0
     * - for PERCENTAGE roles, amount must be ≤ 100
     * - name must be unique per user
     * - saving this role must not push the user's total PERCENTAGE sum over 100%
     */
    @Transactional
    public TipOutRoleDTO createRole(String userEmail, TipOutRoleDTO dto) {
        User user = resolveUser(userEmail);
        validateRoleFields(dto);
        validateNoDuplicateName(user, dto.getName(), null);
        validatePercentageSumAtSave(user, dto, null);

        TipOutRole role = new TipOutRole();
        role.setUser(user);
        role.setName(dto.getName().trim());
        role.setSplitType(dto.getSplitType());
        role.setAmount(dto.getAmount());
        role.setSource(dto.getSource() != null ? dto.getSource() : TipOutSource.BOTH);
        if (dto.getJobId() != null) {
            role.setJob(jobRepository.findByIdAndUserUsername(dto.getJobId(), user.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + dto.getJobId())));
        }

        return toRoleDTO(tipOutRoleRepository.save(role));
    }

    /**
     * Updates an existing role. Enforces the same validations as create.
     * Also verifies the role belongs to the requesting user (IDOR guard).
     */
    @Transactional
    public TipOutRoleDTO updateRole(String userEmail, Long roleId, TipOutRoleDTO dto) {
        TipOutRole role = resolveRoleForUser(userEmail, roleId);
        validateRoleFields(dto);
        validateNoDuplicateName(role.getUser(), dto.getName(), roleId);
        validatePercentageSumAtSave(role.getUser(), dto, roleId);

        role.setName(dto.getName().trim());
        role.setSplitType(dto.getSplitType());
        role.setAmount(dto.getAmount());
        role.setSource(dto.getSource() != null ? dto.getSource() : TipOutSource.BOTH);
        if (dto.getJobId() != null) {
            role.setJob(jobRepository.findByIdAndUserUsername(dto.getJobId(), role.getUser().getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + dto.getJobId())));
        } else {
            role.setJob(null);
        }

        return toRoleDTO(tipOutRoleRepository.save(role));
    }

    /**
     * Deletes a role. Verifies ownership first.
     * Note: existing TipOutRecords referencing this role have role set to nullable,
     * so deletion will null out the FK but preserve the historical record.
     */
    @Transactional
    public void deleteRole(String userEmail, Long roleId) {
        TipOutRole role = resolveRoleForUser(userEmail, roleId);
        tipOutRoleRepository.delete(role);
        log.info("Deleted tip-out role {} for user {}", roleId, userEmail);
    }

    // -------------------------------------------------------------------------
    // Apply roles to a shift (P2-002, P2-003)
    // -------------------------------------------------------------------------

    /**
     * Resolves the given role IDs, validates the combined split against the
     * shift's gross tips, then creates and saves a TipOutRecord for each role.
     *
     * The TipEntry must already be persisted before calling this method
     * so that TipOutRecord can reference it by FK.
     *
     * @param tipEntry  the already-saved shift
     * @param roleIds   the IDs of roles the user selected on the form
     * @return list of DTOs representing the created records
     */
    @Transactional
    public List<TipOutRecordDTO> applyRolesToEntry(TipEntry tipEntry, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        List<TipOutRole> roles = tipOutRoleRepository.findAllById(roleIds);

        // P2-005: validate before writing anything
        validateRoleSplit(roles, tipEntry);

        List<TipOutRecord> records = roles.stream().map(role -> {
            double computed = computeAmount(role, tipEntry);

            TipOutRecord record = new TipOutRecord();
            record.setTipEntry(tipEntry);
            record.setRole(role);
            record.setRoleName(role.getName());     // snapshot — survives role rename/delete
            record.setComputedAmount(computed);
            record.setFinalAmount(computed);         // starts equal; diverges only if overridden
            record.setOverridden(false);
            return record;
        }).toList();

        return tipOutRecordRepository.saveAll(records)
                .stream()
                .map(this::toRecordDTO)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Override a record (P2-004)
    // -------------------------------------------------------------------------

    /**
     * Manually overrides the finalAmount on a TipOutRecord.
     *
     * Sets isOverridden = true so the UI can flag it visually.
     * The computedAmount is never changed — it remains the formula's original result.
     *
     * @param userEmail   the requesting user (IDOR guard)
     * @param recordId    the TipOutRecord to override
     * @param newAmount   the manually entered dollar amount
     */
    @Transactional
    public TipOutRecordDTO overrideRecord(String userEmail, Long recordId, double newAmount) {
        TipOutRecord record = tipOutRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Tip-out record not found: " + recordId));

        // IDOR guard: the record's shift must belong to the requesting user
        String ownerEmail = record.getTipEntry().getUser().getEmail();
        if (!ownerEmail.equals(userEmail)) {
            throw new SecurityException("You do not have permission to modify this record.");
        }

        if (newAmount < 0) {
            throw new IllegalArgumentException("Override amount cannot be negative.");
        }

        record.setFinalAmount(newAmount);
        record.setOverridden(true);

        return toRecordDTO(tipOutRecordRepository.save(record));
    }

    // -------------------------------------------------------------------------
    // Shared calculation & validation (P2-005)
    // -------------------------------------------------------------------------

    /**
     * Computes the dollar deduction for a single role against a tip entry.
     * Respects the role's source: CASH, CREDIT, or BOTH (total).
     */
    public double computeAmount(TipOutRole role, TipEntry entry) {
        double base;
        TipOutSource src = role.getSource() != null ? role.getSource() : TipOutSource.BOTH;
        base = switch (src) {
            case CASH   -> entry.getCashTips()   != null ? entry.getCashTips()   : 0.0;
            case CREDIT -> entry.getCreditTips() != null ? entry.getCreditTips() : 0.0;
            case BOTH   -> entry.getAmount();
        };
        return switch (role.getSplitType()) {
            case PERCENTAGE   -> base * (role.getAmount() / 100.0);
            case FIXED_AMOUNT -> role.getAmount();
        };
    }

    /**
     * Returns the sum of all finalAmounts on a shift's tip-out records.
     * Used by TipEntryService to compute netTips per shift.
     */
    public double sumTipOuts(TipEntry tipEntry) {
        return tipOutRecordRepository.findByTipEntry(tipEntry)
                .stream()
                .mapToDouble(TipOutRecord::getFinalAmount)
                .sum();
    }

    /**
     * Maps all TipOutRecord rows for a shift to DTOs.
     */
    public List<TipOutRecordDTO> getRecordsForEntry(TipEntry tipEntry) {
        return tipOutRecordRepository.findByTipEntry(tipEntry)
                .stream()
                .map(this::toRecordDTO)
                .toList();
    }

    /**
     * P2-005 validation: enforces two rules before records are written.
     *
     * Rule 1 — Percentage sum must not exceed 100%.
     *   Checked independently of fixed amounts because it's logically invalid
     *   regardless of the shift's gross tips.
     *
     * Rule 2 — Total deductions (resolved to dollars) must not exceed gross tips.
     *   Prevents a negative net take-home, which would be nonsensical.
     */
    void validateRoleSplit(List<TipOutRole> roles, TipEntry entry) {
        double grossTips = entry.getAmount();

        // Rule 1: BOTH-source percentage roles must not sum over 100%
        double percentageSum = roles.stream()
                .filter(r -> r.getSplitType() == TipOutType.PERCENTAGE)
                .filter(r -> r.getSource() == null || r.getSource() == TipOutSource.BOTH)
                .mapToDouble(TipOutRole::getAmount)
                .sum();

        if (percentageSum > 100.0) {
            throw new IllegalArgumentException(
                    "Percentage-based tip-out roles (from total) sum to " + percentageSum +
                    "%, which exceeds 100%."
            );
        }

        // Rule 2: total dollar deductions must not exceed gross tips
        double totalDeduction = roles.stream()
                .mapToDouble(role -> computeAmount(role, entry))
                .sum();

        if (totalDeduction > grossTips) {
            throw new IllegalArgumentException(
                    String.format(
                        "Total tip-out deductions ($%.2f) exceed gross tips ($%.2f).",
                        totalDeduction, grossTips
                    )
            );
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private TipOutRole resolveRoleForUser(String userEmail, Long roleId) {
        TipOutRole role = tipOutRoleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Tip-out role not found: " + roleId));
        if (!role.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("You do not have permission to modify this role.");
        }
        return role;
    }

    private void validateRoleFields(TipOutRoleDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank.");
        }
        if (dto.getSplitType() == null) {
            throw new IllegalArgumentException("Split type must be PERCENTAGE or FIXED_AMOUNT.");
        }
        if (dto.getAmount() <= 0) {
            throw new IllegalArgumentException("Role amount must be greater than 0.");
        }
        if (dto.getSplitType() == TipOutType.PERCENTAGE && dto.getAmount() > 100) {
            throw new IllegalArgumentException("Percentage amount cannot exceed 100%.");
        }
    }

    /**
     * Prevents two roles with the same name for the same user.
     * excludeRoleId lets us skip the role being updated (it's its own name).
     */
    private void validateNoDuplicateName(User user, String name, Long excludeRoleId) {
        boolean exists = tipOutRoleRepository.findByUserOrderByNameAsc(user).stream()
                .filter(r -> excludeRoleId == null || !r.getId().equals(excludeRoleId))
                .anyMatch(r -> r.getName().equalsIgnoreCase(name.trim()));
        if (exists) {
            throw new IllegalArgumentException("A role named '" + name.trim() + "' already exists.");
        }
    }

    /**
     * Soft check at template save time: warns if the user's combined percentage roles
     * would exceed 100%. This is checked against the template set, not a live shift,
     * so we can't enforce Rule 2 here (no gross tips value available).
     */
    private void validatePercentageSumAtSave(User user, TipOutRoleDTO newDto, Long excludeRoleId) {
        if (newDto.getSplitType() != TipOutType.PERCENTAGE) {
            return; // fixed-amount roles don't affect the percentage total
        }
        // Scope the check to the same job context (global vs. specific job)
        double existingSum = tipOutRoleRepository.findByUserOrderByNameAsc(user).stream()
                .filter(r -> excludeRoleId == null || !r.getId().equals(excludeRoleId))
                .filter(r -> r.getSplitType() == TipOutType.PERCENTAGE)
                .filter(r -> Objects.equals(r.getJob() != null ? r.getJob().getId() : null, newDto.getJobId()))
                .mapToDouble(TipOutRole::getAmount)
                .sum();

        if (existingSum + newDto.getAmount() > 100.0) {
            throw new IllegalArgumentException(
                    String.format(
                        "Adding this role would make your total percentage tip-out %.1f%%, exceeding 100%%.",
                        existingSum + newDto.getAmount()
                    )
            );
        }
    }

    private TipOutRoleDTO toRoleDTO(TipOutRole role) {
        TipOutSource src = role.getSource() != null ? role.getSource() : TipOutSource.BOTH;
        Long jobId     = role.getJob() != null ? role.getJob().getId()   : null;
        String jobName = role.getJob() != null ? role.getJob().getName() : null;
        return new TipOutRoleDTO(role.getId(), role.getName(), role.getSplitType(), role.getAmount(), src, jobId, jobName);
    }

    private TipOutRecordDTO toRecordDTO(TipOutRecord record) {
        return new TipOutRecordDTO(
                record.getId(),
                record.getRole() != null ? record.getRole().getId() : null,
                record.getRoleName(),
                record.getComputedAmount(),
                record.getFinalAmount(),
                record.isOverridden()
        );
    }
}
