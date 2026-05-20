package com.example.wallet.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID walletId) {
        super("Insufficient funds in wallet: " + walletId);
    }
}
