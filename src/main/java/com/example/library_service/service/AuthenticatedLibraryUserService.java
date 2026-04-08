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
public class AuthenticatedLibraryUserService {

    private final LibraryUserRepository libraryUserRepository;

    public LibraryUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("Authenticated library user not found");
        }

        String studentId = authentication.getName();

        return libraryUserRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException("Library user not found"));
    }

    public LibraryUser getCurrentStudentUser() {
        LibraryUser libraryUser = getCurrentUser();

        if (libraryUser.getRole() != LibraryRole.STUDENT) {
            throw new UnauthorizedOperationException("Only students can access this feature");
        }

        return libraryUser;
    }

    public LibraryUser getCurrentAdminUser() {
        LibraryUser libraryUser = getCurrentUser();

        if (libraryUser.getRole() != LibraryRole.ADMIN) {
            throw new UnauthorizedOperationException("Only admins can access this feature");
        }

        return libraryUser;
    }
}