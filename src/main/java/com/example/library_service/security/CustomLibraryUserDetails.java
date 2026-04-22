package com.example.library_service.security;

import com.example.library_service.entity.LibraryUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
// Custom implementation of UserDetails to integrate LibraryUser with Spring Security
public class CustomLibraryUserDetails implements UserDetails {

    private final Long id;
    private final String studentId;
    private final String password;
    private final String role;
    private final boolean active;

    // Maps LibraryUser entity to Spring Security user details
    public CustomLibraryUserDetails(LibraryUser user) {
        this.id = user.getId();
        this.studentId = user.getStudentId();
        this.password = user.getPinHash();
        this.role = user.getRole().name();
        this.active = user.isActive();
    }

    @Override
    // Converts user role into Spring Security authority format (e.g., ROLE_ADMIN)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    // Uses studentId as the username for authentication
    public String getUsername() {
        return studentId;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}