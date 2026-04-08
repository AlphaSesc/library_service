package com.example.library_service.dto;

import com.example.library_service.entity.LibraryRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LibraryUserResponse {
    private Long id;
    private String studentId;
    private LibraryRole role;
    private boolean firstLogin;
    private boolean active;
}