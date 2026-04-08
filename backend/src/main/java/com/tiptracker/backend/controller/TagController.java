package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.TagDTO;
import com.tiptracker.backend.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for user-scoped tags.
 *
 * GET  /api/tags       — list all tags for the authenticated user
 * POST /api/tags       — create a new tag (idempotent: returns existing if name matches)
 *
 * P2-014: Shift Notes, Tags & Search
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagDTO>> getTags(Principal principal) {
        return ResponseEntity.ok(tagService.getTagsForUser(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<TagDTO> createTag(
            @RequestBody Map<String, String> body,
            Principal principal) {
        String name = body.get("name");
        TagDTO tag = tagService.createTag(name, principal.getName());
        return ResponseEntity.ok(tag);
    }
}
