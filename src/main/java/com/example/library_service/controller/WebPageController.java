package com.example.library_service.controller;

import com.example.library_service.dto.LibraryRegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
// Controller responsible for serving HTML pages (Thymeleaf views)
public class WebPageController {

    // Redirect root URL to login page
    @GetMapping("/")
    public String home() { return "redirect:/login"; }

    // Authentication page
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // Student dashboard page
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "student/dashboard";
    }

    // Student book listing page
    @GetMapping("/books")
    public String booksPage()
    { return "student/books";}

    // Student borrowing history page
    @GetMapping("/borrow-history")
    public String borrowHistoryPage() {
        return "student/borrow-history";
    }

    // Student active borrowings page
    @GetMapping("/my-borrowings")
    public String myBorrowingsPage() {
        return "student/my-borrowings";
    }

    // Admin dashboard page
    @GetMapping("/admin/dashboard")
    public String adminDashboardPage() {
        return "admin/dashboard";
    }

    // Admin page for adding new books
    @GetMapping("/admin/add-book")
    public String addBookPage() {
        return "admin/add-book";
    }

    // Admin book management page
    @GetMapping("/admin/books")
    public String adminBooksPage() {
        return "admin/books";
    }

    // Admin page for viewing current loans
    @GetMapping("/admin/current-loans")
    public String currentLoansPage() {
        return "admin/current-loans";
    }

    // Admin page for viewing overdue loans
    @GetMapping("/admin/overdue")
    public String overduePage() {
        return "admin/overdue";
    }

    // Admin page for viewing student loan summaries
    @GetMapping("/admin/students")
    public String studentsPage() {
        return "admin/students";
    }
}