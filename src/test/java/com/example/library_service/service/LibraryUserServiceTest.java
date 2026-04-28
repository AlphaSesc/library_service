package com.example.library_service.service;

import com.example.library_service.dto.ChangePinRequest;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.exception.InvalidCredentialsException;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.exception.UserNotFoundException;
import com.example.library_service.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryUserServiceTest {

    @Mock
    private LibraryUserRepository libraryUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @InjectMocks
    private LibraryUserService libraryUserService;

    private LibraryUser libraryUser;

    @BeforeEach
    void setUp() {

        libraryUser = LibraryUser.builder()
                .id(1L)
                .studentId("STU-100")
                .pinHash("encoded-pin")
                .role(LibraryRole.STUDENT)
                .firstLogin(true)
                .active(true)
                .build();
    }

    @Test
    void createStudentLibraryAccountShouldCreateNewAccount() {

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("000000"))
                .thenReturn("encoded-default-pin");

        when(libraryUserRepository.save(any(LibraryUser.class)))
                .thenReturn(libraryUser);

        LibraryUser result =
                libraryUserService.createStudentLibraryAccount("STU-100");

        assertNotNull(result);
        assertEquals("STU-100", result.getStudentId());
        assertEquals(LibraryRole.STUDENT, result.getRole());

        verify(passwordEncoder).encode("000000");
        verify(libraryUserRepository).save(any(LibraryUser.class));
    }

    @Test
    void createStudentLibraryAccountShouldThrowWhenAccountAlreadyExists() {

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(libraryUser));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> libraryUserService.createStudentLibraryAccount("STU-100")
        );

        assertEquals(
                "Library account already exists for this student ID",
                exception.getMessage()
        );

        verify(libraryUserRepository, never()).save(any());
    }

    @Test
    void findByUsernameShouldReturnLibraryUser() {

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(libraryUser));

        Optional<LibraryUser> result =
                libraryUserService.findByUsername("STU-100");

        assertTrue(result.isPresent());
        assertEquals("STU-100", result.get().getStudentId());
    }

    @Test
    void authenticateShouldReturnUserWhenCredentialsAreValid() {

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(libraryUser));

        when(passwordEncoder.matches("000000", "encoded-pin"))
                .thenReturn(true);

        LibraryUser result =
                libraryUserService.authenticate("STU-100", "000000");

        assertNotNull(result);
        assertEquals("STU-100", result.getStudentId());
    }

    @Test
    void authenticateShouldThrowWhenUserDoesNotExist() {

        when(libraryUserRepository.findByStudentId("STU-404"))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> libraryUserService.authenticate("STU-404", "000000")
        );

        assertEquals(
                "Library user not found",
                exception.getMessage()
        );
    }

    @Test
    void authenticateShouldThrowWhenAccountIsInactive() {

        libraryUser.setActive(false);

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(libraryUser));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> libraryUserService.authenticate("STU-100", "000000")
        );

        assertEquals(
                "Library account is inactive",
                exception.getMessage()
        );
    }

    @Test
    void authenticateShouldThrowWhenPinIsInvalid() {

        when(libraryUserRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(libraryUser));

        when(passwordEncoder.matches("wrong-pin", "encoded-pin"))
                .thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> libraryUserService.authenticate("STU-100", "wrong-pin")
        );

        assertEquals(
                "Invalid student ID or PIN",
                exception.getMessage()
        );
    }

    @Test
    void changePinShouldUpdatePinSuccessfully() {

        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("000000")
                .newPin("123456")
                .build();

        when(authenticatedLibraryUserService.getCurrentUser())
                .thenReturn(libraryUser);

        when(passwordEncoder.matches("000000", "encoded-pin"))
                .thenReturn(true);

        when(passwordEncoder.encode("123456"))
                .thenReturn("new-encoded-pin");

        libraryUserService.changePin(request);

        assertEquals("new-encoded-pin", libraryUser.getPinHash());
        assertFalse(libraryUser.isFirstLogin());

        verify(libraryUserRepository).save(libraryUser);
    }

    @Test
    void changePinShouldThrowWhenOldPinIsIncorrect() {

        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("wrong-pin")
                .newPin("123456")
                .build();

        when(authenticatedLibraryUserService.getCurrentUser())
                .thenReturn(libraryUser);

        when(passwordEncoder.matches("wrong-pin", "encoded-pin"))
                .thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> libraryUserService.changePin(request)
        );

        assertEquals(
                "Old PIN is incorrect",
                exception.getMessage()
        );

        verify(libraryUserRepository, never()).save(any());
    }
}