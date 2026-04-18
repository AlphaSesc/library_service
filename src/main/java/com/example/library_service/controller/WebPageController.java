package com.example.library_service.controller;

import com.example.library_service.dto.LibraryRegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebPageController {

    @GetMapping("/")
    public String home() { return "redirect:/login"; }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "student/dashboard";
    }

    @GetMapping("/books")
    public String booksPage()
    { return "student/books";}

    @GetMapping("/borrow-history")
    public String borrowHistoryPage() {
        return "student/borrow-history";
    }

    @GetMapping("/my-borrowings")
    public String myBorrowingsPage() {
        return "student/my-borrowings";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboardPage() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/add-book")
    public String addBookPage() {
        return "admin/add-book";
    }

    @GetMapping("/admin/books")
    public String adminBooksPage() {
        return "admin/books";
    }

    @GetMapping("/admin/current-loans")
    public String currentLoansPage() {
        return "admin/current-loans";
    }

    @GetMapping("/admin/overdue")
    public String overduePage() {
        return "admin/overdue";
    }

    @GetMapping("/admin/students")
    public String studentsPage() {
        return "admin/students";
    }
}