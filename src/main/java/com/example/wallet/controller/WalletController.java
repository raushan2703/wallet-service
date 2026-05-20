package com.example.wallet.controller;

import com.example.wallet.dto.WalletBalanceResponse;
import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallet")
    public ResponseEntity<Void> performOperation(@Valid @RequestBody WalletOperationRequest request) {
        walletService.processOperation(
                request.walletId(),
                request.operationType(),
                request.amount()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID walletId) {
        Wallet wallet = walletService.getWallet(walletId);
        return ResponseEntity.ok(new WalletBalanceResponse(wallet.getId(), wallet.getBalance()));
    }
}
