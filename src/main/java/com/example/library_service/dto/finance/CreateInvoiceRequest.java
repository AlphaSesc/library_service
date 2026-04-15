package com.example.library_service.dto.finance;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {

    private String studentId;
    private String courseCode; // nullable
    private BigDecimal amount;
    private InvoiceType invoiceType;
}