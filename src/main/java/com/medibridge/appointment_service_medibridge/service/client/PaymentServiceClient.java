package com.medibridge.appointment_service_medibridge.service.client;

import com.medibridge.appointment_service_medibridge.dto.payment.CheckoutRequest;
import com.medibridge.appointment_service_medibridge.dto.payment.CheckoutResponse;
import com.medibridge.appointment_service_medibridge.dto.payment.CreateInvoiceRequest;
import com.medibridge.appointment_service_medibridge.dto.payment.InvoiceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.feign.payment-service-url:http://localhost:8087/api/v1}")
public interface PaymentServiceClient {

    @PostMapping("/payments/invoices")
    InvoiceDTO createInvoice(@RequestBody CreateInvoiceRequest request);

    @PostMapping("/payments/checkout-link")
    CheckoutResponse getCheckoutLink(@RequestBody CheckoutRequest request);
}
