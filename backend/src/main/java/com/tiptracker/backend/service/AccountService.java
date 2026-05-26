package com.tiptracker.backend.service;

import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles permanent account deletion (FR-041 / Apple App Store Guideline 5.1.1).
 *
 * Deletion order matters — foreign key constraints must be respected:
 *
 *   password_reset_tokens  → FK → users   (not cascaded from User)
 *   user_settings          → FK → users   (not cascaded from User)
 *   tip_out_records        → FK → tip_entry  (cascaded via TipEntry.tipOutRecords)
 *   tip_entry_tags (jct)   → FK → tip_entry  (cascaded via TipEntry ManyToMany)
 *   tip_entry              → FK → users, job (cascaded via User.tipEntries)
 *   tip_out_role           → FK → users, job (cascaded via User.tipOutRoles)
 *   job                    → FK → users   (not cascaded; safe AFTER tip_entry/tip_out_role are gone)
 *   shift_tag              → user_id plain @Column — no FK, cleaned for data hygiene
 *   device_tokens          → FK → users   (DB-level ON DELETE CASCADE from FR-045 handles automatically)
 *   users                  ← deleted last
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final TipOutRecordRepository tipOutRecordRepository;
    private final TipEntryRepository tipEntryRepository;
    private final TipOutRoleRepository tipOutRoleRepository;
    private final JobRepository jobRepository;
    private final TagRepository tagRepository;

    /**
     * Permanently deletes the authenticated user's account and all their data.
     * Runs in a single transaction — if anything fails, the whole operation rolls back.
     *
     * Bulk JPQL/native deletes are used throughout (not JPA cascade) so that the
     * deletion order is explicit and predictable. FK constraints require this order:
     *
     *   tip_out_record → tip_entry (step 3)
     *   tip_entry_tags + tip_entry → users + job (step 4+5, via JPA deleteAll lifecycle)
     *   tip_out_role   → users + job (step 6; tip_out_records FK is already cleared)
     *   job            → users (step 7; tip_entry + tip_out_role FKs are cleared)
     *   users          → deleted last (step 9; device_tokens cleaned by DB cascade)
     *
     * @param email The authenticated user's email (from JWT principal).
     */
    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        Long userId = user.getId();
        log.info("Deleting account for user id={}", userId);

        // Step 1: password_reset_tokens — FK → users, not cascaded.
        passwordResetTokenRepository.deleteByUser(user);

        // Step 2: user_settings — FK → users, not cascaded.
        userSettingsRepository.deleteByUser(user);

        // Step 3: tip_out_records — FK → tip_entry. Must go before tip_entry rows.
        tipOutRecordRepository.deleteByTipEntryUserId(userId);

        // Steps 4+5: tip_entry_tags junction rows + tip_entry rows.
        // deleteAll() uses JPA lifecycle: Hibernate removes each entry's tag join-table
        // rows first, then the tip_entry row itself — no native SQL needed.
        // Safe here because TipOutRecord rows (FK → tip_entry) were deleted in step 3.
        tipEntryRepository.deleteAll(user.getTipEntries());

        // Step 6: tip_out_role — FK → users + nullable FK → job.
        // tip_out_records that referenced these roles were deleted in step 3.
        tipOutRoleRepository.deleteByUserId(userId);

        // Step 7: job — FK → users.
        // tip_entry and tip_out_role rows that held job_id FKs are gone.
        jobRepository.deleteByUser(user);

        // Step 8: shift_tag — user_id is a plain @Column (no FK constraint in DB).
        // Rows are not blocking, but clean them up for data hygiene.
        tagRepository.deleteByUserId(userId);

        // Step 9: Delete user.
        // device_tokens removed automatically by DB-level ON DELETE CASCADE (FR-045).
        userRepository.deleteById(userId);

        log.info("Account deleted for user id={}", userId);
    }
}
