package com.example.wallet.service;

import com.example.wallet.entity.OperationType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void processOperation(UUID walletId, OperationType operationType, long amount) {
        log.debug("wallet={} operation={} amount={}", walletId, operationType, amount);

        if (operationType == OperationType.DEPOSIT) {
            walletRepository.deposit(walletId, amount);
        } else {
            int affected = walletRepository.withdraw(walletId, amount);
            if (affected == 0) {
                // need to figure out why it failed - wallet missing or no funds
                if (!walletRepository.existsById(walletId)) {
                    throw new WalletNotFoundException(walletId);
                }
                log.warn("Insufficient funds: wallet={}, requested={}", walletId, amount);
                throw new InsufficientFundsException(walletId);
            }
        }
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
