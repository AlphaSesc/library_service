package com.example.library_service.repository;

import java.util.List;
import java.util.Optional;

import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for accessing LibraryUser entities
public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {

    // Finds a library user using studentId (links with Student Portal)
    Optional<LibraryUser> findByStudentId(String studentId);

    // Retrieves users based on role (e.g., STUDENT, ADMIN)
    List<LibraryUser> findByRole(LibraryRole role);
}