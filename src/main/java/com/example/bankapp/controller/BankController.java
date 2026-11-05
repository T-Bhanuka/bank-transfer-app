package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final BankService bankService;
    private final TransactionAuditService auditService;

    private void addDashboardAttributes(Model model, String keyword) {
        List<Customer> customers = (keyword == null || keyword.isBlank())
                ? customerRepository.findAllOrdered()
                : customerRepository.searchByName(keyword.trim());
        BigDecimal totalBalance = customerRepository.getTotalBalance();
        model.addAttribute("customers", customers);
        model.addAttribute("allCustomers", customerRepository.findAllOrdered());
        model.addAttribute("transactions", transactionRepository.findAllLatest());
        model.addAttribute("totalCustomers", customerRepository.countCustomersNative());
        model.addAttribute("totalBalance", totalBalance == null ? BigDecimal.ZERO : totalBalance);
        model.addAttribute("successCount", transactionRepository.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", transactionRepository.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        addDashboardAttributes(model, keyword);
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        return "bank";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addDashboardAttributes(model, null);
            model.addAttribute("customer", new Customer());
            return "bank";
        }
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(), request.getAmount(), request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        } catch (AccountNotFoundException | InsufficientBalanceException | BankException ex) {
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(), request.getAmount(), ex.getMessage());
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addDashboardAttributes(model, null);
            model.addAttribute("transferRequest", new TransferRequest());
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully.");
        } catch (BankException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                          RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully.");
        } catch (BankException | AccountNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully.");
        } catch (BankException | AccountNotFoundException | InsufficientBalanceException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/withdraw";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model) {
        Customer customer = customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found."));
        BigDecimal sent = transactionRepository.sumSuccessfulAmountSent(accountNumber);
        if (sent == null) {
            sent = BigDecimal.ZERO;
        }
        BigDecimal fee = sent.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactionRepository.findHistory(accountNumber));
        model.addAttribute("totalSent", sent.add(fee));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        if (min.compareTo(BigDecimal.ZERO) < 0) {
            min = BigDecimal.ZERO;
        }
        model.addAttribute("min", min);
        model.addAttribute("transfers", transactionRepository.findSuccessfulTransfersAbove(min));
        return "large-transfers";
    }
}
