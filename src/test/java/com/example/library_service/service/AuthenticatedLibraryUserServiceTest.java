package com.example.library_service.service;

import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.exception.UnauthorizedOperationException;
import com.example.library_service.exception.UserNotFoundException;
import com.example.library_service.repository.LibraryUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedLibraryUserServiceTest {

    @Mock
    private LibraryUserRepository libraryUserRepository;

    @InjectMocks
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    private LibraryUser studentUser;
    private LibraryUser adminUser;

    @BeforeEach
    void setUp() {

        studentUser = LibraryUser.builder()
                .id(1L)
                .studentId("STU-100")
                .role(LibraryRole.STUDENT)
                .active(true)
                .build();

        adminUser = LibraryUser.builder()
                .id(2L)
                .studentId("ADMIN-1")
                .role(LibraryRole.ADMIN)
                .active(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedLibraryUser() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "STU-100",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(studentUser));

        LibraryUser result =
                authenticatedLibraryUserService.getCurrentUser();

        assertNotNull(result);
        assertEquals("STU-100", result.getStudentId());
        assertEquals(LibraryRole.STUDENT, result.getRole());
    }

    @Test
    void getCurrentUserShouldThrowWhenAuthenticationIsMissing() {

        SecurityContextHolder.clearContext();

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authenticatedLibraryUserService.getCurrentUser()
        );

        assertEquals(
                "Authenticated library user not found",
                exception.getMessage()
        );
    }

    @Test
    void getCurrentUserShouldThrowWhenLibraryUserDoesNotExist() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "STU-404",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("STU-404"))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authenticatedLibraryUserService.getCurrentUser()
        );

        assertEquals(
                "Library user not found",
                exception.getMessage()
        );
    }

    @Test
    void getCurrentStudentUserShouldReturnStudentUser() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "STU-100",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(studentUser));

        LibraryUser result =
                authenticatedLibraryUserService.getCurrentStudentUser();

        assertEquals(LibraryRole.STUDENT, result.getRole());
    }

    @Test
    void getCurrentStudentUserShouldThrowWhenUserIsNotStudent() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "ADMIN-1",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("ADMIN-1"))
                .thenReturn(Optional.of(adminUser));

        UnauthorizedOperationException exception = assertThrows(
                UnauthorizedOperationException.class,
                () -> authenticatedLibraryUserService.getCurrentStudentUser()
        );

        assertEquals(
                "Only students can access this feature",
                exception.getMessage()
        );
    }

    @Test
    void getCurrentAdminUserShouldReturnAdminUser() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "ADMIN-1",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("ADMIN-1"))
                .thenReturn(Optional.of(adminUser));

        LibraryUser result =
                authenticatedLibraryUserService.getCurrentAdminUser();

        assertEquals(LibraryRole.ADMIN, result.getRole());
    }

    @Test
    void getCurrentAdminUserShouldThrowWhenUserIsNotAdmin() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "STU-100",
                        null
                )
        );

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(studentUser));

        UnauthorizedOperationException exception = assertThrows(
                UnauthorizedOperationException.class,
                () -> authenticatedLibraryUserService.getCurrentAdminUser()
        );

        assertEquals(
                "Only admins can access this feature",
                exception.getMessage()
        );
    }
}