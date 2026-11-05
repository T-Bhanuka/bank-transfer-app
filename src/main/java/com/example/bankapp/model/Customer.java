package com.example.bankapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6}", message = "Account number must contain exactly 6 digits")
    private String accountNumber;

    @Column(name = "full_name", nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @Column(precision = 14, scale = 2, nullable = false)
    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
    private BigDecimal balance;
}