package com.example.library_service.repository;

import java.util.Optional;
import com.example.library_service.entity.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {

    Optional<LibraryUser> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}