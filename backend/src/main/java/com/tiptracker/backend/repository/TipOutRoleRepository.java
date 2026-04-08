package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.TipOutRole;
import com.tiptracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipOutRoleRepository extends JpaRepository<TipOutRole, Long> {

    /** Returns all roles for a user, alphabetically — used for the role manager list. */
    List<TipOutRole> findByUserOrderByNameAsc(User user);

    /** Used to prevent duplicate role names per user on create/update. */
    boolean existsByUserAndName(User user, String name);
}
