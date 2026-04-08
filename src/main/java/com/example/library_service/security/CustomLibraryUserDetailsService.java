package com.example.library_service.security;

import com.example.library_service.entity.LibraryUser;
import com.example.library_service.exception.InvalidCredentialsException;
import com.example.library_service.repository.LibraryUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomLibraryUserDetailsService implements UserDetailsService {

    private final LibraryUserRepository libraryUserRepository;

    public CustomLibraryUserDetailsService(LibraryUserRepository libraryUserRepository) {
        this.libraryUserRepository = libraryUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String studentId) {
        LibraryUser user = libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid student ID or PIN"));

        return new CustomLibraryUserDetails(user);
    }
}