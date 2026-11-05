package com.example.bankapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {
    @NotBlank(message = "Sender account is required")
    @Pattern(regexp = "\\d{6}", message = "Sender account must contain exactly 6 digits")
    private String fromAccount;

    @NotBlank(message = "Receiver account is required")
    @Pattern(regexp = "\\d{6}", message = "Receiver account must contain exactly 6 digits")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 integer digits and 2 decimal places")
    private BigDecimal amount;

    private boolean simulateFailure;
}
