package com.example.library_service.service;

import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.exception.UnauthorizedOperationException;
import com.example.library_service.exception.UserNotFoundException;
import com.example.library_service.repository.LibraryUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// Service responsible for retrieving the currently authenticated library user
// and enforcing role-based access control
public class AuthenticatedLibraryUserService {

    private final LibraryUserRepository libraryUserRepository;

    // Retrieves the currently authenticated user from SecurityContext
    public LibraryUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ensure authentication exists
        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("Authenticated library user not found");
        }

        String studentId = authentication.getName();

        // Fetch user from database using studentId stored in token
        return libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException("Library user not found"));
    }

    // Returns current user only if role is STUDENT
    public LibraryUser getCurrentStudentUser() {
        LibraryUser libraryUser = getCurrentUser();

        if (libraryUser.getRole() != LibraryRole.STUDENT) {
            throw new UnauthorizedOperationException("Only students can access this feature");
        }

        return libraryUser;
    }

    // Returns current user only if role is ADMIN
    public LibraryUser getCurrentAdminUser() {
        LibraryUser libraryUser = getCurrentUser();

        if (libraryUser.getRole() != LibraryRole.ADMIN) {
            throw new UnauthorizedOperationException("Only admins can access this feature");
        }

        return libraryUser;
    }
}