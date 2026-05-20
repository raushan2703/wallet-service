package com.example.wallet.dto;

import java.util.UUID;

public record WalletBalanceResponse(
        UUID walletId,
        Long balance
) {
}
