package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.save(new Customer(null, "100001", "Kamal Perera", "kamal@gmail.com", new BigDecimal("50000.00")));
            customerRepository.save(new Customer(null, "100002", "Nimali Fernando", "nimali@gmail.com", new BigDecimal("25000.00")));
            customerRepository.save(new Customer(null, "100003", "Saman Kumara", "saman@gmail.com", new BigDecimal("10000.00")));
            customerRepository.save(new Customer(null, "999999", "Bank Fee Account", "bank-fee@bank.local", BigDecimal.ZERO));
        }
    }
}