package com.springboot.customer.interfaces.rest;

import com.springboot.common.web.ApiResponse;
import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.port.in.QueryCustomerUseCase;
import com.springboot.customer.domain.port.in.RegisterCustomerUseCase;
import com.springboot.customer.interfaces.rest.dto.CustomerResponse;
import com.springboot.customer.interfaces.rest.dto.RegisterCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Driving adapter for HTTP.
 *
 * <p>Depends on the inbound ports, not on the application service class. It
 * translates between HTTP and the use case, and does nothing else - no business
 * rules, no persistence, and no error handling (that is centralised in
 * {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping(path = "/api/v1/customers", produces = "application/json")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomer;
    private final QueryCustomerUseCase queryCustomer;

    public CustomerController(RegisterCustomerUseCase registerCustomer, QueryCustomerUseCase queryCustomer) {
        this.registerCustomer = registerCustomer;
        this.queryCustomer = queryCustomer;
    }

    @GetMapping
    public ApiResponse<List<CustomerResponse>> findAll() {
        return ApiResponse.ok(queryCustomer.findAll().stream()
                .map(CustomerResponse::from)
                .toList());
    }

    @GetMapping("/{customerNumber}")
    public ApiResponse<CustomerResponse> findByNumber(@PathVariable String customerNumber) {
        Customer customer = queryCustomer.getByCustomerNumber(CustomerNumber.of(customerNumber));
        return ApiResponse.ok(CustomerResponse.from(customer));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse<CustomerResponse>> register(
            @Valid @RequestBody RegisterCustomerRequest request) {
        Customer registered = registerCustomer.register(
                new RegisterCustomerUseCase.RegisterCustomerCommand(request.fullName(), request.email()));

        return ResponseEntity
                .created(URI.create("/api/v1/customers/" + registered.customerNumber().value()))
                .body(ApiResponse.ok("Customer registered", CustomerResponse.from(registered)));
    }
}
