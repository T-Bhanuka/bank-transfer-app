package com.example.bankapp.service;

import com.example.bankapp.exception.*;
import com.example.bankapp.model.*;
import com.example.bankapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    private void validateTransfer(String fromAcc, String toAcc, BigDecimal amount) {
        if (fromAcc == null || fromAcc.isBlank() || toAcc == null || toAcc.isBlank()) {
            throw new BankException("Sender and receiver accounts are required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Transfer amount must be greater than zero.");
        }
        if (fromAcc.equals(toAcc)) {
            throw new BankException("From and To accounts must be different.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAcc, String toAcc, BigDecimal amount, boolean simulateFailure) {
        // 1. Validation
        validateTransfer(fromAcc, toAcc, amount);
        if (customerRepository.countAccounts(fromAcc) == 0) {
            throw new AccountNotFoundException("Sender account " + fromAcc + " not found.");
        }
        if (customerRepository.countAccounts(toAcc) == 0) {
            throw new AccountNotFoundException("Receiver account " + toAcc + " not found.");
        }

        // 2. Debit
        BigDecimal fee = amount.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);
        int updatedRows = customerRepository.debit(fromAcc, totalDebit);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAcc);
        }

        // 3. Crash simulation checkbox (Rollback පරීක්ෂා කිරීමට)
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal!");
        }

        // 4. Credit
        customerRepository.credit(toAcc, amount);
        if (customerRepository.countAccounts("999999") > 0) {
            customerRepository.credit("999999", fee);
        }

        // 5. Record SUCCESS
        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Transfer completed");
        tx.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Account and deposit amount are required.");
        }
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Account " + accountNumber + " not found.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Account and withdrawal amount are required.");
        }
        if (customerRepository.debit(accountNumber, amount) == 0) {
            if (customerRepository.countAccounts(accountNumber) == 0) {
                throw new AccountNotFoundException("Account " + accountNumber + " not found.");
            }
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNumber);
        }
    }

    @Transactional
    public void createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists.");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists.");
        }
        customerRepository.save(customer);
    }
}