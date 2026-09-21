package com.nero.transferguard;

import com.nero.transferguard.domain.Account;
import com.nero.transferguard.repository.InMemoryAccountRepository;
import com.nero.transferguard.service.TransferService;

import java.math.BigDecimal;
import java.time.Clock;

public final class Main {
    public static void main(String[] args) {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        repository.save(new Account("A-100", new BigDecimal("500.00")));
        repository.save(new Account("B-200", new BigDecimal("120.00")));

        TransferService service = new TransferService(repository, receipt -> {
        }, Clock.systemUTC());

        var receipt = service.transfer("REQ-001", "A-100", "B-200", new BigDecimal("75.50"));

        System.out.println("Transfer completed: " + receipt.requestId());
        System.out.println("A-100 balance: " + repository.findById("A-100").orElseThrow().balance());
        System.out.println("B-200 balance: " + repository.findById("B-200").orElseThrow().balance());
    }
}
