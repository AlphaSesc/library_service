package com.example.library_service.service;

import com.example.library_service.dto.ChangePinRequest;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.exception.InvalidCredentialsException;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.exception.UserNotFoundException;
import com.example.library_service.repository.LibraryUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
// Service responsible for library user management and authentication
public class LibraryUserService {

    private final LibraryUserRepository libraryUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;

    public LibraryUserService(LibraryUserRepository libraryUserRepository,
                              PasswordEncoder passwordEncoder, AuthenticatedLibraryUserService authenticatedLibraryUserService) {
        this.libraryUserRepository = libraryUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedLibraryUserService = authenticatedLibraryUserService;
    }

    // Creates a library account for a student with default PIN
    public LibraryUser createStudentLibraryAccount(String studentId) {

        // Prevent duplicate accounts for same student
        if (libraryUserRepository.findByStudentId(studentId).isPresent()) {
            throw new ResourceAlreadyExistsException("Library account already exists for this student ID");
        }

        // Initialize user with default PIN and first-login flag
        LibraryUser user = LibraryUser.builder()
                .studentId(studentId)
                .pinHash(passwordEncoder.encode("000000"))
                .role(LibraryRole.STUDENT)
                .firstLogin(true)
                .active(true)
                .build();

        return libraryUserRepository.save(user);
    }

    // Finds user by studentId (used for authentication)
    public Optional<LibraryUser> findByUsername(String studentId) {
        return libraryUserRepository.findByStudentId(studentId);
    }

    // Authenticates library user using studentId and PIN
    public LibraryUser authenticate(String studentId, String pin) {
        LibraryUser user = libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException("Library user not found"));

        // Ensure user account is active
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Library account is inactive");
        }

        // Validate PIN against stored hash
        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            throw new InvalidCredentialsException("Invalid student ID or PIN");
        }

        return user;
    }

    // Allows authenticated user to change their PIN
    public void changePin(ChangePinRequest request) {

        LibraryUser user = authenticatedLibraryUserService.getCurrentUser();

        // Verify old PIN before allowing change
        if (!passwordEncoder.matches(request.getOldPin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Old PIN is incorrect");
        }

        // Update PIN and mark first login as completed
        user.setPinHash(passwordEncoder.encode(request.getNewPin()));
        user.setFirstLogin(false);

        libraryUserRepository.save(user);
    }
}