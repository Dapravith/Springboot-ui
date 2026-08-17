package com.springboot.account.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotBlank @Pattern(regexp = "CUS\\d{10}", message = "must match CUS followed by 10 digits")
        String customerNumber,

        @NotNull @DecimalMin(value = "0.00", message = "opening balance must not be negative")
        @Digits(integer = 15, fraction = 4)
        BigDecimal openingBalance,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code")
        String currency) {
}
