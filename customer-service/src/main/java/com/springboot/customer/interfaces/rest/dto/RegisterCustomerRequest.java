package com.springboot.customer.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP-facing input. Separate from the use-case command so the wire contract can evolve independently. */
public record RegisterCustomerRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email) {
}
