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

    public LibraryUser createStudentLibraryAccount(String studentId) {
        if (libraryUserRepository.findByStudentId(studentId).isPresent()) {
            throw new ResourceAlreadyExistsException("Library account already exists for this student ID");
        }

        LibraryUser user = LibraryUser.builder()
                .studentId(studentId)
                .pinHash(passwordEncoder.encode("000000"))
                .role(LibraryRole.STUDENT)
                .firstLogin(true)
                .active(true)
                .build();

        return libraryUserRepository.save(user);
    }

    public Optional<LibraryUser> findByUsername(String studentId) {
        return libraryUserRepository.findByStudentId(studentId);
    }

    public LibraryUser authenticate(String studentId, String pin) {
        LibraryUser user = libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException("Library user not found"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Library account is inactive");
        }

        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            throw new InvalidCredentialsException("Invalid student ID or PIN");
        }

        return user;
    }

    public void changePin(ChangePinRequest request) {
        LibraryUser user = authenticatedLibraryUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Old PIN is incorrect");
        }

        user.setPinHash(passwordEncoder.encode(request.getNewPin()));
        user.setFirstLogin(false);
        libraryUserRepository.save(user);
    }
}