package com.example.library_service.dto;

import com.example.library_service.entity.LibraryRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// Response DTO returned after successful library authentication
public class LibraryAuthResponse {

    // JWT token used for authenticating future requests
    private String token;
    private String studentId;
    private LibraryRole role;
    // Indicates if user needs to change PIN on first login
    private boolean firstLogin;
}