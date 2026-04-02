package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UserSettings.
 * Each user has at most one settings row, created on first access.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUser(User user);
}
