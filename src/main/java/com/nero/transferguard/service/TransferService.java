package com.nero.transferguard.service;

import com.nero.transferguard.domain.Account;
import com.nero.transferguard.domain.TransferReceipt;
import com.nero.transferguard.port.TransferAuditPort;
import com.nero.transferguard.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TransferService {
    private final AccountRepository accountRepository;
    private final TransferAuditPort auditPort;
    private final Clock clock;
    private final Map<String, TransferReceipt> completedRequests = new HashMap<>();

    public TransferService(AccountRepository accountRepository, TransferAuditPort auditPort, Clock clock) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized TransferReceipt transfer(
            String requestId,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount) {

        requireText(requestId, "requestId");
        requireText(sourceAccountId, "sourceAccountId");
        requireText(targetAccountId, "targetAccountId");

        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        TransferReceipt existing = completedRequests.get(requestId);
        if (existing != null) {
            return existing;
        }

        Account source = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account target = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        boolean sourceDebited = false;
        boolean targetCredited = false;

        try {
            source.debit(amount);
            sourceDebited = true;

            target.credit(amount);
            targetCredited = true;

            TransferReceipt receipt = new TransferReceipt(
                    requestId,
                    sourceAccountId,
                    targetAccountId,
                    amount,
                    Instant.now(clock));

            auditPort.record(receipt);
            accountRepository.save(source);
            accountRepository.save(target);
            completedRequests.put(requestId, receipt);
            return receipt;
        } catch (RuntimeException ex) {
            if (targetCredited) {
                target.debit(amount);
            }
            if (sourceDebited) {
                source.credit(amount);
            }
            throw ex;
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
