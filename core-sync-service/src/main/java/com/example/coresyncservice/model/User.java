package com.example.coresyncservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Core user entity.
 *
 * <p>The table is named {@code app_user} to avoid clashing with the
 * reserved {@code user} keyword in PostgreSQL and other databases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String name;

    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ── Brute-force protection ────────────────────────────────

    /** Incremented on every failed login; reset to 0 on success. */
    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** Set to true by the system when too many failed attempts are detected. */
    @Builder.Default
    @Column(nullable = false)
    private boolean accountLocked = false;

    // ── UserDetails implementation ────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return !accountLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
