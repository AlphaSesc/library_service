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

    @GetMapping("/admin/dashboard")
    public String adminDashboardPage() {
        return "admin/dashboard";
    }
}