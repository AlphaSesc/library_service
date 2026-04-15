package com.example.library_service.client;

import com.example.library_service.dto.finance.CreateInvoiceRequest;
import com.example.library_service.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FinanceClient {

    private final RestTemplate restTemplate;

    @Value("${finance.service.base-url}")
    private String financeBaseUrl;

    public void createInvoice(CreateInvoiceRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/invoices",
                    request,
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create invoice in finance service");
        }
    }
}