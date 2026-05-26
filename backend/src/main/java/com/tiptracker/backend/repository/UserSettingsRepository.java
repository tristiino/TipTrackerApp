package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for UserSettings.
 * Each user has at most one settings row, created on first access.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT s FROM UserSettings s " +
           "WHERE s.notificationsEnabled = true " +
           "AND s.reminderDayOfWeek = :dow " +
           "AND s.reminderTime >= :windowStart " +
           "AND s.reminderTime < :windowEnd")
    List<UserSettings> findEligibleForNotification(
            @Param("dow") int dayOfWeek,
            @Param("windowStart") LocalTime windowStart,
            @Param("windowEnd") LocalTime windowEnd
    );
}
