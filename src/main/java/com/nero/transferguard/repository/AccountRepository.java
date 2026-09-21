package com.nero.transferguard.repository;

import com.nero.transferguard.domain.Account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(String id);
    void save(Account account);
}
