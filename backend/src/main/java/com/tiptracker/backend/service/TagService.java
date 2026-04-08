package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.TagDTO;
import com.tiptracker.backend.model.Tag;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.TagRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for user-defined shift tags.
 * P2-014: Shift Notes, Tags & Search
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    /** Returns all tags belonging to the authenticated user. */
    public List<TagDTO> getTagsForUser(String userEmail) {
        Long userId = resolveUserId(userEmail);
        return tagRepository.findByUserId(userId)
                .stream()
                .map(t -> new TagDTO(t.getId(), t.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Creates a new tag with the given name for the user.
     * If a tag with that name already exists (case-insensitive), returns the existing one.
     */
    @Transactional
    public TagDTO createTag(String name, String userEmail) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be blank.");
        }
        String trimmed = name.strip();
        Long userId = resolveUserId(userEmail);

        Tag tag = tagRepository.findByNameIgnoreCaseAndUserId(trimmed, userId)
                .orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(trimmed);
                    t.setUserId(userId);
                    return tagRepository.save(t);
                });

        return new TagDTO(tag.getId(), tag.getName());
    }

    /**
     * Resolves a list of tag IDs into Tag entities, scoped to the given user.
     * Tags that don't belong to the user are silently ignored (ownership check).
     */
    public List<Tag> resolveTagsForUser(List<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        return tagRepository.findAllById(tagIds)
                .stream()
                .filter(t -> t.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    private Long resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return user.getId();
    }
}
