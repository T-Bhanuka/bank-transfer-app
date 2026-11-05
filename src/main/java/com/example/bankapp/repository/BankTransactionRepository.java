package com.example.bankapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();

    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :acc OR t.toAccount = :acc ORDER BY t.createdAt DESC")
    List<BankTransaction> findHistory(@Param("acc") String acc);

    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT t FROM BankTransaction t WHERE t.status = com.example.bankapp.model.TransactionStatus.SUCCESS AND t.amount > :min ORDER BY t.createdAt DESC")
    List<BankTransaction> findSuccessfulTransfersAbove(@Param("min") BigDecimal min);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t WHERE t.status = com.example.bankapp.model.TransactionStatus.SUCCESS AND t.fromAccount = :account")
    BigDecimal sumSuccessfulAmountSent(@Param("account") String account);
}