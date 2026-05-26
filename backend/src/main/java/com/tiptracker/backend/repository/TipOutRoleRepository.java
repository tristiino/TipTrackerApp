package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.TipOutRole;
import com.tiptracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TipOutRoleRepository extends JpaRepository<TipOutRole, Long> {

    /** Returns all roles for a user, alphabetically — used for the role manager list. */
    List<TipOutRole> findByUserOrderByNameAsc(User user);

    /** Used to prevent duplicate role names per user on create/update. */
    boolean existsByUserAndName(User user, String name);

    /**
     * Bulk-deletes all tip-out roles for a user.
     * Safe to run after TipOutRecord rows are deleted (they held the FK reference).
     */
    @Modifying
    @Query("DELETE FROM TipOutRole r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
