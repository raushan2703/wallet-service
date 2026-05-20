package com.example.wallet.dto;

import com.example.wallet.entity.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record WalletOperationRequest(
        @NotNull(message = "walletId must not be null")
        UUID walletId,

        @NotNull(message = "operationType must not be null")
        OperationType operationType,

        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be greater than zero")
        Long amount
) {
}
