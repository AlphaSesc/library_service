package com.example.library_service.dto.finance;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO for creating an invoice in the Finance service
public class CreateInvoiceRequest {

    // Student identifier used across microservices
    private String studentId;

    // Course code (nullable, used only for course-related invoices)
    private String courseCode; // nullable

    // Amount to be charged (e.g., fine for late return)
    private BigDecimal amount;

    // Type of invoice (e.g., LIBRARY_FINE, COURSE_ENROLLMENT)
    private InvoiceType invoiceType;
}