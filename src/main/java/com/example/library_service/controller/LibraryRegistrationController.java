package com.example.library_service.controller;

import com.example.library_service.dto.LibraryRegisterRequest;
import com.example.library_service.dto.LibraryUserResponse;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.service.LibraryUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LibraryRegistrationController {

    private final LibraryUserService libraryUserService;

    public LibraryRegistrationController(LibraryUserService libraryUserService) {
        this.libraryUserService = libraryUserService;
    }

    @PostMapping("/register")
    public LibraryUserResponse register(@Valid @RequestBody LibraryRegisterRequest request) {
        LibraryUser savedUser = libraryUserService.createStudentLibraryAccount(request.getStudentId());

        return new LibraryUserResponse(
                savedUser.getId(),
                savedUser.getStudentId(),
                savedUser.getRole(),
                savedUser.isFirstLogin(),
                savedUser.isActive()
        );
    }
}