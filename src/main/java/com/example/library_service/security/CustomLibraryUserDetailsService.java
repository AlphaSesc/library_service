package com.example.library_service.security;

import com.example.library_service.entity.LibraryUser;
import com.example.library_service.exception.InvalidCredentialsException;
import com.example.library_service.repository.LibraryUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
// Service used by Spring Security to load user details during authentication
public class CustomLibraryUserDetailsService implements UserDetailsService {

    private final LibraryUserRepository libraryUserRepository;

    public CustomLibraryUserDetailsService(LibraryUserRepository libraryUserRepository) {
        this.libraryUserRepository = libraryUserRepository;
    }

    @Override
    // Fetches user by studentId and throws exception if not found
    public UserDetails loadUserByUsername(String studentId) {
        LibraryUser user = libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid student ID or PIN"));

        return new CustomLibraryUserDetails(user);
    }
}