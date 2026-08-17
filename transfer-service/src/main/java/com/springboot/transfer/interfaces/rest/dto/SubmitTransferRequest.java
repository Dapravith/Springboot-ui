package com.springboot.transfer.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record SubmitTransferRequest(
        @NotBlank @Pattern(regexp = "ACC\\d{12}", message = "must match ACC followed by 12 digits")
        String fromAccountNumber,

        @NotBlank @Pattern(regexp = "ACC\\d{12}", message = "must match ACC followed by 12 digits")
        String toAccountNumber,

        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code")
        String currency) {
}
