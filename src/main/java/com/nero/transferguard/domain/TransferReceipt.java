package com.nero.transferguard.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferReceipt(
        String requestId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        Instant completedAt) {
}
