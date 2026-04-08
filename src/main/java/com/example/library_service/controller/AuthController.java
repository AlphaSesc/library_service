package com.example.library_service.controller;

import com.example.library_service.dto.LibraryAuthResponse;
import com.example.library_service.dto.LibraryLoginRequest;
import com.example.library_service.dto.LibraryRegisterRequest;
import com.example.library_service.dto.LibraryUserResponse;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.security.CustomLibraryUserDetails;
import com.example.library_service.security.JwtService;
import com.example.library_service.service.LibraryUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LibraryUserService libraryUserService;
    private final JwtService jwtService;

    public AuthController(LibraryUserService libraryUserService, JwtService jwtService) {
        this.libraryUserService = libraryUserService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LibraryAuthResponse login(@Valid @RequestBody LibraryLoginRequest request) {
        LibraryUser user = libraryUserService.authenticate(
                request.getStudentId(),
                request.getPin()
        );

        String token = jwtService.generateToken(new CustomLibraryUserDetails(user));

        return new LibraryAuthResponse(
                token,
                user.getStudentId(),
                user.getRole(),
                user.isFirstLogin()
        );
    }
}