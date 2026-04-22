package com.example.library_service.client;

import com.example.library_service.dto.ApiErrorResponse;
import com.example.library_service.dto.finance.CreateInvoiceRequest;
import com.example.library_service.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
// Client responsible for communicating with Finance microservice
public class FinanceClient {

    private final RestTemplate restTemplate;

    @Value("${finance.service.base-url}")
    // Base URL of Finance service configured externally
    private String financeBaseUrl;

    // Sends invoice creation request to Finance service (e.g., late return fine)
    public void createInvoice(CreateInvoiceRequest request) {
        try {
            restTemplate.postForObject(
                    financeBaseUrl + "/api/invoices",
                    request,
                    Void.class
            );
        }catch (HttpStatusCodeException ex) {
            // Extract meaningful error returned by Finance service
            throw new ExternalServiceException(extractErrorMessage(ex));
        }
        catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to create invoice in finance service");
        }
    }

    // Extracts structured error message from Finance service response
    private String extractErrorMessage(HttpStatusCodeException ex) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ApiErrorResponse errorResponse = objectMapper.readValue(
                    ex.getResponseBodyAsString(),
                    ApiErrorResponse.class
            );

            if (errorResponse.getMessage() != null && !errorResponse.getMessage().isBlank()) {
                return errorResponse.getMessage();
            }
        } catch (Exception ignored) {
        }

        return "Finance service request failed";
    }
}