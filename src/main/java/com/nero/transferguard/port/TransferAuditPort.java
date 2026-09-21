package com.nero.transferguard.port;

import com.nero.transferguard.domain.TransferReceipt;

@FunctionalInterface
public interface TransferAuditPort {
    void record(TransferReceipt receipt);
}
