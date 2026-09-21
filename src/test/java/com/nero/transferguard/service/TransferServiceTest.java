package com.nero.transferguard.service;

import com.nero.transferguard.domain.Account;
import com.nero.transferguard.repository.InMemoryAccountRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TransferServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC);

    public static void main(String[] args) {
        transfersMoneyBetweenAccounts();
        returnsSameReceiptForDuplicateRequest();
        rollsBackWhenAuditFails();
        rejectsInsufficientFunds();
        System.out.println("All TransferService tests passed.");
    }

    private static void transfersMoneyBetweenAccounts() {
        var repository = repositoryWithBalances("100.00", "20.00");
        var service = new TransferService(repository, receipt -> {}, FIXED_CLOCK);

        var receipt = service.transfer("REQ-1", "A", "B", new BigDecimal("25.00"));

        assertMoney("75.00", repository.findById("A").orElseThrow().balance());
        assertMoney("45.00", repository.findById("B").orElseThrow().balance());
        if (!receipt.completedAt().equals(Instant.parse("2026-09-21T12:00:00Z"))) {
            throw new AssertionError("Expected deterministic completion time");
        }
    }

    private static void returnsSameReceiptForDuplicateRequest() {
        var repository = repositoryWithBalances("100.00", "20.00");
        var service = new TransferService(repository, receipt -> {}, FIXED_CLOCK);

        var first = service.transfer("REQ-2", "A", "B", new BigDecimal("10.00"));
        var second = service.transfer("REQ-2", "A", "B", new BigDecimal("10.00"));

        if (first != second) {
            throw new AssertionError("Expected the cached receipt for duplicate request");
        }
        assertMoney("90.00", repository.findById("A").orElseThrow().balance());
        assertMoney("30.00", repository.findById("B").orElseThrow().balance());
    }

    private static void rollsBackWhenAuditFails() {
        var repository = repositoryWithBalances("100.00", "20.00");
        var service = new TransferService(repository, receipt -> {
            throw new IllegalStateException("Audit unavailable");
        }, FIXED_CLOCK);

        try {
            service.transfer("REQ-3", "A", "B", new BigDecimal("40.00"));
            throw new AssertionError("Expected audit failure");
        } catch (IllegalStateException expected) {
            assertMoney("100.00", repository.findById("A").orElseThrow().balance());
            assertMoney("20.00", repository.findById("B").orElseThrow().balance());
        }
    }

    private static void rejectsInsufficientFunds() {
        var repository = repositoryWithBalances("10.00", "20.00");
        var service = new TransferService(repository, receipt -> {}, FIXED_CLOCK);

        try {
            service.transfer("REQ-4", "A", "B", new BigDecimal("50.00"));
            throw new AssertionError("Expected insufficient funds failure");
        } catch (IllegalStateException expected) {
            assertMoney("10.00", repository.findById("A").orElseThrow().balance());
            assertMoney("20.00", repository.findById("B").orElseThrow().balance());
        }
    }

    private static InMemoryAccountRepository repositoryWithBalances(String a, String b) {
        var repository = new InMemoryAccountRepository();
        repository.save(new Account("A", new BigDecimal(a)));
        repository.save(new Account("B", new BigDecimal(b)));
        return repository;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
