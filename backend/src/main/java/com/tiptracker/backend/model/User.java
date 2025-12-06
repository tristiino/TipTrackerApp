package com.tiptracker.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a User entity in the database.
 * This class also implements Spring Security's UserDetails interface to integrate
 * with the authentication and authorization framework.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // Explicitly naming the table is good practice
public class User implements UserDetails {

    /**
     * The unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user's chosen username.
     */
    private String username;

    /**
     * The user's hashed password for authentication.
     */
    private String password;

    /**
     * The user's email address, used for login. Must be unique.
     */
    @Column(unique = true)
    private String email;

    /**
     * The role assigned to the user (e.g., USER, ADMIN), which determines their permissions.
     */
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * The list of tip entries associated with this user.
     * This represents the "one" side of a one-to-many relationship with TipEntry.
     * JsonManagedReference is used to prevent infinite loops during JSON serialization.
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipEntry> tipEntries = new ArrayList<>();


    // --- UserDetails Interface Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}